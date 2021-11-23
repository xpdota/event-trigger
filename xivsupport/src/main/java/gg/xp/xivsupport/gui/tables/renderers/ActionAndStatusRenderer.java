package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.HasIconURL;
import gg.xp.xivdata.jobs.StatusEffectIcon;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivStatusEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ActionAndStatusRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final boolean iconOnly;

	public ActionAndStatusRenderer() {
		this(false);
	}

	public ActionAndStatusRenderer(boolean iconOnly) {
		this.iconOnly = iconOnly;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		Component defaultLabel;
		if (value instanceof NameIdPair) {
			if (iconOnly) {
				defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			}
			else {
				defaultLabel = fallback.getTableCellRendererComponent(table, ((NameIdPair) value).getName(), isSelected, hasFocus, row, column);
			}
		}
		else {
			return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		HasIconURL icon;
		if (value instanceof XivAbility) {
			icon = ActionIcon.forId(((XivAbility) value).getId());
		}
		else if (value instanceof XivStatusEffect) {
			icon = StatusEffectIcon.forId(((XivStatusEffect) value).getId());
		}
		else {
			// Would never actually happen
			return defaultLabel;
		}
		if (icon == null) {
			return defaultLabel;
		}
		return IconTextRenderer.getComponent(icon, defaultLabel, iconOnly);


	}
}
