package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class NameJobRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof XivPlayerCharacter) {
			Component defaultLabel = fallback.getTableCellRendererComponent(table, ((XivPlayerCharacter) value).getName(), isSelected, hasFocus, row, column);
			Job job = ((XivPlayerCharacter) value).getJob();
			if (job != null) {
				return IconTextRenderer.getComponent(job, defaultLabel);
			}
			return defaultLabel;
		}
		if (value instanceof XivEntity) {
			return fallback.getTableCellRendererComponent(table, ((XivEntity) value).getName(), isSelected, hasFocus, row, column);
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
