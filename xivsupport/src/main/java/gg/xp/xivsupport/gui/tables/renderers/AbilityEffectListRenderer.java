package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class AbilityEffectListRenderer extends JPanel implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final AbilityEffectRenderer renderer = new AbilityEffectRenderer(false);

	public AbilityEffectListRenderer() {
//		this.setLayout(null);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
	}
	// TODO: test with multiple hits like the BLU spell
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof List) {
			this.removeAll();
			List<?> coll = (List<?>) value;
			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			this.setBackground(defaultLabel.getBackground());
			StringBuilder tooltipBuilder = new StringBuilder();
			int count = coll.size();
			for (int i = 0; i < count; i++) {
				Object obj = coll.get(i);
				AbilityEffectRenderer renderer;
				// The renderer will try to re-use the label. Normally this is safe, but not when there are multiple.
				if (i == 0) {
					renderer = this.renderer;
				}
				else {
					renderer = new AbilityEffectRenderer(false);
				}
				List<Component> components = renderer.getTableCellRendererComponents(table, obj, isSelected, hasFocus, row, column);
				components.forEach(this::add);
				if (obj instanceof AbilityEffect) {
					tooltipBuilder.append(((AbilityEffect) obj).getDescription());
					tooltipBuilder.append('\n');
				}
				if (i < (count - 1)) {
					this.add(Box.createHorizontalStrut(3));
				}
			}

			if (count > 1) {
				int foo = 5 + 2;
			}
			String tooltip = tooltipBuilder.toString().stripTrailing();
			if (!tooltip.isEmpty()) {
				this.setToolTipText(tooltip);
			}

			return this;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
