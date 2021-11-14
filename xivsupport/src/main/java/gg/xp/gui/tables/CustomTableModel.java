package gg.xp.gui.tables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CustomTableModel<X> extends AbstractTableModel {

	private static final Logger log = LoggerFactory.getLogger(CustomTableModel.class);

	public static class CustomTableModelBuilder<B> {
		private final Supplier<List<B>> dataGetter;
		private final List<CustomColumn<B>> columns = new ArrayList<>();
		private BiPredicate<B, B> selectionEquivalence = Objects::equals;

		private CustomTableModelBuilder(Supplier<List<B>> dataGetter) {
			this.dataGetter = dataGetter;
		}

		public CustomTableModelBuilder<B> addColumn(CustomColumn<B> colDef) {
			columns.add(colDef);
			return this;
		}

		public CustomTableModel<B> build() {
			return new CustomTableModel<>(dataGetter, columns, selectionEquivalence);
		}

		public CustomTableModelBuilder<B> setSelectionEquivalence(BiPredicate<B, B> selectionEquivalence) {
			this.selectionEquivalence = selectionEquivalence;
			return this;
		}
	}

	public static <B> CustomTableModelBuilder<B> builder(Supplier<List<B>> dataGetter) {
		return new CustomTableModelBuilder<>(dataGetter);
	}


	private final Supplier<List<X>> dataGetter;
	private final List<CustomColumn<X>> columns;
	private List<X> data = Collections.emptyList();
	private final BiPredicate<X, X> selectionEquivalence;
	// TODO
	private final boolean appendFastPathOk = true;


	private CustomTableModel(Supplier<List<X>> dataGetter, List<CustomColumn<X>> columns, BiPredicate<X, X> selectionEquivalence) {
		this.dataGetter = dataGetter;
		this.columns = columns;
		this.selectionEquivalence = selectionEquivalence;
		fullRefresh();
	}

	public void appendOnlyRefresh() {
		JTable table = getTable();
		if (table == null) {
			data = dataGetter.get();
			// This shouldn't really happen anyway, no need to optimize
			fireTableDataChanged();
		}
		else {
			if (data.isEmpty()) {
				// Fast path for when data is currently empty
				fireTableDataChanged();
				return;
			}
			ListSelectionModel selectionModel = table.getSelectionModel();
			int oldSize = data.size();
			int[] oldSelectionIndices = selectionModel.getSelectedIndices();
			List<X> oldSelections = Arrays.stream(oldSelectionIndices)
					.mapToObj(i -> data.get(i))
					.collect(Collectors.toList());
			// TODO: smarter data provider that informs us of append-only operations
			data = dataGetter.get();
			int newSize = data.size();
			fireTableRowsInserted(oldSize, newSize - 1);
			// fast path for typical case where data is only appended and we only have a single selection
			if (oldSelections.size() == 1) {
				X theItem = oldSelections.get(0);
				int theIndex = oldSelectionIndices[0];
				if (theIndex >= data.size()) {
					return;
				}
				if (data.get(theIndex) == theItem) {
					selectionModel.addSelectionInterval(theIndex, theIndex);
					return;
				}
			}
			for (X oldItem : oldSelections) {
				for (int i = 0; i < data.size(); i++) {
					X newItem = data.get(i);
					if (selectionEquivalence.test(oldItem, newItem)) {
						selectionModel.addSelectionInterval(i, i);
						break;
					}
				}
			}
		}

	}

	public void fullRefresh() {
		JTable table = getTable();
		if (table == null) {
			data = dataGetter.get();
			fireTableDataChanged();
		}
		else {
			if (data.isEmpty()) {
				// Fast path for when data is currently empty
				data = dataGetter.get();
				fireTableDataChanged();
				return;
			}
			ListSelectionModel selectionModel = table.getSelectionModel();
			int[] oldSelectionIndices = selectionModel.getSelectedIndices();
			List<X> oldSelections = Arrays.stream(oldSelectionIndices)
					.mapToObj(i -> data.get(i))
					.collect(Collectors.toList());
			// TODO: smarter data provider that informs us of append-only operations
			data = dataGetter.get();
			fireTableDataChanged();
			for (X oldItem : oldSelections) {
				for (int i = 0; i < data.size(); i++) {
					X newItem = data.get(i);
					if (selectionEquivalence.test(oldItem, newItem)) {
						selectionModel.addSelectionInterval(i, i);
						break;
					}
				}
			}
		}
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	// TODO: this is technically wrong since multiple tables can have the same model instance
	private JTable getTable() {
		return (JTable) Arrays.stream(getTableModelListeners()).filter(JTable.class::isInstance).findFirst().orElse(null);
	}

//	@Override
//	public void fireTableDataChanged() {
//		JTable table = getTable();
//		if (table == null) {
//			log.warn("Did not find an associated table");
//			super.fireTableDataChanged();
//		}
//		else {
//			Arrays.stream(table.getSelectionModel().getSelectedIndices())
//
//			super.fireTableDataChanged();
//
//		}
//	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	@Override
	public String getColumnName(int column) {
		return columns.get(column).getColumnName();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	public X getValueForRow(int row) {
		return data.get(row);
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		X item = data.get(rowIndex);
		try {
			return columns.get(columnIndex).getValue(item);
		}
		catch (Throwable e) {
			log.error("ERROR getting value for row {} col {} value {}", rowIndex, columnIndex, item, e);
			return "INTERNAL ERROR";
		}
	}
}
