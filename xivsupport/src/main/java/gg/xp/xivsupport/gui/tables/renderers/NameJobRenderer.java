package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NameJobRenderer implements TableCellRenderer {
	private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();

	private final ComponentListRenderer listRenderer;
	private final boolean transparent;

	public NameJobRenderer() {
		this(false, false);
	}

	public NameJobRenderer(boolean transparent, boolean reversed) {
		this.transparent = transparent;
		listRenderer = new ComponentListRenderer(1, reversed);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected) {
			listRenderer.setBackground(table.getSelectionBackground());
		}
		else {
			listRenderer.setBackground(null);
		}
		final Component icon;
		final Component label;
		final String tooltip;
		if (value instanceof XivEntity entity) {
			label = fallback.getTableCellRendererComponent(table, entity.getName(), isSelected, false, row, column);
			Job job;
			if (value instanceof XivPlayerCharacter player && (job = player.getJob()) != null) {
				icon = IconTextRenderer.getIconOnly(job);
				tooltip = (String.format("%s - %s (0x%x, %s)", player.getName(), player.getJob(), player.getId(), player.getId()));
			}
			else {
				tooltip = String.format("%s (0x%x, %s)", entity.getName(), entity.getId(), entity.getId());
				icon = null;
			}
		}
		else if (value == null) {
			tooltip = null;
			label = null;
			icon = null;
		}
		else {
			tooltip = null;
			label = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			icon = null;
		}
		if (transparent && label instanceof JComponent jc) {
			jc.setOpaque(false);
		}
		List<Component> components = new ArrayList<>(2);
		if (icon != null) {
			components.add(icon);
		}
		if (label != null) {
			components.add(label);
		}
		listRenderer.setComponents(components);
		listRenderer.setToolTipText(tooltip);
		return listRenderer;
	}
}
