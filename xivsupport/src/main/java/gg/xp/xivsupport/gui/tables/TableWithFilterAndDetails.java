package gg.xp.xivsupport.gui.tables;

import gg.xp.xivsupport.gui.GuiGlobals;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.SplitVisualFilter;
import gg.xp.xivsupport.gui.tables.filters.VisualFilter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class TableWithFilterAndDetails<X, D> extends TitleBorderFullsizePanel {

	private static final Logger log = LoggerFactory.getLogger(TableWithFilterAndDetails.class);
	private final ExecutorService refresherPool = Executors.newSingleThreadExecutor();
//	private final ExecutorService filteringPool = Executors.newSingleThreadExecutor();

	private final Supplier<List<X>> dataGetter;
	private final List<VisualFilter<? super X>> filters;
	private final CustomTableModel<X> mainModel;
	private final @Nullable JCheckBox stayAtBottom;
	private final JTable table;
	private final @Nullable AutoBottomScrollHelper scroller;
	private @Nullable JSplitPane splitPane;
	private volatile X currentSelection;
	private List<X> dataRaw = Collections.emptyList();
	private List<X> dataFiltered = Collections.emptyList();
	private volatile boolean isAutoRefreshEnabled;
	private final boolean appendOrPruneOnly;
	private final String title;
	private EditMode editMode = EditMode.NEVER;

	private TableWithFilterAndDetails(
			String title,
			Supplier<List<X>> dataGetter,
			List<CustomColumn<? super X>> mainColumns,
			List<CustomColumn<? super D>> detailsColumns,
			Function<? super X, List<D>> detailsConverter,
			List<Function<Runnable, VisualFilter<? super X>>> filterCreators,
			List<Function<TableWithFilterAndDetails<X, ?>, Component>> widgets,
			RightClickOptionRepo rightClickOptions,
			BiPredicate<? super X, ? super X> selectionEquivalence,
			BiPredicate<? super D, ? super D> detailsSelectionEquivalence,
			boolean appendOrPruneOnly,
			boolean fixedData) {
		super(title);
		this.title = title;
		// TODO: add count of events
		this.dataGetter = dataGetter;
		this.appendOrPruneOnly = appendOrPruneOnly;
		// TODO: the layout is being weird when I resize. Even with GBL it takes a second to change.
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		setLayout(new GridBagLayout());

		CustomTableModel.CustomTableModelBuilder<D> detailsBuilder = CustomTableModel.builder(() -> detailsConverter.apply(this.currentSelection));
		detailsColumns.forEach(detailsBuilder::addColumn);
		detailsBuilder.setItemEquivalence(detailsSelectionEquivalence);
		CustomTableModel<D> detailsModel = detailsBuilder
				.build();


		CustomTableModel.CustomTableModelBuilder<X> mainBuilder = CustomTableModel.builder(this::getFilteredData);
		mainColumns.forEach(mainBuilder::addColumn);
		mainBuilder.setItemEquivalence(selectionEquivalence);
		mainModel = mainBuilder.build();


		// Main table
		table = new JTable(mainModel) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return switch (editMode) {
					case NEVER -> false;
					case AUTO -> !(getCellEditor(row, column) instanceof NoCellEditor);
					case ALWAYS -> true;
				};
			}
		};
		mainModel.configureColumns(table);
		table.setTableHeader(new JTableHeader(table.getColumnModel()));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel selectionModel = table.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			int[] selected = selectionModel.getSelectedIndices();
			log.trace("Selected: {}", Arrays.toString(selected));
			if (selected.length == 0) {
				currentSelection = null;
			}
			else {
				currentSelection = mainModel.getValueForRow(selected[0]);
			}
			// TODO: selection doesn't stick for details items as table updates
			SwingUtilities.invokeLater(detailsModel::fullRefresh);
//			detailsModel.fireTableDataChanged();
		});

		FilterRowPanel filterRowPanel = new FilterRowPanel(table.getTableHeader());

		// Top panel
		JPanel topBasicPanel = new JPanel() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(x, y, width, height);
				SwingUtilities.invokeLater(this::revalidate);
			}
		};
		topBasicPanel.setOpaque(false);
		List<Component> extraPanels = new ArrayList<>();
		topBasicPanel.setLayout(new WrapLayout(WrapLayout.LEFT, 7, 7));

		{
			JButton refreshButton = new JButton(fixedData ? "Load" : "Refresh");
			refreshButton.addActionListener(e -> updateAll());
			topBasicPanel.add(refreshButton);
		}

		JScrollPane scroller;
		if (!fixedData) {
			JCheckBox autoRefresh = new JCheckBox("Auto Refresh");
			autoRefresh.addItemListener(e -> isAutoRefreshEnabled = autoRefresh.isSelected());
			autoRefresh.setSelected(true);

			stayAtBottom = new JCheckBox("Scroll to Bottom");
			this.scroller = new AutoBottomScrollHelper(table, stayAtBottom::setSelected);
			scroller = this.scroller;
			stayAtBottom.addItemListener(e -> ((AutoBottomScrollHelper) scroller).setAutoScrollEnabled(stayAtBottom.isSelected()));
			stayAtBottom.setSelected(true);
			topBasicPanel.add(autoRefresh);
			topBasicPanel.add(stayAtBottom);
		}
		else {
			scroller = new JScrollPane(table);
			this.scroller = null;
			stayAtBottom = null;
			isAutoRefreshEnabled = true;
		}
		configureScrollHeaderCorner(scroller, table, filterRowPanel);
		List<VisualFilter<? super X>> allFilters = new ArrayList<>();
		filterCreators.stream().map(filterCreator -> filterCreator.apply(this::updateFiltering))
				.filter(Objects::nonNull)
				.forEach(filter -> {
					allFilters.add(filter);
					if (filter instanceof SplitVisualFilter<?> splitFilter) {
						JPanel component = splitFilter.getComponent();
						component.setBorder(new EmptyBorder(0, 7, 7, 7));
						component.setOpaque(false);
						extraPanels.add(component);
						component.setVisible(false);
						JButton button = new JButton("Show/Hide " + splitFilter.getName());
						topBasicPanel.add(button);
						button.addActionListener(l -> component.setVisible(!component.isVisible()));
					}
					else {
						Component component = filter.getComponent();
						if (component instanceof JComponent jc) {
							jc.setOpaque(false);
						}
						topBasicPanel.add(component);
					}

				});
		// Handle column-specific filters
		TableColumnModel tcm = table.getColumnModel();
		for (int i = 0; i < mainColumns.size(); i++) {
			CustomColumn<? super X> col = mainColumns.get(i);
			VisualFilter<? super X> filter = col.getFilter(this::updateFiltering);
			if (filter != null) {
				allFilters.add(filter);
				// Add the filter component to the table header
				filterRowPanel.add(new HeaderFilterWrapper(tcm.getColumn(i), filter.getHeaderComponent()));
			}
		}
		filters = allFilters;
		widgets.stream().map(w -> w.apply(this)).forEach(topBasicPanel::add);

		add(topBasicPanel, c);
		c.gridy++;
		extraPanels.forEach(panel -> {
			add(panel, c);
			c.gridy++;
		});
		c.weighty = 1;

		JTable detailsTable = detailsModel.makeTable();
		JScrollPane detailsScroller = new JScrollPane(detailsTable);
		configureScrollHeaderCorner(detailsScroller, detailsTable, null);
		detailsScroller.setPreferredSize(detailsScroller.getMaximumSize());

		rightClickOptions.configureTable(table, mainModel);

		// If no details, don't bother with a splitpane
		// TODO: also cut out some of the selection logic
		if (detailsColumns.isEmpty()) {
			add(scroller, c);
		}
		else {
			// Split pane
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scroller, detailsScroller);
			add(splitPane, c);
			SwingUtilities.invokeLater(() -> {
				splitPane.setDividerLocation(0.7);
				splitPane.setResizeWeight(1);
				splitPane.setOneTouchExpandable(true);
				updateAll();
			});
		}
	}

	private List<X> doFiltering(List<X> thingsToFilter) {
		long before = System.currentTimeMillis();
		// Parallel should be safe here, because we're still squatting on the EDT, so there shouldn't be concurrent
		// modifications to the visual filters themselves. However, it's a waste if the data set is too small.
		int numberOfThings = thingsToFilter.size();
		List<X> out = ((numberOfThings > 1000) ? thingsToFilter.parallelStream() : thingsToFilter.stream())
				.filter(this::passesFilters)
				.collect(Collectors.toList());
		long after = System.currentTimeMillis();
		long delta = after - before;
		if (delta >= 100) {
			log.warn("Slow filtering for table {}: took {}ms to filter {} items", title, delta, numberOfThings);
		}
		return out;
	}

	public boolean passesFilters(X item) {
		return filters.stream().allMatch(filter -> filter.passesFilter(item));
	}

	private List<X> getFilteredData() {
		return Collections.unmodifiableList(dataFiltered);
	}

	private void filterFully() {
		List<X> raw = dataRaw;
		List<X> out = doFiltering(raw);
		refreshNeeded = RefreshType.FULL;
		dataFiltered = out;
	}

	public CustomTableModel<X> getMainModel() {
		return mainModel;
	}

	public JTable getMainTable() {
		return table;
	}

	public void setAndScrollToSelection(X item) {
		mainModel.setSelectedValue(item);
		mainModel.scrollToSelectedValue();
	}

	public @Nullable JSplitPane getSplitPane() {
		return this.splitPane;
	}

	private enum RefreshType {
		NONE,
		APPEND,
		FULL
	}

	private RefreshType refreshNeeded = RefreshType.FULL;

	private void updateAndFilterData() {
		if (appendOrPruneOnly) {
			List<X> dataBefore = dataRaw;
			int sizeBefore = dataBefore.size();
			List<X> dataAfter = dataGetter.get();
			this.dataRaw = dataAfter;
			int sizeAfter = dataAfter.size();
			if (sizeAfter == 0 || sizeBefore == 0) {
				dataFiltered = doFiltering(dataAfter);
				// A full refresh technically isn't always needed here, but it doesn't really hurt and simplifies the code
				refreshNeeded = RefreshType.FULL;
				return;
			}
			X firstBefore = dataBefore.get(0);
			X firstAfter = dataAfter.get(0);
			if (firstBefore == firstAfter) {
				if (sizeBefore == sizeAfter) {
					refreshNeeded = RefreshType.NONE;
				}
				else {
					List<X> newData = dataAfter.subList(sizeBefore, sizeAfter);
					dataFiltered.addAll(doFiltering(newData));
					refreshNeeded = RefreshType.APPEND;
				}
			}
			else {
				dataFiltered = doFiltering(dataAfter);
				refreshNeeded = RefreshType.FULL;
			}
		}
		else {
			dataRaw = dataGetter.get();
			filterFully();
			refreshNeeded = RefreshType.FULL;
		}
	}


	private void updateModel() {
		switch (refreshNeeded) {
			case NONE:
				return;
			case APPEND:
				mainModel.appendOnlyRefresh();
				break;
			case FULL:
				mainModel.fullRefresh();
				break;
		}
		refreshNeeded = RefreshType.NONE;
	}

	private final AtomicBoolean pendingRefresh = new AtomicBoolean();

	public void signalNewData() {
		if (isAutoRefreshEnabled) {
			// This setup allows for there to be exactly one refresh in progress, and one pending after that
			boolean skipRefresh = pendingRefresh.compareAndExchange(false, true);
			if (!skipRefresh) {
				refresherPool.submit(() -> {
					SwingUtilities.invokeLater(this::updateAll);
					try {
						// Cap updates to 1000/x fps, while not delaying updates
						// if they come in less frequently than that
						Thread.sleep(GuiGlobals.REFRESH_MIN_DELAY);
//						Thread.sleep(50);
					}
					catch (InterruptedException e) {
						// ignored
					}
				});
			}
		}
	}

	private void updateFiltering() {
		refresherPool.submit(() -> {
			Integer offset = mainModel.getSelectedItemViewportOffsetIfVisible();
			filterFully();
			updateModel();
			// Only scroll back to selected instance if auto scroll is disabled
			if (scroller != null && (!scroller.isAutoScrollActive()) && offset != null) {
				mainModel.setVisibleItemScrollOffset(offset);
				log.info("Offset: {}", offset);
			}
		});
	}

	private void updateAll() {
		refresherPool.submit(() -> {
			pendingRefresh.set(false);
			updateAndFilterData();
			updateModel();
		});
	}

	public @Nullable X getCurrentSelection() {
		return currentSelection;
	}

	public static final class TableWithFilterAndDetailsBuilder<X, D> {
		private final String title;
		private final Supplier<List<X>> dataGetter;
		private final List<CustomColumn<? super X>> mainColumns = new ArrayList<>();
		private final List<CustomColumn<? super D>> detailsColumns = new ArrayList<>();
		private final Function<X, List<D>> detailsConverter;
		private final List<Function<Runnable, VisualFilter<? super X>>> filters = new ArrayList<>();
		private final List<Function<TableWithFilterAndDetails<X, ?>, Component>> widgets = new ArrayList<>();
		private BiPredicate<? super X, ? super X> selectionEquivalence = Objects::equals;
		private BiPredicate<? super D, ? super D> detailsSelectionEquivalence = Objects::equals;
		private boolean appendOrPruneOnly;
		private boolean fixedData;
		private RightClickOptionRepo rightClickOptionRepo = RightClickOptionRepo.EMPTY;


		private TableWithFilterAndDetailsBuilder(String title, Supplier<List<X>> dataGetter, Function<X, List<D>> detailsConverter) {
			this.title = title;
			this.dataGetter = dataGetter;
			this.detailsConverter = detailsConverter;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addMainColumn(CustomColumn<? super X> mainColumn) {
			this.mainColumns.add(mainColumn);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addDetailsColumn(CustomColumn<D> detailsColumn) {
			this.detailsColumns.add(detailsColumn);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> withRightClickRepo(RightClickOptionRepo rightClickOptionRepo) {
			this.rightClickOptionRepo = rightClickOptionRepo;
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addFilter(Function<Runnable, VisualFilter<? super X>> filter) {
			if (filter != null) {
				this.filters.add(filter);
			}
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addWidget(Function<TableWithFilterAndDetails<X, ?>, Component> widget) {
			if (widget != null) {
				this.widgets.add(widget);
			}
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> setSelectionEquivalence(BiPredicate<X, X> selectionEquivalence) {
			this.selectionEquivalence = selectionEquivalence;
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> setDetailsSelectionEquivalence(BiPredicate<? super D, ? super D> detailsSelectionEquivalence) {
			this.detailsSelectionEquivalence = detailsSelectionEquivalence;
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> setAppendOrPruneOnly(boolean appendOrPruneOnly) {
			this.appendOrPruneOnly = appendOrPruneOnly;
			return this;
		}

		public TableWithFilterAndDetails<X, D> build() {
			return new TableWithFilterAndDetails<>(title, dataGetter, mainColumns, detailsColumns, detailsConverter, filters, widgets, rightClickOptionRepo, selectionEquivalence, detailsSelectionEquivalence, appendOrPruneOnly, fixedData);
		}

		public TableWithFilterAndDetailsBuilder<X, D> setFixedData(boolean fixedData) {
			setAppendOrPruneOnly(true);
			this.fixedData = fixedData;
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> apply(Consumer<? super TableWithFilterAndDetailsBuilder<X, D>> func) {
			func.accept(this);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> transform(Function<? super TableWithFilterAndDetailsBuilder<X, D>, TableWithFilterAndDetailsBuilder<X, D>> func) {
			return func.apply(this);
		}
	}

	public static <X, D> TableWithFilterAndDetailsBuilder<X, D> builder(String title, Supplier<List<X>> dataGetter) {
		return new TableWithFilterAndDetailsBuilder<>(title, dataGetter, (i) -> Collections.emptyList());
	}

	public static <X, D> TableWithFilterAndDetailsBuilder<X, D> builder(String title, Supplier<List<X>> dataGetter, Function<X, List<D>> detailsConverter) {
		return new TableWithFilterAndDetailsBuilder<>(title, dataGetter, detailsConverter);
	}

	public void setBottomScroll(boolean value) {
		if (stayAtBottom != null) {
			stayAtBottom.setSelected(value);
		}
	}

	public void setEditMode(EditMode editMode) {
		this.editMode = editMode;
	}
	
	/**
	 * Configures the scroll pane for the table, including setting up the custom header corner,
	 * making components transparent, and applying the custom border.
	 *
	 * @param scroller the scroll pane to configure
	 * @param table    the table within the scroll pane
	 */
	private void configureScrollHeaderCorner(JScrollPane scroller, JTable table, @Nullable Component filterRow) {
		JTableHeader tableHeader = table.getTableHeader();
		if (tableHeader != null) {
			if (filterRow == null) {
				setColumnHeader(scroller, tableHeader);
			}
			else {
				log.debug("Configuring combined header for table");
				JPanel combined = new JPanel(new BorderLayout()) {
					@Override
					public Color getBackground() {
						return TableWithFilterAndDetails.this.getBackground();
					}
				};
				combined.setOpaque(true);
				combined.add(filterRow, BorderLayout.NORTH);
				combined.add(tableHeader, BorderLayout.CENTER);
				setColumnHeader(scroller, combined);
			}
		}
		scroller.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new HeaderCorner(tableHeader));
		scroller.setOpaque(false);
		// Note: we do NOT set the main viewport to non-opaque, because we want the table area
		// to have its normal background.
		table.setFillsViewportHeight(true);
		JViewport columnHeader = scroller.getColumnHeader();
		if (columnHeader != null) {
			columnHeader.setOpaque(false);
		}
	}

	private static void setColumnHeader(JScrollPane scroller, Component view) {
		log.debug("Setting column header view for scroller {}: {}", scroller, view);
		if (scroller instanceof AutoBottomScrollHelper helper) {
			helper.setColumnHeaderViewLocked(view);
		}
		else {
			scroller.setColumnHeaderView(view);
		}
	}

	/**
	 * Height in pixels for the filter component area in the table header.
	 */
	private static final int HEADER_FILTER_HEIGHT = 32;

	private static class FilterRowPanel extends JPanel {
		private final JTableHeader header;

		FilterRowPanel(JTableHeader header) {
			this.header = header;
			setLayout(new HeaderFilterLayout(header));
			setOpaque(false);
			header.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
				@Override
				public void columnAdded(TableColumnModelEvent e) {
					sync();
				}

				@Override
				public void columnRemoved(TableColumnModelEvent e) {
					sync();
				}

				@Override
				public void columnMoved(TableColumnModelEvent e) {
					sync();
				}

				@Override
				public void columnMarginChanged(ChangeEvent e) {
					sync();
				}

				@Override
				public void columnSelectionChanged(ListSelectionEvent e) {
				}

				private void sync() {
					revalidate();
					repaint();
				}
			});
			// Repaint when dragging
			header.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					revalidate();
					repaint();
				}
			});
			header.addPropertyChangeListener(e -> {
				if ("draggedColumn".equals(e.getPropertyName())) {
					TableColumn column = (TableColumn) e.getNewValue();
					if (column != null) {
						for (Component comp : getComponents()) {
							if (comp instanceof HeaderFilterWrapper wrapper && wrapper.getColumn() == column) {
								setComponentZOrder(comp, 0);
								break;
							}
						}
					}
					revalidate();
					repaint();
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			if (getComponentCount() > 0) {
				return new Dimension(header.getPreferredSize().width, HEADER_FILTER_HEIGHT);
			}
			return new Dimension(header.getPreferredSize().width, 0);
		}
	}

	/**
	 * Layout manager for the JTableHeader that positions filter components above
	 * the standard header labels.
	 */
	private static class HeaderFilterLayout implements LayoutManager {
		private final JTableHeader header;

		public HeaderFilterLayout(JTableHeader header) {
			this.header = header;
		}

		@Override
		public void addLayoutComponent(String name, Component comp) {
		}

		@Override
		public void removeLayoutComponent(Component comp) {
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			return parent.getPreferredSize();
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return parent.getMinimumSize();
		}

		@Override
		public void layoutContainer(Container parent) {
			TableColumnModel tcm = header.getColumnModel();
			for (Component comp : parent.getComponents()) {
				if (comp instanceof HeaderFilterWrapper wrapper) {
					TableColumn column = wrapper.getColumn();
					int viewIndex = -1;
					for (int i = 0; i < tcm.getColumnCount(); i++) {
						if (tcm.getColumn(i) == column) {
							viewIndex = i;
							break;
						}
					}
					if (viewIndex != -1) {
						Rectangle rect = header.getHeaderRect(viewIndex);
						int x = rect.x;
						// Account for the distance the column is being dragged
						if (column == header.getDraggedColumn()) {
							x += header.getDraggedDistance();
						}
						comp.setBounds(x, 0, rect.width, HEADER_FILTER_HEIGHT);
					}
				}
			}
		}
	}

	/**
	 * A wrapper component that associates a filter component with a TableColumn.
	 * This is used by HeaderFilterLayout to correctly position filters.
	 */
	private static class HeaderFilterWrapper extends JPanel {
		private final TableColumn column;

		public HeaderFilterWrapper(TableColumn column, Component filter) {
			super(new BorderLayout());
			this.column = column;
			add(filter, BorderLayout.CENTER);
			setBorder(new EmptyBorder(3, 2, 5, 2));
			setOpaque(false);
		}

		public TableColumn getColumn() {
			return column;
		}
	}

	/**
	 * A corner component for the JScrollPane that matches the header's look.
	 */
	private static class HeaderCorner extends JPanel {
		private final JTableHeader header;

		HeaderCorner(JTableHeader header) {
			this.header = header;
			setOpaque(false);
		}

		@Override
		public Color getBackground() {
			return (header == null) ? super.getBackground() : header.getBackground();
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (header != null) {
				g.setColor(header.getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			Color separatorColor = UIManager.getColor("TableHeader.separatorColor");
			if (separatorColor == null) {
				separatorColor = UIManager.getColor("TableHeader.bottomSeparatorColor");
			}
			if (separatorColor != null) {
				g.setColor(separatorColor);
				g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
			}
		}
	}
}
