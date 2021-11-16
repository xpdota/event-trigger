package gg.xp.xivsupport.gui.tree;

import gg.xp.reevent.topology.TopoItem;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CheckboxTreeNode extends JPanel {

	private final JCheckBox checkBox;

	public CheckboxTreeNode(JTree tree, TopoItem item, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		String stringLabel = item.getName();
		checkBox = new JCheckBox();
		boolean isActuallySelected = selected || hasFocus;
		TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
		Component label = defaultRenderer.getTreeCellRendererComponent(tree, stringLabel, isActuallySelected, expanded, leaf, row, isActuallySelected);
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(checkBox);
		add(label);
		setOpaque(false);
		setSize(label.getPreferredSize());
		setMaximumSize(label.getPreferredSize());
		setMinimumSize(label.getPreferredSize());
		checkBox.setEnabled(item.isEnabledByParent());
		checkBox.setSelected(item.isEnabledDirectly());
		// TODO for testing
		checkBox.setOpaque(false);
		checkBox.addItemListener(event -> item.setEnabledDirectly(((JCheckBox) event.getSource()).isSelected()));

	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}
}
