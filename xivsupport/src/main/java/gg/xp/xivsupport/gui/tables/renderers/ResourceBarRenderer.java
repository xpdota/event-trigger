package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public abstract class ResourceBarRenderer extends JPanel implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final JPanel leftPanel;
	private final JPanel rightPanel;
	private final JLabel label;

	protected ResourceBarRenderer() {
		this.setLayout(new OverlapLayout());
		JPanel panel1 = new JPanel();
		panel1.setBorder(new LineBorder(Color.DARK_GRAY, 1));
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

		leftPanel = new JPanel();
		rightPanel = new JPanel();
		panel1.add(leftPanel);
		panel1.add(rightPanel);
		panel1.setBounds(0, 0, 40, 40);
		// TODO
//		if (percent < 1.0) {
//			panel1.add(rightPanel);
//		}
		this.add(panel1);
		this.label = new JLabel("Text");
		this.add(label);
	}

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


//			Color greenColor = new Color(95, 148, 95, 98);
			Color colorRaw = getBarColor(percent, hp);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			leftPanel.setBackground(actualColor);
			Color gray = baseLabel.getBackground();
			rightPanel.setBackground(new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), 128));


			leftPanel.setPreferredSize(new Dimension((int) (width * percent), 10));
			rightPanel.setPreferredSize(new Dimension((int) (width * (1 - percent)), 10));
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

	protected abstract Color getBarColor(double percent, CurrentMaxPair item);

	protected void formatLabel(JLabel label, CurrentMaxPair item, int width) {
		// Try to do long label, otherwise fall back to short label
		String text = String.format("%s / %s", item.getCurrent(), item.getMax());
		label.setText(text);
		if (label.getPreferredSize().width > width) {
			label.setText(String.valueOf(item.getCurrent()));
		}
	}
}
