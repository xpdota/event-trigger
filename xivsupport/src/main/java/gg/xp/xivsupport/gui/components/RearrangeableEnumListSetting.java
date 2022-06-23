package gg.xp.xivsupport.gui.components;

import gg.xp.xivsupport.persistence.settings.EnumListSetting;

public class RearrangeableEnumListSetting<X extends Enum<X>> {

	private final RearrangeableList<X> listGui;

	public RearrangeableEnumListSetting(EnumListSetting<X> setting) {
		listGui = new RearrangeableList<>(setting.get(), setting::set);
		setting.addListener(() -> listGui.setValues(setting.get()));
	}

	public RearrangeableList<X> getListGui() {
		return listGui;
	}
}
