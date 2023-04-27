package gg.xp.xivsupport.gui.tree;

import gg.xp.reevent.topology.TopoItem;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class TopologyTreeRenderer implements TreeCellRenderer {

	private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof TopoItem ti) {
//				checkBox.setEnabled(instance.canBeDisabled());
			return new CheckboxTreeNode(tree, ti, selected, expanded, leaf, row, hasFocus);
		}
		return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
	}
}
