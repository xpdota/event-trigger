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
//	private final ActionAndStatusRenderer renderer;
	private final IconNameIdRenderer renderer = new IconNameIdRenderer();

	@Override
	public Component getListCellRendererComponent(JList<? extends XivStatusEffect> list, XivStatusEffect status, int index, boolean isSelected, boolean cellHasFocus) {
		Component defaultComponent = dflt.getListCellRendererComponent(list, status, index, isSelected, cellHasFocus);
		renderer.reset();
		renderer.formatFrom(defaultComponent);
		renderer.setMainText(status.getName());
		renderer.setIcon(StatusEffectLibrary.iconForId(status.getId(), 1));
		renderer.setToolTipText(String.format("%s (0x%x, %s)", status.getName(), status.getId(), status.getId()));
		renderer.setIdText(String.format("%X", status.getId()));
		return renderer;
	}
}
