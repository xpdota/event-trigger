package gg.xp.gui.tables;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomTableModel<X> extends AbstractTableModel {


	public static class CustomTableModelBuilder<B> {
		private final Supplier<List<B>> dataGetter;
		private final List<CustomColumn<B>> columns = new ArrayList<>();
		private Consumer<B> rowSelectedCallback = b -> {};

		private CustomTableModelBuilder(Supplier<List<B>> dataGetter) {
			this.dataGetter = dataGetter;
		}

		public CustomTableModelBuilder<B> addColumn(CustomColumn<B> colDef) {
			columns.add(colDef);
			return this;
		}

		public CustomTableModelBuilder<B> rowSelectedCallback(Consumer<B> callback) {
			rowSelectedCallback = callback;
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
	private CustomTableModel(Supplier<List<X>> dataGetter, List<CustomColumn<X>> columns) {
		this.dataGetter = dataGetter;
		this.columns = columns;
		refresh();
	}

	public void refresh() {
		int sizeBefore = data.size();
		data = dataGetter.get();
		int sizeAfter = data.size();
		fireTableRowsInserted(sizeBefore, sizeAfter - 1);
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

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



	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		X item = data.get(rowIndex);
		return columns.get(columnIndex).getValue(item);
	}


}
