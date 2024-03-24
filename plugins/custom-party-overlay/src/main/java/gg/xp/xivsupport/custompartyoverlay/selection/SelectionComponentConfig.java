package gg.xp.xivsupport.custompartyoverlay.selection;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.awt.*;

@ScanMe
public class SelectionComponentConfig extends ObservableSetting {

	private final ColorSetting borderColor;
	private final ColorSetting bgColor;
	private final BooleanSetting enableBorder;
	private final BooleanSetting enableBg;
	private final IntSetting borderThickness;
	private final IntSetting borderRadiusX;
	private final IntSetting borderRadiusY;
	private final BooleanSetting enableGradient;
	private final IntSetting gradientAngle;
	private final IntSetting gradientStart;
	private final IntSetting gradientLength;

	public SelectionComponentConfig(PersistenceProvider pers) {
		String settingBase = "custom-party-selection-component.";
		borderColor = new ColorSetting(pers, settingBase + "border-color", new Color(200, 255, 255, 200));
		borderColor.addListener(this::notifyListeners);
		bgColor = new ColorSetting(pers, settingBase + "bg-color", new Color(200, 255, 255, 128));
		bgColor.addListener(this::notifyListeners);
		enableBorder = new BooleanSetting(pers, settingBase + "enable-border", true);
		enableBorder.addListener(this::notifyListeners);
		enableBg = new BooleanSetting(pers, settingBase + "enable-bg", false);
		enableBg.addListener(this::notifyListeners);
		borderThickness = new IntSetting(pers, settingBase + "border-thickness", 3, 1, 50);
		borderThickness.addListener(this::notifyListeners);
		borderRadiusX = new IntSetting(pers, settingBase + "border-radius-x", 10, 1, 50);
		borderRadiusX.addListener(this::notifyListeners);
		borderRadiusY = new IntSetting(pers, settingBase + "border-radius-y", 10, 1, 50);
		borderRadiusY.addListener(this::notifyListeners);
		enableGradient = new BooleanSetting(pers, settingBase + "enable-gradient", true);
		gradientAngle = new IntSetting(pers, settingBase + "gradient-angle", 135, 0, 359);
		gradientAngle.addListener(this::notifyListeners);
		gradientStart = new IntSetting(pers, settingBase + "gradient-start", -80, -1000, 1000);
		gradientStart.addListener(this::notifyListeners);
		gradientLength = new IntSetting(pers, settingBase + "gradient-length", 150, 0, 1000);
		gradientLength.addListener(this::notifyListeners);
	}

	public ColorSetting getBorderColor() {
		return borderColor;
	}

	public ColorSetting getBgColor() {
		return bgColor;
	}

	public BooleanSetting getEnableBorder() {
		return enableBorder;
	}

	public BooleanSetting getEnableBg() {
		return enableBg;
	}

	public IntSetting getBorderThickness() {
		return borderThickness;
	}

	public IntSetting getBorderRadiusX() {
		return borderRadiusX;
	}

	public IntSetting getBorderRadiusY() {
		return borderRadiusY;
	}

	public BooleanSetting getEnableGradient() {
		return enableGradient;
	}

	public IntSetting getGradientAngle() {
		return gradientAngle;
	}

	public IntSetting getGradientStart() {
		return gradientStart;
	}

	public IntSetting getGradientLength() {
		return gradientLength;
	}
}
