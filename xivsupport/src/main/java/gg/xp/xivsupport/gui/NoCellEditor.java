package gg.xp.xivsupport.gui;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

public class NoCellEditor implements TableCellEditor {
	public static final NoCellEditor INSTANCE = new NoCellEditor();
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		SwingUtilities.invokeLater(() -> table.getCellEditor().stopCellEditing());
		return table.getCellRenderer(row, column).getTableCellRendererComponent(table, value, isSelected, true, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return false;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		return false;
	}

	@Override
	public void cancelCellEditing() {

	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {

	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {

	}
}
