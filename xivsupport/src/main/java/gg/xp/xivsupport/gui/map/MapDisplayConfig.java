package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.map.omen.OmenDisplayMode;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;

@ScanMe
public class MapDisplayConfig {
	private final EnumSetting<NameDisplayMode> nameDisplayMode;
	private final EnumSetting<OmenDisplayMode> omenDisplayMode;
	private final BooleanSetting castBars;
	private final BooleanSetting hpBars;
	private final BooleanSetting ids;

	public MapDisplayConfig(PersistenceProvider pers) {
		String settingBase = "map-display-config.";
		nameDisplayMode = new EnumSetting<>(pers, settingBase + "name-display-mode", NameDisplayMode.class, NameDisplayMode.FULL);
		omenDisplayMode = new EnumSetting<>(pers, settingBase + "omen-display-mode", OmenDisplayMode.class, OmenDisplayMode.SELECTED_ONLY);
		castBars = new BooleanSetting(pers, settingBase + "display-cast-bars", true);
		hpBars = new BooleanSetting(pers, settingBase + "display-hp-bars", true);
		ids = new BooleanSetting(pers, settingBase + "display-ids", true);
	}

	public EnumSetting<NameDisplayMode> getNameDisplayMode() {
		return nameDisplayMode;
	}

	public EnumSetting<OmenDisplayMode> getOmenDisplayMode() {
		return omenDisplayMode;
	}

	public BooleanSetting getCastBars() {
		return castBars;
	}

	public BooleanSetting getHpBars() {
		return hpBars;
	}

	public BooleanSetting getIds() {
		return ids;
	}
}
