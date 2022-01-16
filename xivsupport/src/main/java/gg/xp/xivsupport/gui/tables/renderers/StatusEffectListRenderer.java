package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.models.XivStatusEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StatusEffectListRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final ActionAndStatusRenderer renderer = new ActionAndStatusRenderer(true, false, false);
	private final ComponentListRenderer listRenderer = new ComponentListRenderer(0);
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof Collection<?> coll) {
			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			if (isSelected) {
				listRenderer.setBackground(defaultLabel.getBackground());
			}
			else {
				listRenderer.setBackground(null);
			}
			List<Component> comps = new ArrayList<>();
			if (!((Collection<?>) value).isEmpty()) {
				StringBuilder tooltipBuilder = new StringBuilder();
				coll.forEach(obj -> {
					Component component = (renderer.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column));
					comps.add(component);
					if (obj instanceof HasStatusEffect status) {
						tooltipBuilder.append(status.getBuff().getName());
						long id = status.getBuff().getId();
						tooltipBuilder.append(" (0x").append(Long.toString(id, 16))
								.append(", ").append(id).append(", Stacks: ").append(status.getStacks()).append(")\n\n");
					}
					else if (obj instanceof XivStatusEffect status) {
						tooltipBuilder.append(status.getName());
						long id = status.getId();
						tooltipBuilder.append(" (0x").append(Long.toString(id, 16))
								.append(", ").append(id).append(")\n\n");
					}
				});
				listRenderer.setToolTipText(tooltipBuilder.toString().stripTrailing());
			}
			listRenderer.setComponents(comps);
			return listRenderer;
		}
		listRenderer.reset();
		return listRenderer;
	}
}
