package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.HitPoints;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public class HpRenderer implements TableCellRenderer {


	TableCellRenderer fallback = new DefaultTableCellRenderer();
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof HitPoints) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			TableColumn col = table.getColumnModel().getColumn(column);
			int width = col.getWidth();
			HitPoints hp = (HitPoints) value;
			double hpPercent;
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
			hpPercent = effectiveCurrent / (double) effectiveMax;

			JPanel outerPanel = new JPanel();
			outerPanel.setLayout(new OverlapLayout());
			JPanel panel1 = new JPanel();
			panel1.setBorder(new LineBorder(Color.DARK_GRAY, 1));
			panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

			JPanel leftPanel = new JPanel();
			JPanel rightPanel = new JPanel();

//			Color greenColor = new Color(95, 148, 95, 98);
			Color colorRaw = Color.getHSBColor((float) (0.33f * hpPercent), 0.36f, 0.58f);
			Color actualColor = new Color(colorRaw.getRed(), colorRaw.getGreen(), colorRaw.getBlue(), 98);
			leftPanel.setBackground(actualColor);
			Color gray = baseLabel.getBackground();
			rightPanel.setBackground(new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), 128));

			panel1.add(leftPanel);
			panel1.add(rightPanel);

			leftPanel.setPreferredSize(new Dimension((int) (width * hpPercent), 10));
			rightPanel.setPreferredSize(new Dimension((int) (width * (1 - hpPercent)), 10));
			JLabel label = new JLabel(String.format("%s / %s", actualCurrent, actualMax));
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

}
