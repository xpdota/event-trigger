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
	private final boolean bypassCache;

	public ActionAndStatusRenderer() {
		this(false, false, true);
	}

	public ActionAndStatusRenderer(boolean iconOnly, boolean bypassCache, boolean enableTooltips) {
		this.iconOnly = iconOnly;
		this.bypassCache = bypassCache;
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
		String tooltip;
		if (value instanceof XivAbility) {
			XivAbility ability = ((XivAbility) value);
			icon = ActionIcon.forId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		}
		else if (value instanceof XivStatusEffect) {
			XivStatusEffect status = (XivStatusEffect) value;
			icon = StatusEffectIcon.forId(status.getId());
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
		}
		else {
			// Would never actually happen
			return defaultLabel;
		}
		if (icon == null) {
			return defaultLabel;
		}
		Component component = IconTextRenderer.getComponent(icon, defaultLabel, iconOnly, false, bypassCache);
		RenderUtils.setTooltip(component, tooltip);
		return component;


	}
}
