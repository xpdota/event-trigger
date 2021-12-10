package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class AbilityEffectListRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final AbilityEffectRenderer renderer = new AbilityEffectRenderer(false);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof List) {
			List<?> coll = (List<?>) value;
			Component defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			JPanel panel = new JPanel();
			panel.setBackground(defaultLabel.getBackground());
			panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			for (int i = 0; i < coll.size(); i++) {
				Object obj = coll.get(i);
				AbilityEffectRenderer renderer;
				// The renderer will try to re-use the label. Normally this is safe, but not when there are multiple.
				if (i == 0) {
					renderer = this.renderer;
				}
				else {
					renderer = new AbilityEffectRenderer(false);
				}
				Component component = renderer.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
				panel.add(component);
			}

			if (coll.size() > 1) {
				int foo = 5 + 2;
			}

			return panel;
		}
		return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
