package gg.xp.xivsupport.custompartyoverlay.name;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.overlay.TextAlignment;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.awt.*;

@ScanMe
public class NameComponentConfig extends ObservableSetting {

	private final EnumSetting<TextAlignment> alignment;
	private final BooleanSetting dropShadow;
	private final ColorSetting fontColor;

	public NameComponentConfig(PersistenceProvider pers) {
		alignment = new EnumSetting<>(pers, "custom-party-overlay.name.alignment", TextAlignment.class, TextAlignment.RIGHT);
		dropShadow = new BooleanSetting(pers, "custom-party-overlay.name.drop-shadow", true);
		fontColor = new ColorSetting(pers, "custom-party-overlay.name.font-color", Color.WHITE);
		alignment.addListener(this::notifyListeners);
		dropShadow.addListener(this::notifyListeners);
		fontColor.addListener(this::notifyListeners);
	}

	public EnumSetting<TextAlignment> getAlignment() {
		return alignment;
	}

	public BooleanSetting getDropShadow() {
		return dropShadow;
	}

	public ColorSetting getFontColor() {
		return fontColor;
	}
}
