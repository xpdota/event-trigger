package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.HasIconURL;
import gg.xp.xivdata.jobs.StatusEffectIcon;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
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
	private final boolean enableTooltips;

	public ActionAndStatusRenderer() {
		this(false, false, true);
	}

	public ActionAndStatusRenderer(boolean iconOnly, boolean bypassCache, boolean enableTooltips) {
		this.iconOnly = iconOnly;
		this.bypassCache = bypassCache;
		this.enableTooltips = enableTooltips;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		Component defaultLabel;
		if (value instanceof NameIdPair && !iconOnly) {
			defaultLabel = fallback.getTableCellRendererComponent(table, ((NameIdPair) value).getName(), isSelected, hasFocus, row, column);
		}
		else {
			defaultLabel = fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
		}

		HasIconURL icon;
		String tooltip;
		if (value instanceof XivAbility ability) {
			icon = ActionIcon.forId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		}
		else if (value instanceof XivStatusEffect status) {
			icon = StatusEffectIcon.forId(status.getId(), 1);
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
		}
		else if (value instanceof HasAbility hasAbility) {
			XivAbility ability = hasAbility.getAbility();
			icon = ActionIcon.forId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		}
		else if (value instanceof HasStatusEffect hasStatus) {
			XivStatusEffect status = hasStatus.getBuff();
			long stacks = hasStatus.getStacks();
			icon = StatusEffectIcon.forId(status.getId(), stacks);
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
		}
		else {
			return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		if (icon == null) {
			return defaultLabel;
		}

		Component component = IconTextRenderer.getComponent(icon, defaultLabel, iconOnly, false, bypassCache);
		if (enableTooltips) {
			RenderUtils.setTooltip(component, tooltip);
		}
		else {
			RenderUtils.setTooltip(component, null);
		}
		return component;


	}
}
