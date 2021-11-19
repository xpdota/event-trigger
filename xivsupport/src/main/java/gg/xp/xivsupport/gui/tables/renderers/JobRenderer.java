package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.Job;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

public class JobRenderer implements TableCellRenderer, ListCellRenderer<Job> {
	TableCellRenderer fallback = new DefaultTableCellRenderer();
	ListCellRenderer<Object> flr = new DefaultListCellRenderer();

	private final Map<Job, Image> cache = new EnumMap<>(Job.class);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component defaultLabel = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof Job) {
			return getComponent((Job) value, defaultLabel);
		}
		return defaultLabel;
	}

	private Component getComponent(Job value, Component defaultLabel) {

		Image scaled = cache.computeIfAbsent(value, job -> {
			URL imageUrl = value.getIcon();
			if (imageUrl == null) {
				return null;
			}
			ImageIcon icon = new ImageIcon(imageUrl);
			return icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
		});
		if (scaled == null) {
			return defaultLabel;
		}
		ImageIcon scaledIcon = new ImageIcon(scaled);
		JLabel label = new JLabel(scaledIcon);
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.setOpaque(true);
		panel.setBackground(defaultLabel.getBackground());
		panel.add(label);
		panel.add(defaultLabel);
		return panel;

	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Job> list, Job value, int index, boolean isSelected, boolean cellHasFocus) {
		Component defaultLabel = flr.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		return getComponent(value, defaultLabel);
	}
}
