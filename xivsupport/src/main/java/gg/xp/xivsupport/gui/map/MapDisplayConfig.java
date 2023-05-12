package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.EnumSetting;

@ScanMe
public class MapDisplayConfig {
	private final EnumSetting<NameDisplayMode> nameDisplayMode;
	private final EnumSetting<OmenDisplayMode> omenDisplayMode;

	public MapDisplayConfig(PersistenceProvider pers) {
		String settingBase = "map-display-config.";
		nameDisplayMode = new EnumSetting<>(pers, settingBase + "name-display-mode", NameDisplayMode.class, NameDisplayMode.FULL);
		omenDisplayMode = new EnumSetting<>(pers, settingBase + "omen-display-mode", OmenDisplayMode.class, OmenDisplayMode.SELECTED_ONLY);
	}

	public EnumSetting<NameDisplayMode> getNameDisplayMode() {
		return nameDisplayMode;
	}

	public EnumSetting<OmenDisplayMode> getOmenDisplayMode() {
		return omenDisplayMode;
	}
}
