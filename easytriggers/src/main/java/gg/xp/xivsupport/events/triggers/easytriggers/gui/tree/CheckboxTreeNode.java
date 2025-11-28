package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.FailedDeserializationTrigger;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.gui.util.ColorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

public class CheckboxTreeNode extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(CheckboxTreeNode.class);
	private final JCheckBox checkBox;
	private final BaseTrigger<?> item;
	private final boolean isSelected;
	private final DefaultTreeCellRenderer defaultRenderer;

	public CheckboxTreeNode(JTree tree, BaseTrigger<?> item, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		String stringLabel = item.getName();
		checkBox = new JCheckBox();
		DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
		this.defaultRenderer = defaultRenderer;
		Component label = defaultRenderer.getTreeCellRendererComponent(tree, stringLabel, selected, expanded, leaf, row, false);
		if (item instanceof FailedDeserializationTrigger) {
			label.setForeground(Color.RED);
		}
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(checkBox);
		add(label);
		setOpaque(false);
		checkBox.setSelected(item.isEnabled());
		checkBox.addItemListener(event -> item.setEnabled(((JCheckBox) event.getSource()).isSelected()));
		this.item = item;
		this.isSelected = selected;

	}

	@Override
	protected void paintChildren(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		super.paintChildren(g);
		if (!item.isEnabled() || item.isDisabledByParent()) {
			// This is kind of janky but I haven't found a better way.
			// Forcing transparency with AlphaComposite directly leads to weird font rendering issues.
			// The same thing happens if you try to proxy via a BufferedImage.
			Composite old = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			Color baseColor = isSelected ? defaultRenderer.getBackgroundSelectionColor() : defaultRenderer.getBackgroundNonSelectionColor();
			g2d.setColor(RenderUtils.withAlpha(baseColor, 90));
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.setComposite(old);
		}
	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}

	@Override
	public boolean isVisible() {
		// TODO: remove?
		return false;
	}
}
