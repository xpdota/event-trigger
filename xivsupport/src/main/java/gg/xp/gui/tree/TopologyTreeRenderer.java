package gg.xp.gui.tree;

import gg.xp.topology.TopoItem;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class TopologyTreeRenderer implements TreeCellRenderer {

	private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof TopoItem) {
//				checkBox.setEnabled(item.canBeDisabled());
			return new CheckboxTreeNode(tree, (TopoItem) value, selected, expanded, leaf, row, hasFocus);
		}
		else {
			return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}
	}
}
