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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TableWithFilterAndDetails<X, D> extends TitleBorderFullsizePanel {

	private static final Logger log = LoggerFactory.getLogger(TableWithFilterAndDetails.class);

	private final Supplier<List<X>> dataGetter;
	private final List<VisualFilter<X>> filters;
	private final CustomTableModel<X> mainModel;
	private volatile X currentSelection;
	private List<X> dataRaw = Collections.emptyList();
	private volatile boolean isAutoRefreshEnabled;

	private TableWithFilterAndDetails(
			String title,
			Supplier<List<X>> dataGetter,
			List<CustomColumn<X>> mainColumns,
			List<CustomColumn<D>> detailsColumns,
			Function<X, List<D>> detailsConverter,
			List<Function<Runnable, VisualFilter<X>>> filterCreators
	) {
		super(title);
		// TODO: add count of events
		this.dataGetter = dataGetter;
		setLayout(new BorderLayout());

		CustomTableModel.CustomTableModelBuilder<D> detailsBuilder = CustomTableModel.builder(() -> detailsConverter.apply(this.currentSelection));
		detailsColumns.forEach(detailsBuilder::addColumn);
		CustomTableModel<D> detailsModel = detailsBuilder
				.build();


		CustomTableModel.CustomTableModelBuilder<X> mainBuilder = CustomTableModel.builder(this::getFilteredData);
		mainColumns.forEach(mainBuilder::addColumn);
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
			int[] selected = selectionModel.getSelectedIndices();
			log.trace("Selected: {}", Arrays.toString(selected));
			if (selected.length == 0) {
				currentSelection = null;
			}
			else {
				currentSelection = mainModel.getValueForRow(selected[0]);
			}
			detailsModel.refresh();
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
		filters = filterCreators.stream().map(filterCreator -> filterCreator.apply(mainModel::refresh)).collect(Collectors.toList());
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

	private List<X> getFilteredData() {
		List<X> raw = dataRaw;
		return raw.stream()
				.filter(event -> filters.stream()
						.allMatch(filter -> filter.passesFilter(event)))
				.collect(Collectors.toList());
	}

	private void updateDataOnly() {
		dataRaw = dataGetter.get();
	}

	private void updateModel() {
		mainModel.refresh();
	}

	public void signalUpdate() {
		if (isAutoRefreshEnabled) {
			SwingUtilities.invokeLater(this::updateAll);
		}
	}

	private void updateAll() {
		updateDataOnly();
		updateModel();
	}

	public static final class TableWithFilterAndDetailsBuilder<X, D> {
		private final String title;
		private final Supplier<List<X>> dataGetter;
		private final List<CustomColumn<X>> mainColumns = new ArrayList<>();
		private final List<CustomColumn<D>> detailsColumns = new ArrayList<>();
		private final Function<X, List<D>> detailsConverter;
		private final List<Function<Runnable, VisualFilter<X>>> filters = new ArrayList<>();


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

		public TableWithFilterAndDetailsBuilder<X, D> addFilter(Function<Runnable, VisualFilter<X>> filter) {
			this.filters.add(filter);
			return this;
		}


		public TableWithFilterAndDetails<X, D> build() {
			return new TableWithFilterAndDetails<>(title, dataGetter, mainColumns, detailsColumns, detailsConverter, filters);
		}
	}

	public static <X, D> TableWithFilterAndDetailsBuilder<X, D> builder(String title, Supplier<List<X>> dataGetter, Function<X, List<D>> detailsConverter) {
		return new TableWithFilterAndDetailsBuilder<>(title, dataGetter, detailsConverter);
	}
}
