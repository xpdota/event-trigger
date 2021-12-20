package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPredicted;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Bar that shows "movement", like that white are on a bar that shows how much HP you just lost.
 */
public abstract class ResourceBarRendererWithMovement implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	protected final ResourceBar bar = new ResourceBar();
	private final EmptyRenderer empty = new EmptyRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof CurrentMaxPredicted hp) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			double percent;
			double percentChange;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			long actualPredicted = hp.getPredicted();
			int effectiveMax;
			int effectiveCurrent;
			int effectivePredicted;
			if (actualMax == 0 || (actualMax < actualCurrent) || (actualMax == 1)) {
				return emptyComponent(isSelected, table);
			}
			else {
				effectiveMax = (int) (actualMax);
				effectiveCurrent = (int) (actualCurrent);
				effectivePredicted = (int) (actualPredicted);
			}
			percent = effectiveCurrent / (double) effectiveMax;
			percentChange = (effectivePredicted - effectiveCurrent) / (double) effectiveMax;


			Color barColor = getBarColor(percent, percentChange, hp);
			bar.setColor1(barColor);
			Color originalBg = baseLabel.getBackground();
			Color bg = new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 128);
			bar.setColor3(bg);
			bar.setBorderColor(getBorderColor(percent, percentChange, hp, originalBg));
			bar.setTextColor(baseLabel.getForeground());

			if (percentChange == 0) {
				// No predicted change - logic is easy:
				// | Current | Max - Current |
				bar.setPercent1(percent);
				bar.setPercent2(0);
			}
			else {
				// Predicted higher than current
				// | Current | ΔPredicted | Max - (Current + Predicted) |
				Color movementColor = getMovementBarColor(percent, percentChange, hp);
				bar.setColor2(movementColor);
				if (percentChange > 0) {
					if (percent + percentChange > 1) {
						// Cap current + predicted to 100% since you can't overheal
						percentChange = 1 - percent;
					}

					bar.setPercent1(percent);
					bar.setPercent2(percentChange);
				}
				else {
					// Predicted lower than current
					// | Current - ΔPredicted | ΔPredicted | Max - (Current) |
					if (percent + percentChange < 0) {
						// Don't let it go below zero
						percentChange = -1 * percent;
					}

					bar.setPercent1(percent + percentChange);
					bar.setPercent2(-1 * percentChange);
				}

			}

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

	protected abstract Color getBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected abstract Color getMovementBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected abstract Color getBorderColor(double percent, double percentChange, CurrentMaxPredicted item, Color originalBg);

	protected void formatLabel(CurrentMaxPredicted item) {
		// Try to do long label, otherwise fall back to short label
		String longText = String.format("%s / %s", item.getPredicted(), item.getMax());
		bar.setTextOptions(longText, String.valueOf(item.getPredicted()));
	}
}
