package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class ResourceBarRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final EmptyRenderer empty = new EmptyRenderer();
	protected final ResourceBar bar = new ResourceBar();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof CurrentMaxPair hp) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			double percent;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			int effectiveMax;
			int effectiveCurrent;
			if (actualMax == 0 || (actualMax < actualCurrent) || (actualMax == 1)) {
				return emptyComponent(isSelected, table);
			}
			else {
				effectiveMax = (int) (actualMax);
				effectiveCurrent = (int) (actualCurrent);
			}
			percent = effectiveCurrent / (double) effectiveMax;


			Color barColor = getBarColor(percent, hp);
			bar.setColor1(barColor);
			Color originalBg = baseLabel.getBackground();
			bar.setColor3(new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 128));
			bar.setBorderColor(getBorderColor(percent, hp, originalBg));


			bar.setPercent1(percent);
			bar.setTextColor(baseLabel.getForeground());

			formatLabel(hp);

			return bar;
		}
		return emptyComponent(isSelected, table);
	}

	private Component emptyComponent(boolean isSelected, JTable table) {
		if (isSelected) {
			empty.setBackground(table.getSelectionBackground());
		}
		else {
			empty.setBackground(null);
		}
		return empty;
	}

	protected abstract Color getBarColor(double percent, CurrentMaxPair item);

	protected Color getBorderColor(double percent, CurrentMaxPair item, Color originalBg) {
		return originalBg;
	}

	protected void formatLabel(CurrentMaxPair item) {
		// Try to do long label, otherwise fall back to short label
		String longText = String.format("%s / %s", item.getCurrent(), item.getMax());
		bar.setTextOptions(longText, String.valueOf(item.getCurrent()));
	}
}
