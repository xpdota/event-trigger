package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class EasyTriggersTreeRenderer extends DefaultTreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof BaseTrigger<?> bt) {
			return new CheckboxTreeNode(tree, bt, selected, expanded, leaf, row, hasFocus);
		}
		else if (value instanceof EasyTriggers) {
			return super.getTreeCellRendererComponent(tree, "Triggers", sel, expanded, leaf, row, hasFocus);
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}

	@Override
	public boolean isVisible() {
		return false;
	}
}
