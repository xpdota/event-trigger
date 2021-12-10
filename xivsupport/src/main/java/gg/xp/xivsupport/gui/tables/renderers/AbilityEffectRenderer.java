package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.HasIconURL;
import gg.xp.xivdata.jobs.StatusEffectIcon;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.MpGain;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class AbilityEffectRenderer implements TableCellRenderer {
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final boolean iconOnly;

	public AbilityEffectRenderer() {
		this(false);
	}

	public AbilityEffectRenderer(boolean iconOnly) {
		this.iconOnly = iconOnly;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		Component defaultLabel;
		if (!(value instanceof AbilityEffect)) {
			return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		String text;
		HasIconURL icon;
		boolean textOnRight = false;
		if (value instanceof DamageEffect) {
			text = ((DamageEffect) value).getSeverity().getSymbol() + ((DamageEffect) value).getAmount();
			icon = ActionIcon.forId(9);
		}
		else if (value instanceof HealEffect) {
			text = ((HealEffect) value).getSeverity().getSymbol() + ((HealEffect) value).getAmount();
			icon = ActionIcon.forId(3594);
		}
		else if (value instanceof MpGain) {
			text = "+" + ((MpGain) value).getAmount();
			icon = ActionIcon.forId(7562);
		}
		else if (value instanceof StatusAppliedEffect) {
			text = "+";
			icon = StatusEffectIcon.forId(((StatusAppliedEffect) value).getStatus().getId());
			textOnRight = true;
		}
		else {
			return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		defaultLabel = fallback.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
		return IconTextRenderer.getComponent(icon, defaultLabel, iconOnly, textOnRight);


	}
}
