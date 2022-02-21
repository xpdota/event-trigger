package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.URLIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;

public class IconUrlRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component defaultLabel = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof URL url) {
			ScaledImageComponent iconOnly = IconTextRenderer.getIconOnly(new URLIcon(url));
			if (iconOnly == null) {
				return defaultLabel;
			}
			iconOnly.setBackground(defaultLabel.getBackground());
			return iconOnly;
		}
		return defaultLabel;
	}
}
