package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import java.awt.*;

public class DotBarRenderer extends ResourceBarRenderer {

	private static final Color colorExpired = new Color(255, 0, 0);
	private static final Color colorGood = new Color(79, 211, 255);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (tableCellRendererComponent instanceof JComponent) {
			((JComponent) tableCellRendererComponent).setOpaque(false);
		}
		return tableCellRendererComponent;
	}

	@Override
	protected void formatLabel(CurrentMaxPair item) {
		if (item instanceof LabelOverride) {
			bar.setTextOptions(((LabelOverride) item).getLabel());
		}
		else {
			super.formatLabel(item);
		}
	}

	@Override
	protected Color getBarColor(double percent, CurrentMaxPair item) {
		if (percent > 0.999d) {
			return colorExpired;
		}
		return colorGood;
	}
}
