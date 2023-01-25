package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;

public class ActionAndStatusRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final IconNameIdRenderer renderer = new IconNameIdRenderer();
	private final boolean iconOnly;
	private final boolean enableTooltips;
	private boolean showId;

	public ActionAndStatusRenderer() {
		this(false, true, false);
	}


	public ActionAndStatusRenderer(boolean iconOnly, boolean enableTooltips, boolean showId) {
		this.iconOnly = iconOnly;
		this.enableTooltips = enableTooltips;
		this.showId = showId;
		renderer.setIdAlpha(128);
	}

	public static ActionAndStatusRenderer full() {
		return new ActionAndStatusRenderer();
	}

	public static ActionAndStatusRenderer iconOnlyNoTip() {
		return new ActionAndStatusRenderer(true, false, false);
	}

	public static ActionAndStatusRenderer withIdByDefault() {
		return new ActionAndStatusRenderer(false, true, true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {


		@Nullable HasIconURL icon;
		final String tooltip;
		final String text;
		@Nullable String idText = null;
		if (value instanceof XivAbility ability) {
			text = ability.getName();
			icon = ActionLibrary.iconForId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
			idText = String.format("%X", ability.getId());
		}
		else if (value instanceof XivStatusEffect status) {
			text = status.getName();
			icon = StatusEffectLibrary.iconForId(status.getId(), 1);
			tooltip = String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId());
			idText = String.format("%X", status.getId());
		}
		else if (value instanceof URL url) {
			text = "";
			icon = new URLIcon(url);
			tooltip = "";
			idText = null;
		}
		else if (value instanceof HasAbility hasAbility) {
			XivAbility ability = hasAbility.getAbility();
			text = ability.getName();
			icon = ActionLibrary.iconForId(ability.getId());
			tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
			idText = String.format("%X", ability.getId());
		}
		else if (value instanceof HasStatusEffect hasStatus) {
			XivStatusEffect status = hasStatus.getBuff();
			// TODO: duration?
			long stacks = hasStatus.getStacks();
			String preAppText = "";
			if (value instanceof BuffApplied ba) {
				StatusAppliedEffect preAppInfo = ba.getPreAppInfo();
				if (preAppInfo != null) {
					preAppText = "\nValues from Pre-App: %s\nRaw: %X %X".formatted(preAppInfo.getPreAppFlagsFormatted(), preAppInfo.getFlags(), preAppInfo.getValue());
				}
			}
			StringBuilder sb = new StringBuilder(status.getName());
			if (stacks > 0) {
				sb.append(" (").append(stacks).append(')');
			}
			if (value instanceof HasDuration hd && !hd.isIndefinite()) {
				sb.append(" (").append(String.format("%.02f", hd.getInitialDuration().toMillis() / 1_000.0));
			}
			text = sb.toString();
			icon = StatusEffectLibrary.iconForId(status.getId(), stacks);
			if (icon == null && status.getId() != 0) {
				icon = StatusEffectLibrary.iconForId(760, 0);
			}

			tooltip = String.format("%s (0x%x, %s)%s", status.getName(), status.getId(), status.getId(), preAppText);
			idText = String.format("%X", status.getId());
		}
		else if (value instanceof NameIdPair pair) {
			return fallback.getTableCellRendererComponent(table, pair.getName(), isSelected, hasFocus, row, column);
		}
		// Ehhh, really not the place for this, but it can move later
		else if (value instanceof HasPrimaryValue hasPrimaryValue) {
			// Using call text since it is fixed rather than dynamic
			tooltip = text = hasPrimaryValue.getPrimaryValue();
			icon = null;
		}
		else if (value instanceof HasIconURL hiu) {
			icon = hiu;
			tooltip = text = "";
		}
		else {
			return fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
		}
		Component defaultLabel;
		defaultLabel = fallback.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

		if (iconOnly) {
			if (icon == null) {
				icon = StatusEffectLibrary.iconForId(760, 0);
			}

			return IconTextRenderer.getIconOnly(icon);
		}
//
//		Component component = IconTextRenderer.getComponent(icon, defaultLabel, iconOnly, false, bypassCache, extraComponent);
		Component component = renderer;
		renderer.reset();
		renderer.setMainText(text);
		renderer.setIcon(icon);
		renderer.setIdText(showId ? idText : null);
		renderer.formatFrom(defaultLabel);
		if (enableTooltips) {
			RenderUtils.setTooltip(component, tooltip);
		}
		else {
			RenderUtils.setTooltip(component, null);
		}
		return component;


	}

	public boolean isShowId() {
		return showId;
	}

	public void setShowId(boolean showId) {
		this.showId = showId;
	}
}
