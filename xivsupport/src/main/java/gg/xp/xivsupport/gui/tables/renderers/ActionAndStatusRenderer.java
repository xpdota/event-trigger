package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivdata.data.URLIcon;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivStatusEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;

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


		HasIconURL icon;
		final String tooltip;
		final String text;
		if (value instanceof XivAbility ability) {
			text = ability.getName();
			icon = ActionLibrary.iconForId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		}
		else if (value instanceof XivStatusEffect status) {
			text = status.getName();
			icon = StatusEffectLibrary.iconForId(status.getId(), 1);
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
		}
		else if (value instanceof URL url) {
			text = "";
			icon = new URLIcon(url);
			tooltip = "";
		}
		else if (value instanceof HasAbility hasAbility) {
			XivAbility ability = hasAbility.getAbility();
			text = ability.getName();
			icon = ActionLibrary.iconForId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		}
		else if (value instanceof HasStatusEffect hasStatus) {
			XivStatusEffect status = hasStatus.getBuff();
			// TODO: duration?
			long stacks = hasStatus.getStacks();
			if (stacks > 0) {
				text = String.format("%s (%s)", status.getName(), stacks);
			}
			else {
				text = String.format("%s", status.getName());
			}
			icon = StatusEffectLibrary.iconForId(status.getId(), stacks);
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
		}
		else if (value instanceof NameIdPair pair) {
			return fallback.getTableCellRendererComponent(table, pair.getName(), isSelected, hasFocus, row, column);
		}
		else {
			return fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
		}
		Component defaultLabel;
		defaultLabel = fallback.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

		if (icon == null && iconOnly) {
			icon = StatusEffectLibrary.iconForId(760, 0);
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
