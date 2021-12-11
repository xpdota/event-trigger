package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.models.XivStatusEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StatusEffectsRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final ActionAndStatusRenderer renderer = new ActionAndStatusRenderer(true, false, false);
	private final ActionAndStatusRenderer rendererNoCache = new ActionAndStatusRenderer(true, true, false);
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof Collection) {
			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			Collection<?> coll = (Collection<?>) value;
			if (coll.isEmpty()) {
				return defaultLabel;
			}
			JPanel panel = new JPanel();
			panel.setBackground(defaultLabel.getBackground());
			panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			StringBuilder tooltipBuilder = new StringBuilder();
			Set<Component> seen = new HashSet<>(coll.size());
			coll.forEach(obj -> {
				Component component = (renderer.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column));
				boolean alreadyExists = !seen.add(component);
				if (alreadyExists) {
					// TODO: this is messy
					// Better way would be to make a special JPanel subclass that allows you to add components lazily,
					// and then paint them manually when rendered.
					component = rendererNoCache.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);;
				}
				panel.add(component);
				if (obj instanceof XivStatusEffect) {
					XivStatusEffect status = (XivStatusEffect) obj;
					tooltipBuilder.append(status.getName());
					long id = status.getId();
					tooltipBuilder.append(" (0x").append(Long.toString(id, 16))
							.append(", ").append(id).append(")\n\n");
				}
			});
			panel.setToolTipText(tooltipBuilder.toString().stripTrailing());
			return panel;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
