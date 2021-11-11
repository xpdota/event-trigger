package gg.xp.gui.tables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CustomTableModel<X> extends AbstractTableModel {

	private static final Logger log = LoggerFactory.getLogger(CustomTableModel.class);

	public static class CustomTableModelBuilder<B> {
		private final Supplier<List<B>> dataGetter;
		private final List<CustomColumn<B>> columns = new ArrayList<>();

		private CustomTableModelBuilder(Supplier<List<B>> dataGetter) {
			this.dataGetter = dataGetter;
		}

		public CustomTableModelBuilder<B> addColumn(CustomColumn<B> colDef) {
			columns.add(colDef);
			return this;
		}

		public CustomTableModel<B> build() {
			return new CustomTableModel<>(dataGetter, columns);
		}
	}

	public static <B> CustomTableModelBuilder<B> builder(Supplier<List<B>> dataGetter) {
		return new CustomTableModelBuilder<>(dataGetter);
	}


	private final Supplier<List<X>> dataGetter;
	private final List<CustomColumn<X>> columns;
	private List<X> data = Collections.emptyList();

	// TODO: will this keep selection?
	public CustomTableModel(Supplier<List<X>> dataGetter, List<CustomColumn<X>> columns) {
		this.dataGetter = dataGetter;
		this.columns = columns;
		refresh();
	}

	public void refresh() {
		JTable table = getTable();
		if (table == null) {
			data = dataGetter.get();
			fireTableDataChanged();
		}
		else {
			ListSelectionModel selectionModel = table.getSelectionModel();
			List<X> oldSelections = Arrays.stream(selectionModel.getSelectedIndices())
					.mapToObj(i -> data.get(i))
					.collect(Collectors.toList());
			data = dataGetter.get();
			fireTableDataChanged();
			oldSelections.stream()
					.mapToInt(o -> data.indexOf(o))
					.filter(i -> i != -1)
							.forEach(i -> selectionModel.addSelectionInterval(i, i));
		}
//
//
//
//		int sizeBefore = data.size();
//		int sizeAfter = data.size();
//		fireTableRowsInserted(sizeBefore, sizeAfter - 1);
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
