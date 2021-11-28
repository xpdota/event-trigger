package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public abstract class ResourceBarRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof CurrentMaxPair) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			TableColumn col = table.getColumnModel().getColumn(column);
			int width = col.getWidth();
			CurrentMaxPair hp = (CurrentMaxPair) value;
			double percent;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			int effectiveMax;
			int effectiveCurrent;
			if (actualMax == 0 || (actualMax < actualCurrent) || (actualMax == 1)) {
				return baseLabel;
//				effectiveMax = 1;
//				effectiveCurrent = 0;
			}
			else {
				effectiveMax = (int) (actualMax);
				effectiveCurrent = (int) (actualCurrent);
			}
			percent = effectiveCurrent / (double) effectiveMax;

			JPanel outerPanel = new JPanel();
			outerPanel.setLayout(new OverlapLayout());
			JPanel panel1 = new JPanel();
			panel1.setBorder(new LineBorder(Color.DARK_GRAY, 1));
			panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

			JPanel leftPanel = new JPanel();
			JPanel rightPanel = new JPanel();

//			Color greenColor = new Color(95, 148, 95, 98);
			Color colorRaw = getBarColor(percent, hp);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			leftPanel.setBackground(actualColor);
			Color gray = baseLabel.getBackground();
			rightPanel.setBackground(new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), 128));

			panel1.add(leftPanel);
			if (percent < 1.0) {
				panel1.add(rightPanel);
			}

			leftPanel.setPreferredSize(new Dimension((int) (width * percent), 10));
			rightPanel.setPreferredSize(new Dimension((int) (width * (1 - percent)), 10));
			JLabel label = getLabel(hp, width);
			leftPanel.add(label);
			label.setForeground(baseLabel.getForeground());

			panel1.setBounds(0, 0, 40, 40);
			label.setBounds(0, 0, label.getPreferredSize().width, label.getPreferredSize().height);
			label.setHorizontalAlignment(0);
			label.setVerticalAlignment(0);
			outerPanel.add(panel1);
			outerPanel.add(label);
			return outerPanel;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	protected abstract Color getBarColor(double percent, CurrentMaxPair item);

	protected JLabel getLabel(CurrentMaxPair item, int width) {
		// Try to do long label, otherwise fall back to short label
		JLabel longLabel = new JLabel(String.format("%s / %s", item.getCurrent(), item.getMax()));
		if (longLabel.getPreferredSize().width <= width) {
			return longLabel;
		}
		return new JLabel(String.valueOf(item.getCurrent()));
	}
}
