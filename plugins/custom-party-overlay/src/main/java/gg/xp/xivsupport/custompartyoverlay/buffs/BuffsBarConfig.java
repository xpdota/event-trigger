package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.util.List;

@ScanMe
public class BuffsBarConfig extends ObservableSetting {

	private final BooleanSetting timers;

	private final ColorSetting normalTextColor;
	private final ColorSetting myBuffTextColor;
	private final ColorSetting removeableBuffColor;

	private final BooleanSetting shadows;

	private final IntSetting xPadding;

	private final BooleanSetting showFcBuffs;
	private final BooleanSetting showFoodBuff;

	public BuffsBarConfig(PersistenceProvider pers) {
		String settingKeyBase = "custom-party-overlay.buffs.";
		normalTextColor = new ColorSetting(pers, settingKeyBase + "normal-color", BuffsBar.defaultTextColor);
		myBuffTextColor = new ColorSetting(pers, settingKeyBase + "mybuff-color", BuffsBar.defaultMyBuffColor);
		removeableBuffColor = new ColorSetting(pers, settingKeyBase + "removeablebuff-color", BuffsBar.defaultRemovableBuffColor);
		timers = new BooleanSetting(pers, settingKeyBase + "show-timers", true);
		shadows = new BooleanSetting(pers, settingKeyBase + "text-shadows", true);
		xPadding = new IntSetting(pers, settingKeyBase + "xpad", 0, -20, 1000);
		showFcBuffs = new BooleanSetting(pers, settingKeyBase + "fc-buffs", false);
		showFoodBuff = new BooleanSetting(pers, settingKeyBase + "food-buffs", false);
		List.of(normalTextColor, myBuffTextColor, removeableBuffColor, timers, shadows, xPadding, showFcBuffs, showFoodBuff)
				.forEach(setting -> setting.addListener(this::notifyListeners));
	}

	public BooleanSetting getTimers() {
		return timers;
	}

	public ColorSetting getNormalTextColor() {
		return normalTextColor;
	}

	public ColorSetting getMyBuffTextColor() {
		return myBuffTextColor;
	}

	public ColorSetting getRemoveableBuffColor() {
		return removeableBuffColor;
	}

	public BooleanSetting getShadows() {
		return shadows;
	}

	public IntSetting getxPadding() {
		return xPadding;
	}

	public BooleanSetting getShowFcBuffs() {
		return showFcBuffs;
	}

	public BooleanSetting getShowFoodBuff() {
		return showFoodBuff;
	}
}
