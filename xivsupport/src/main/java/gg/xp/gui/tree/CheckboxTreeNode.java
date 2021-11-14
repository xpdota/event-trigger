package gg.xp.gui.tree;

import gg.xp.topology.TopoItem;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CheckboxTreeNode extends JPanel {

	private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
	private final JCheckBox checkBox;

	public CheckboxTreeNode(JTree tree, TopoItem item, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		String stringLabel = item.getName();
		checkBox = new JCheckBox();
		boolean isActuallySelected = selected || hasFocus;
		Component label = defaultRenderer.getTreeCellRendererComponent(tree, stringLabel, isActuallySelected, expanded, leaf, row, isActuallySelected);
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(checkBox);
		add(label);
		setOpaque(false);
		setSize(label.getPreferredSize());
		setMaximumSize(label.getPreferredSize());
		setMinimumSize(label.getPreferredSize());
		checkBox.setSelected(item.isEnabled());
		// TODO for testing
		checkBox.setEnabled(true);
		checkBox.setOpaque(false);
		checkBox.addItemListener(event -> item.setEnabled(((JCheckBox) event.getSource()).isSelected()));

	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}
}
