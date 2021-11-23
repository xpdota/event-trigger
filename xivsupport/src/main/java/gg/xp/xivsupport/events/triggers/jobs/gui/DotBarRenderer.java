package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import java.awt.*;

public class DotBarRenderer extends ResourceBarRenderer {

	private static final Color colorExpired = new Color(255, 0, 0);
	private static final Color colorGood = new Color(81, 143, 162);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (tableCellRendererComponent instanceof JComponent) {
			((JComponent) tableCellRendererComponent).setOpaque(false);
		}
		return tableCellRendererComponent;
	}

	@Override
	protected String getText(CurrentMaxPair item) {
		if (item instanceof VisualDotInfo) {
			return ((VisualDotInfo) item).getLabel();
		}
		return super.getText(item);
	}

	@Override
	protected Color getBarColor(double percent) {
		if (percent > 0.999d) {
			return colorExpired;
		}
		return colorGood;
	}
}
