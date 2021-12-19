package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class NameJobRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	private final ComponentListRenderer listRenderer = new ComponentListRenderer(1);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof XivPlayerCharacter) {
			XivPlayerCharacter player = (XivPlayerCharacter) value;
			Component defaultLabel = fallback.getTableCellRendererComponent(table, player.getName(), isSelected, false, row, column);
			Job job = player.getJob();
			if (job != null) {
				Component icon = IconTextRenderer.getIconOnly(job);
				if (icon != null) {
					listRenderer.setComponents(List.of(icon, defaultLabel));
					listRenderer.setToolTipText(String.format("%s - %s (0x%x, %s)", player.getName(), player.getJob(), player.getId(), player.getId()));
					if (isSelected) {
						listRenderer.setBackground(table.getSelectionBackground());
					}
					else {
						listRenderer.setBackground(null);
					}
					return listRenderer;
				}
			}
			RenderUtils.setTooltip(defaultLabel, String.format("%s (0x%x, %s)", player.getName(), player.getId(), player.getId()));
			return defaultLabel;
		}
		if (value instanceof XivEntity) {
			Component component = fallback.getTableCellRendererComponent(table, ((XivEntity) value).getName(), isSelected, hasFocus, row, column);
			RenderUtils.setTooltip(component, String.format("%s (0x%x, %s)", ((XivEntity) value).getName(), ((XivEntity) value).getId(), ((XivEntity) value).getId()));
			return component;
		}
		Component defaultLabel = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		RenderUtils.setTooltip(defaultLabel, null);
		return defaultLabel;
	}
}
