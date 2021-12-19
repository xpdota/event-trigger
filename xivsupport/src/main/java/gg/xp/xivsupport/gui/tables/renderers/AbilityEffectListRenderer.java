package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AbilityEffectListRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final AbilityEffectRenderer itemRenderer = new AbilityEffectRenderer(false);
	private final ComponentListRenderer listRenderer = new ComponentListRenderer(2);

	// TODO: test with multiple hits like the BLU spell
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof List) {
			List<Component> allComponents = new ArrayList<>();
			List<?> coll = (List<?>) value;
//			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			if (isSelected) {
				listRenderer.setBackground(table.getSelectionBackground());
			}
			else {
				listRenderer.setBackground(null);
			}
//			this.setBackground(defaultLabel.getBackground());
			StringBuilder tooltipBuilder = new StringBuilder();
			int count = coll.size();
			for (int i = 0; i < count; i++) {
				Object obj = coll.get(i);
				AbilityEffectRenderer renderer;
				// The renderer will try to re-use the label. Normally this is safe, but not when there are multiple.
				if (i == 0) {
					renderer = this.itemRenderer;
				}
				else {
					renderer = new AbilityEffectRenderer(false);
				}
				List<Component> components = renderer.getTableCellRendererComponents(table, obj, isSelected, false, row, column);
				allComponents.addAll(components);
				if (obj instanceof AbilityEffect) {
					tooltipBuilder.append(((AbilityEffect) obj).getDescription());
					tooltipBuilder.append('\n');
				}
				if (i < (count - 1)) {
					components.add(Box.createHorizontalStrut(3));
				}
			}

			listRenderer.setComponents(allComponents);

			String tooltip = tooltipBuilder.toString().stripTrailing();
			if (!tooltip.isEmpty()) {
				listRenderer.setToolTipText(tooltip);
			}
			else {
				listRenderer.setToolTipText(null);
			}

			return listRenderer;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
