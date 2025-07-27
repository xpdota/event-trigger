package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CheckboxTreeNode extends JPanel {

	private final JCheckBox checkBox;

	public CheckboxTreeNode(JTree tree, BaseTrigger<?> item, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		String stringLabel = item.getName();
		checkBox = new JCheckBox();
		boolean isActuallySelected = selected || hasFocus;
		TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
//		Component label = defaultRenderer.getTreeCellRendererComponent(tree, stringLabel, isActuallySelected, expanded, leaf, row, isActuallySelected);
		Component label = defaultRenderer.getTreeCellRendererComponent(tree, stringLabel, isActuallySelected, expanded, leaf, row, hasFocus);
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(checkBox);
		add(label);
		setOpaque(false);
		setSize(label.getPreferredSize());
		setMaximumSize(label.getPreferredSize());
		setMinimumSize(label.getPreferredSize());
		// TODO
//		checkBox.setEnabled(item.isEnabledByParent());
		checkBox.setSelected(item.isEnabled());
//		checkBox.setOpaque(false);
		checkBox.addItemListener(event -> item.setEnabled(((JCheckBox) event.getSource()).isSelected()));

	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}

	@Override
	public boolean isVisible() {
		return false;
	}
}
