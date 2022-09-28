package gg.xp.xivsupport.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

@ScanMe
public class WindowConfig {

	private final BooleanSetting startMinimized;

	public WindowConfig(PersistenceProvider pers) {
		startMinimized = new BooleanSetting(pers, "window-config.start-minimized", false);
	}

	public BooleanSetting getStartMinimized() {
		return startMinimized;
	}
}
