package gg.xp.xivsupport.timelines.gui;

import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.timelines.TimelineEntry;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.function.Predicate;

public class RowConditionalTableCellEditor<X> extends AbstractCellEditor implements TableCellEditor  {

	private final TableCellEditor wrapped;
	private final Predicate<X> enabledFor;

	public RowConditionalTableCellEditor(TableCellEditor wrapped, Predicate<X> enabledFor) {
		this.wrapped = wrapped;
		this.enabledFor = enabledFor;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		X rowVal = ((CustomTableModel<X>) table.getModel()).getValueForRow(row);
		if (!enabledFor.test(rowVal)) {
			return NoCellEditor.INSTANCE.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		return wrapped.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		return wrapped.getCellEditorValue();
	}
}
