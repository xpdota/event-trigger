package gg.xp.gui.tables;

import gg.xp.gui.TitleBorderFullsizePanel;
import gg.xp.gui.WrapLayout;
import gg.xp.gui.tables.filters.VisualFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TableWithFilterAndDetails<X, D> extends TitleBorderFullsizePanel {

	private static final Logger log = LoggerFactory.getLogger(TableWithFilterAndDetails.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();

	private final Supplier<List<X>> dataGetter;
	private final List<VisualFilter<? super X>> filters;
	private final CustomTableModel<X> mainModel;
	private volatile X currentSelection;
	private List<X> dataRaw = Collections.emptyList();
	private List<X> dataFiltered = Collections.emptyList();
	private volatile boolean isAutoRefreshEnabled;
	private final boolean appendOrPruneOnly;
	private final String title;

	private TableWithFilterAndDetails(
			String title,
			Supplier<List<X>> dataGetter,
			List<CustomColumn<X>> mainColumns,
			List<CustomColumn<D>> detailsColumns,
			Function<X, List<D>> detailsConverter,
			List<Function<Runnable, VisualFilter<? super X>>> filterCreators,
			BiPredicate<X, X> selectionEquivalence,
			boolean appendOrPruneOnly) {
		super(title);
		this.title = title;
		// TODO: add count of events
		this.dataGetter = dataGetter;
		this.appendOrPruneOnly = appendOrPruneOnly;
		setLayout(new BorderLayout());

		CustomTableModel.CustomTableModelBuilder<D> detailsBuilder = CustomTableModel.builder(() -> detailsConverter.apply(this.currentSelection));
		detailsColumns.forEach(detailsBuilder::addColumn);
		CustomTableModel<D> detailsModel = detailsBuilder
				.build();


		CustomTableModel.CustomTableModelBuilder<X> mainBuilder = CustomTableModel.builder(this::getFilteredData);
		mainColumns.forEach(mainBuilder::addColumn);
		mainBuilder.setSelectionEquivalence(selectionEquivalence);
		mainModel = mainBuilder.build();


		// Main table
		JTable table = new JTable(mainModel);
		for (int i = 0; i < mainColumns.size(); i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			CustomColumn<X> customColumn = mainColumns.get(i);
			customColumn.configureColumn(column);
		}
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel selectionModel = table.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			// TODO: bug where you mess up your selection by selecting a row in the details table
			int[] selected = selectionModel.getSelectedIndices();
			log.trace("Selected: {}", Arrays.toString(selected));
			if (selected.length == 0) {
				currentSelection = null;
			}
			else {
				currentSelection = mainModel.getValueForRow(selected[0]);
			}
			detailsModel.fullRefresh();
			detailsModel.fireTableDataChanged();
		});
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(e -> {
			updateAll();
		});
		JCheckBox autoRefresh = new JCheckBox("Auto Refresh");
		autoRefresh.addItemListener(e -> {
			isAutoRefreshEnabled = autoRefresh.isSelected();
		});
		autoRefresh.setSelected(true);

		JCheckBox stayAtBottom = new JCheckBox("Scroll to Bottom");
		AutoBottomScrollHelper scroller = new AutoBottomScrollHelper(table, () -> stayAtBottom.setSelected(false));
		stayAtBottom.addItemListener(e -> scroller.setAutoScrollEnabled(stayAtBottom.isSelected()));
		stayAtBottom.setSelected(true);

		// Top panel
		JPanel topPanel = new JPanel();
		topPanel.add(refreshButton);
		topPanel.add(autoRefresh);
		topPanel.add(stayAtBottom);
		topPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		filters = filterCreators.stream().map(filterCreator -> filterCreator.apply(this::updateFiltering)).collect(Collectors.toList());
		filters.forEach(filter -> topPanel.add(filter.getComponent()));
		add(topPanel, BorderLayout.PAGE_START);


		JTable detailsTable = new JTable(detailsModel);
		JScrollPane detailsScroller = new JScrollPane(detailsTable);
		detailsScroller.setPreferredSize(detailsScroller.getMaximumSize());

		// Split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scroller, detailsScroller);
		add(splitPane);
		SwingUtilities.invokeLater(() -> {
			splitPane.setDividerLocation(0.7);
			splitPane.setResizeWeight(1);
			updateAll();
		});
	}

	private List<X> doFiltering(List<X> thingsToFilter) {
		long before = System.currentTimeMillis();
		// Parallel should be safe here, because we're still squatting on the EDT, so there shouldn't be concurrent
		// modifications to the visual filters themselves. However, it's a waste if the data set is too small.
		int numberOfThings = thingsToFilter.size();
		List<X> out = ((numberOfThings > 1000) ? thingsToFilter.parallelStream() : thingsToFilter.stream())
				.filter(event -> filters.stream()
						.allMatch(filter -> filter.passesFilter(event)))
				.collect(Collectors.toList());
		long after = System.currentTimeMillis();
		long delta = after - before;
		if (delta >= 3) {
			log.warn("Slow filtering for table {}: took {}ms to filter {} items", title, delta, numberOfThings);
		}
		return out;
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
				// A full refresh technically isn't always needed here, but it doesn't really and simplifies the code
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
				break;
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
				exs.submit(() -> {
					SwingUtilities.invokeLater(this::updateAll);
					try {
						// Cap updates to 100 fps, while not delaying updates
						// if they come in less frequent
						Thread.sleep(10);
					}
					catch (InterruptedException e) {
						// ignored
					}
				});
			}
		}
	}

	private void updateFiltering() {
		filterFully();
		updateModel();
	}

	private void updateAll() {
		pendingRefresh.set(false);
		updateAndFilterData();
		updateModel();
	}

	public static final class TableWithFilterAndDetailsBuilder<X, D> {
		private final String title;
		private final Supplier<List<X>> dataGetter;
		private final List<CustomColumn<X>> mainColumns = new ArrayList<>();
		private final List<CustomColumn<D>> detailsColumns = new ArrayList<>();
		private final Function<X, List<D>> detailsConverter;
		private final List<Function<Runnable, VisualFilter<? super X>>> filters = new ArrayList<>();
		private BiPredicate<X, X> selectionEquivalence = Objects::equals;
		private boolean appendOrPruneOnly;


		private TableWithFilterAndDetailsBuilder(String title, Supplier<List<X>> dataGetter, Function<X, List<D>> detailsConverter) {
			this.title = title;
			this.dataGetter = dataGetter;
			this.detailsConverter = detailsConverter;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addMainColumn(CustomColumn<X> mainColumn) {
			this.mainColumns.add(mainColumn);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addDetailsColumn(CustomColumn<D> detailsColumn) {
			this.detailsColumns.add(detailsColumn);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> addFilter(Function<Runnable, VisualFilter<? super X>> filter) {
			this.filters.add(filter);
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> setSelectionEquivalence(BiPredicate<X, X> selectionEquivalence) {
			this.selectionEquivalence = selectionEquivalence;
			return this;
		}

		public TableWithFilterAndDetailsBuilder<X, D> setAppendOrPruneOnly(boolean appendOrPruneOnly) {
			this.appendOrPruneOnly = appendOrPruneOnly;
			return this;
		}

		public TableWithFilterAndDetails<X, D> build() {
			return new TableWithFilterAndDetails<>(title, dataGetter, mainColumns, detailsColumns, detailsConverter, filters, selectionEquivalence, appendOrPruneOnly);
		}

	}

	public static <X, D> TableWithFilterAndDetailsBuilder<X, D> builder(String title, Supplier<List<X>> dataGetter, Function<X, List<D>> detailsConverter) {
		return new TableWithFilterAndDetailsBuilder<>(title, dataGetter, detailsConverter);
	}
}
