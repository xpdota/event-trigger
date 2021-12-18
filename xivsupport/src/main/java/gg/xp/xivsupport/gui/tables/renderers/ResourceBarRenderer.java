package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public abstract class ResourceBarRenderer extends JPanel implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	protected final ResourceBar bar = new ResourceBar();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof CurrentMaxPair) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			CurrentMaxPair hp = (CurrentMaxPair) value;
			double percent;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			int effectiveMax;
			int effectiveCurrent;
			if (actualMax == 0 || (actualMax < actualCurrent) || (actualMax == 1)) {
				return baseLabel;
			}
			else {
				effectiveMax = (int) (actualMax);
				effectiveCurrent = (int) (actualCurrent);
			}
			percent = effectiveCurrent / (double) effectiveMax;


			Color colorRaw = getBarColor(percent, hp);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			bar.setColor1(actualColor);
			Color originalBg = baseLabel.getBackground();
			bar.setColor3(new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 128));
			bar.setBorderColor(originalBg);


			bar.setPercent1(percent);
			bar.setTextColor(baseLabel.getForeground());

			formatLabel(hp);

			return bar;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	protected abstract Color getBarColor(double percent, CurrentMaxPair item);

	protected void formatLabel(CurrentMaxPair item) {
		// Try to do long label, otherwise fall back to short label
		String longText = String.format("%s / %s", item.getCurrent(), item.getMax());
		bar.setTextOptions(longText, String.valueOf(item.getCurrent()));
	}
}
