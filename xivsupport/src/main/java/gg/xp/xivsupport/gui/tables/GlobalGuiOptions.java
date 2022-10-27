package gg.xp.xivsupport.gui.tables;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

@ScanMe
public class GlobalGuiOptions {

	private final BooleanSetting displayIds;

	public GlobalGuiOptions(PersistenceProvider pers) {
		displayIds = new BooleanSetting(pers, "global-gui-settings.display-ids", false);
	}

	public BooleanSetting displayIds() {
		return displayIds;
	}
}
