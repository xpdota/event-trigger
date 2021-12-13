package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPredicted;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Bar that shows "movement", like that white are on a bar that shows how much HP you just lost.
 */
public abstract class ResourceBarRendererWithMovement implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof CurrentMaxPredicted) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			TableColumn col = table.getColumnModel().getColumn(column);
			int width = col.getWidth();
			CurrentMaxPredicted hp = (CurrentMaxPredicted) value;
			double percent;
			double percentChange;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			long actualPredicted = hp.getPredicted();
			int effectiveMax;
			int effectiveCurrent;
			int effectivePredicted;
			if (actualMax == 0 || (actualMax < actualCurrent) || (actualMax == 1)) {
				return baseLabel;
			}
			else {
				effectiveMax = (int) (actualMax);
				effectiveCurrent = (int) (actualCurrent);
				effectivePredicted = (int) (actualPredicted);
			}
			percent = effectiveCurrent / (double) effectiveMax;
			percentChange = (effectivePredicted - effectiveCurrent) / (double) effectiveMax;

			JPanel outerPanel = new JPanel();
			outerPanel.setLayout(new OverlapLayout());
			JPanel panel1 = new JPanel();
			panel1.setBorder(new LineBorder(Color.DARK_GRAY, 1));
			panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

			JPanel leftPanel = new JPanel();
			JPanel midPanel;
			JPanel rightPanel = new JPanel();

			Color colorRaw = getBarColor(percent, percentChange, hp);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			leftPanel.setBackground(actualColor);
			Color gray = baseLabel.getBackground();
			rightPanel.setBackground(new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), 128));
			JLabel label = getLabel(hp, width);
			leftPanel.add(label);
			label.setForeground(baseLabel.getForeground());
			panel1.setBounds(0, 0, 40, 40);
			panel1.add(leftPanel);

			if (percentChange == 0) {
				// No predicted change - logic is easy:
				// | Current | Max - Current |

				if (percent < 1.0) {
					panel1.add(rightPanel);
				}

				leftPanel.setPreferredSize(new Dimension((int) (width * percent), 10));
				rightPanel.setPreferredSize(new Dimension((int) (width * (1 - percent)), 10));
			}
			else {
				// Predicted higher than current
				// | Current | ΔPredicted | Max - (Current + Predicted) |
				Color predictedColorRaw = getMovementBarColor(percent, percentChange, hp);
				Color actualPredictedColor = new Color(predictedColorRaw.getRed(), predictedColorRaw.getGreen(), predictedColorRaw.getBlue(), 98);
				midPanel = new JPanel();
				midPanel.setBackground(actualPredictedColor);
				if (percentChange > 0) {
					if (percent + percentChange > 1) {
						// Cap current + predicted to 100% since you can't overheal
						percentChange = 1 - percent;
					}

					panel1.add(midPanel);
					if ((percent + percentChange) < 1.0) {
						panel1.add(rightPanel);
					}

					leftPanel.setPreferredSize(new Dimension((int) (width * percent), 10));
					midPanel.setPreferredSize(new Dimension((int) (width * percentChange), 10));
					rightPanel.setPreferredSize(new Dimension((int) (width * (1 - (percent + percentChange))), 10));
				}
				else {
					// Predicted lower than current
					// | Current - ΔPredicted | ΔPredicted | Max - (Current) |
					if (percent + percentChange < 0) {
						// Don't let it go below zero
						percentChange = -1 * percent;
					}

					panel1.add(midPanel);
					if ((percent) < 1.0) {
						panel1.add(rightPanel);
					}

					leftPanel.setPreferredSize(new Dimension((int) (width * (percent + percentChange)), 10));
					midPanel.setPreferredSize(new Dimension((int) (width * -1 * percentChange), 10));
					rightPanel.setPreferredSize(new Dimension((int) (width * (1 - (percent))), 10));
				}

			}
			label.setBounds(0, 0, label.getPreferredSize().width, label.getPreferredSize().height);
			label.setHorizontalAlignment(0);
			label.setVerticalAlignment(0);
			outerPanel.add(panel1);
			outerPanel.add(label);
			return outerPanel;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	protected abstract Color getBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected abstract Color getMovementBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected JLabel getLabel(CurrentMaxPredicted item, int width) {
		// Try to do long label, otherwise fall back to short label
		JLabel longLabel = new JLabel(String.format("%s / %s", item.getPredicted(), item.getMax()));
		if (longLabel.getPreferredSize().width <= width) {
			return longLabel;
		}
		return new JLabel(String.valueOf(item.getCurrent()));
	}
}
