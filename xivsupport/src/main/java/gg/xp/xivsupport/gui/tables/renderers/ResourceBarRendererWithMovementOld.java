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
public abstract class ResourceBarRendererWithMovementOld extends JPanel implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final JPanel leftPanel;
	private final JPanel midPanel;
	private final JPanel rightPanel;
	private final JLabel label;

	protected ResourceBarRendererWithMovementOld() {
		this.setLayout(new OverlapLayout());
		JPanel panel1 = new JPanel();
		panel1.setBorder(new LineBorder(Color.DARK_GRAY, 1));
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

		leftPanel = new JPanel();
		midPanel = new JPanel();
		rightPanel = new JPanel();
		panel1.add(leftPanel);
		panel1.add(midPanel);
		panel1.add(rightPanel);
		panel1.setBounds(0, 0, 40, 40);
		this.add(panel1);
		this.label = new JLabel("Text");
		this.add(label);

	}

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


			Color colorRaw = getBarColor(percent, percentChange, hp);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			leftPanel.setBackground(actualColor);
			Color gray = baseLabel.getBackground();
			rightPanel.setBackground(new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), 128));

			if (percentChange == 0) {
				// No predicted change - logic is easy:
				// | Current | Max - Current |
				leftPanel.setPreferredSize(new Dimension((int) (width * percent), 10));
				midPanel.setPreferredSize(new Dimension(0, 10));
				rightPanel.setPreferredSize(new Dimension((int) (width * (1 - percent)), 10));
			}
			else {
				// Predicted higher than current
				// | Current | ΔPredicted | Max - (Current + Predicted) |
				Color predictedColorRaw = getMovementBarColor(percent, percentChange, hp);
				Color actualPredictedColor = new Color(predictedColorRaw.getRed(), predictedColorRaw.getGreen(), predictedColorRaw.getBlue(), 98);
				midPanel.setBackground(actualPredictedColor);
				if (percentChange > 0) {
					if (percent + percentChange > 1) {
						// Cap current + predicted to 100% since you can't overheal
						percentChange = 1 - percent;
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

					leftPanel.setPreferredSize(new Dimension((int) (width * (percent + percentChange)), 10));
					midPanel.setPreferredSize(new Dimension((int) (width * -1 * percentChange), 10));
					rightPanel.setPreferredSize(new Dimension((int) (width * (1 - (percent))), 10));
				}

			}

			label.setForeground(baseLabel.getForeground());
			formatLabel(label, hp, width);

			label.setBounds(0, 0, label.getPreferredSize().width, label.getPreferredSize().height);
			label.setHorizontalAlignment(0);
			label.setVerticalAlignment(0);

			validate();

			return this;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	protected abstract Color getBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected abstract Color getMovementBarColor(double percent, double percentChange, CurrentMaxPredicted item);

	protected void formatLabel(JLabel label, CurrentMaxPredicted item, int width) {
		// Try to do long label, otherwise fall back to short label
		String text = String.format("%s / %s", item.getPredicted(), item.getMax());
		label.setText(text);
		if (label.getPreferredSize().width > width) {
			label.setText(String.valueOf(item.getPredicted()));
		}
	}
}
