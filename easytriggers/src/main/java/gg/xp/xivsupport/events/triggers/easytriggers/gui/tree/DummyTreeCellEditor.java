package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.util.EventObject;

/**
 * Class that lets us use a workaround to <a href="https://stackoverflow.com/a/22348852">this issue</a>.
 * <p>
 * We use an artificially-started edit + immediate cancel to get the BasicTreeUI to realize that the cell size
 * has changed after we change the name of a trigger.
 */
public class DummyTreeCellEditor implements TreeCellEditor {
	@Override
	public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
		return new JLabel(".");
	}

	@Override
	public Object getCellEditorValue() {
		return "";
	}

	@Override
	public boolean isCellEditable(EventObject event) {
		// if event is null, it is an artificially-initiated edit, which is what we're using this hack for.
		return event == null;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		return true;
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
