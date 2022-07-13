package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivStatusEffect;

import javax.swing.*;
import java.awt.*;

public class StatusListCellRenderer implements ListCellRenderer<XivStatusEffect> {

	private final DefaultListCellRenderer dflt = new DefaultListCellRenderer();

	@Override
	public Component getListCellRendererComponent(JList<? extends XivStatusEffect> list, XivStatusEffect ability, int index, boolean isSelected, boolean cellHasFocus) {
		String tooltip = String.format("%s (0x%x, %s)", ability.getName(), ability.getId(), ability.getId());
		HasIconURL icon = StatusEffectLibrary.iconForId(ability.getId(), 0);
		Component component = IconTextRenderer.getComponent(icon, dflt.getListCellRendererComponent(list, ability.getName(), index, isSelected, cellHasFocus), false);
		RenderUtils.setTooltip(component, tooltip);
		return component;
	}
}
