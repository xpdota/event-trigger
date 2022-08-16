package gg.xp.xivsupport.custompartyoverlay.mpbar;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.tables.renderers.BarFractionDisplayOption;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.gui.tables.renderers.MpBar;
import gg.xp.xivsupport.gui.tables.renderers.MpBar;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.util.List;

@ScanMe
public class MpBarComponentConfig extends ObservableSetting {

	private final ColorSetting backgroundColor;
	private final ColorSetting mpColor;
	private final ColorSetting textColor;

	private final IntSetting fgTransparency;
	private final IntSetting bgTransparency;

	private final EnumSetting<BarFractionDisplayOption> fractionDisplayMode;

	public MpBarComponentConfig(PersistenceProvider pers) {
		String settingKeyBase = "custom-party-overlay.mp.";
		backgroundColor = new ColorSetting(pers, settingKeyBase + "bg", MpBar.defaultBgColor);
		mpColor = new ColorSetting(pers, settingKeyBase + "bar", MpBar.defaultMpColor);
		textColor = new ColorSetting(pers, settingKeyBase + "text", HpBar.defaultTextColor);
		fgTransparency = new IntSetting(pers, settingKeyBase + "fgtrans", 255, 0, 255);
		bgTransparency = new IntSetting(pers, settingKeyBase + "bgtrans", 100, 0, 255);
		fractionDisplayMode = new EnumSetting<>(pers, settingKeyBase + "fractionmode", BarFractionDisplayOption.class, BarFractionDisplayOption.AUTO);
		List.of(backgroundColor, mpColor, textColor, fgTransparency, bgTransparency, fractionDisplayMode)
				.forEach(setting -> setting.addListener(this::notifyListeners));
	}

	public ColorSetting getBackgroundColor() {
		return backgroundColor;
	}

	public ColorSetting getMpColor() {
		return mpColor;
	}

	public ColorSetting getTextColor() {
		return textColor;
	}

	public IntSetting getFgTransparency() {
		return fgTransparency;
	}

	public IntSetting getBgTransparency() {
		return bgTransparency;
	}

	public EnumSetting<BarFractionDisplayOption> getFractionDisplayMode() {
		return fractionDisplayMode;
	}
}
