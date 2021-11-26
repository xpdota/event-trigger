package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Collection;

public class StatusEffectsRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final ActionAndStatusRenderer renderer = new ActionAndStatusRenderer(true);
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof Collection) {
			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			JPanel panel = new JPanel();
			panel.setBackground(defaultLabel.getBackground());
//			panel.setOpaque(false);
			panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			((Collection<?>) value).forEach(obj -> {
				panel.add(renderer.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column));
			});

			return panel;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
