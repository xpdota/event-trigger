package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

@ScanMe
public class NormalBuffsBarConfig extends BuffsBarConfig {
	private final BooleanSetting showFcBuffs;
	private final BooleanSetting showFoodBuff;

	public NormalBuffsBarConfig(PersistenceProvider pers) {
		super(pers, "custom-party-overlay.buffs.");
		showFcBuffs = new BooleanSetting(pers, settingKeyBase + "fc-buffs", false);
		showFoodBuff = new BooleanSetting(pers, settingKeyBase + "food-buffs", false);
	}

	public BooleanSetting getShowFcBuffs() {
		return showFcBuffs;
	}

	public BooleanSetting getShowFoodBuff() {
		return showFoodBuff;
	}
}
