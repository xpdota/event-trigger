package gg.xp.xivsupport.gui.tree;

import gg.xp.reevent.topology.TopoItem;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class TopologyTreeEditor extends AbstractCellEditor implements TreeCellEditor {

	private final TopologyTreeRenderer renderer = new TopologyTreeRenderer();
	private final JTree tree;

	public TopologyTreeEditor(JTree tree) {
		this.tree = tree;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public boolean isCellEditable(EventObject event) {
		boolean returnValue = false;
		if (event instanceof MouseEvent mouseEvent) {
			TreePath path = tree.getPathForLocation(mouseEvent.getX(),
					mouseEvent.getY());
			if (path != null) {
				Object node = path.getLastPathComponent();
				if ((node instanceof TopoItem)) {
					return ((TopoItem) node).canBeDisabled();
				}
			}
		}
		return returnValue;
	}

	@Override
	public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
		Component editor = renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
		ItemListener itemListener = itemEvent -> {
			if (stopCellEditing()) {
				fireEditingStopped();
			}
		};
		if (editor instanceof CheckboxTreeNode) {
			((CheckboxTreeNode) editor).getCheckBox().addItemListener(itemListener);
		}
		return editor;
	}
}
