package gg.xp.xivsupport.custompartyoverlay.hpbar;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.tables.renderers.BarFractionDisplayOption;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.util.List;

@ScanMe
public class HpBarComponentConfig extends ObservableSetting {

	private final ColorSetting backgroundColor;
	private final ColorSetting shieldColor;
	private final ColorSetting hpGradientEmpty;
	private final ColorSetting hpGradientFull;
	private final ColorSetting fullHpColor;
	private final ColorSetting textColor;

	private final IntSetting fgTransparency;
	private final IntSetting bgTransparency;

	private final EnumSetting<BarFractionDisplayOption> fractionDisplayMode;

	public HpBarComponentConfig(PersistenceProvider pers) {
		String settingKeyBase = "custom-party-overlay.hp.";
		backgroundColor = new ColorSetting(pers, settingKeyBase + "bg", HpBar.defaultBgColor);
		shieldColor = new ColorSetting(pers, settingKeyBase + "shield", HpBar.defaultShieldColor);
		hpGradientEmpty = new ColorSetting(pers, settingKeyBase + "gradempty", HpBar.defaultEmptyGradientColor);
		hpGradientFull = new ColorSetting(pers, settingKeyBase + "gradfull", HpBar.defaultFullGradientColor);
		fullHpColor = new ColorSetting(pers, settingKeyBase + "full", HpBar.defaultFullHpColor);
		textColor = new ColorSetting(pers, settingKeyBase + "text", HpBar.defaultTextColor);
		fgTransparency = new IntSetting(pers, settingKeyBase + "fgtrans", 255, 0, 255);
		bgTransparency = new IntSetting(pers, settingKeyBase + "bgtrans", 128, 0, 255);
		fractionDisplayMode = new EnumSetting<>(pers, settingKeyBase + "fractionmode", BarFractionDisplayOption.class, BarFractionDisplayOption.AUTO);
		List.of(backgroundColor, shieldColor, hpGradientEmpty, hpGradientFull, fullHpColor, textColor, fgTransparency, bgTransparency, fractionDisplayMode)
				.forEach(setting -> setting.addListener(this::notifyListeners));
	}

	public ColorSetting getBackgroundColor() {
		return backgroundColor;
	}

	public ColorSetting getShieldColor() {
		return shieldColor;
	}

	public ColorSetting getHpGradientEmpty() {
		return hpGradientEmpty;
	}

	public ColorSetting getHpGradientFull() {
		return hpGradientFull;
	}

	public ColorSetting getFullHpColor() {
		return fullHpColor;
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
