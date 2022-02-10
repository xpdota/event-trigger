package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;

public class IconUrlRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof URL url) {
			return IconTextRenderer.getIconOnly(() -> url);
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
