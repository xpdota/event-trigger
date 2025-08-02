package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class TriggerTreeEditor extends AbstractCellEditor implements TreeCellEditor {

	private final EasyTriggersTreeRenderer renderer = new EasyTriggersTreeRenderer();
	private final JTree tree;

	public TriggerTreeEditor(JTree tree) {
		this.tree = tree;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public boolean isCellEditable(EventObject event) {
		if (event instanceof MouseEvent mouseEvent) {
			TreePath path = tree.getPathForLocation(mouseEvent.getX(),
					mouseEvent.getY());
			if (path != null) {
				Object node = path.getLastPathComponent();
				return node instanceof BaseTrigger<?>;
			}
		}
		return false;
	}

	@Override
	public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
		Component editor = renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
		ItemListener itemListener = itemEvent -> {
			if (stopCellEditing()) {
				fireEditingStopped();
			}
		};
		if (editor instanceof CheckboxTreeNode ctn) {
			ctn.getCheckBox().addItemListener(itemListener);
		}
		return editor;
	}
}
