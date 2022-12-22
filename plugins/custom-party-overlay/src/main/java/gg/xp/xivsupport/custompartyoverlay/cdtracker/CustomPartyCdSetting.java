package gg.xp.xivsupport.custompartyoverlay.cdtracker;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import java.util.EnumSet;
import java.util.Set;

public class CustomPartyCdSetting {
	private final BooleanSetting enable;

	public CustomPartyCdSetting(PersistenceProvider persistence, String settingKeyBase, boolean enableByDefault) {
		this.enable = new BooleanSetting(persistence, settingKeyBase + ".in-custom-party-overlay", enableByDefault);
	}

	public BooleanSetting getEnable() {
		return enable;
	}
}
