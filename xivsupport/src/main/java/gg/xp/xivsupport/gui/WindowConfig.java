package gg.xp.xivsupport.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.themes.BuiltinTheme;
import gg.xp.xivsupport.persistence.EarlyPropsProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;

@ScanMe
public class WindowConfig {

	private static final EnumSetting<BuiltinTheme> theme;
	private final BooleanSetting startMinimized;
	private final BooleanSetting minimizeToTray;

	static {
		PersistenceProvider earlyProps = EarlyPropsProvider.getProvider();
		theme = new EnumSetting<>(earlyProps, "window-config.theme", BuiltinTheme.class, BuiltinTheme.DARK);
	}

	public WindowConfig(PersistenceProvider pers) {
		startMinimized = new BooleanSetting(pers, "window-config.start-minimized", false);
		minimizeToTray = new BooleanSetting(pers, "window-config.minimize-to-tray", false);
	}

	public BooleanSetting getStartMinimized() {
		return startMinimized;
	}

	public BooleanSetting getMinimizeToTray() {
		return minimizeToTray;
	}

	public static EnumSetting<BuiltinTheme> getThemeSettingStatic() {
		return theme;
	}

	public EnumSetting<BuiltinTheme> getThemeSetting() {
		return theme;
	}
}
