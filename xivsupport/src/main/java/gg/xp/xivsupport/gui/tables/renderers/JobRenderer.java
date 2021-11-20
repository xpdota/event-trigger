package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.Job;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class JobRenderer implements TableCellRenderer, ListCellRenderer<Job> {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final ListCellRenderer<Object> flr = new DefaultListCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component defaultLabel = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof Job) {
			return IconTextRenderer.getComponent((Job) value, defaultLabel);
		}
		return defaultLabel;
	}


	@Override
	public Component getListCellRendererComponent(JList<? extends Job> list, Job value, int index, boolean isSelected, boolean cellHasFocus) {
		Component defaultLabel = flr.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		return IconTextRenderer.getComponent(value, defaultLabel);
	}
}
