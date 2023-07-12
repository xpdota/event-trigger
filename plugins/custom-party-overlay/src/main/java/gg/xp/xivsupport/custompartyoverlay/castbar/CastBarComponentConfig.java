package gg.xp.xivsupport.custompartyoverlay.castbar;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import java.util.List;

@ScanMe
public class CastBarComponentConfig extends ObservableSetting {

	private final ColorSetting inProgressColor;
	private final ColorSetting successColor;
	private final ColorSetting interruptedColor;
	private final ColorSetting unknownColor;
	private final ColorSetting backgroundColor;
	private final ColorSetting textColor;
	private final EnumSetting<JobIconPlacement> jobIconSetting;

//	private final IntSetting fgTransparency;
//	private final IntSetting bgTransparency;
//
//	private final EnumSetting<BarFractionDisplayOption> fractionDisplayMode;

	public CastBarComponentConfig(PersistenceProvider pers) {
		String settingKeyBase = "custom-party-overlay.castbar.";
		inProgressColor = new ColorSetting(pers, settingKeyBase + "in-progress", CastBarComponent.defaultInProgressColor);
		successColor = new ColorSetting(pers, settingKeyBase + "success", CastBarComponent.defaultSuccessColor);
		interruptedColor = new ColorSetting(pers, settingKeyBase + "interrupted", CastBarComponent.defaultInterruptedColor);
		unknownColor = new ColorSetting(pers, settingKeyBase + "unknown", CastBarComponent.defaultUnknownColor);
		backgroundColor = new ColorSetting(pers, settingKeyBase + "bg", CastBarComponent.defaultBackgroundColor);
		textColor = new ColorSetting(pers, settingKeyBase + "textcolor", CastBarComponent.defaultTextColor);
		jobIconSetting = new EnumSetting<>(pers, settingKeyBase + "job-icon-placement", JobIconPlacement.class, JobIconPlacement.RIGHT);

//		fgTransparency = new IntSetting(pers, settingKeyBase + "fgtrans", 255, 0, 255);
//		bgTransparency = new IntSetting(pers, settingKeyBase + "bgtrans", 128, 0, 255);
//		fractionDisplayMode = new EnumSetting<>(pers, settingKeyBase + "fractionmode", BarFractionDisplayOption.class, BarFractionDisplayOption.AUTO);
		List.of(backgroundColor, inProgressColor, successColor, interruptedColor, unknownColor, textColor, jobIconSetting)
				.forEach(setting -> setting.addListener(this::notifyListeners));
	}

	public ColorSetting getInProgressColor() {
		return inProgressColor;
	}

	public ColorSetting getSuccessColor() {
		return successColor;
	}

	public ColorSetting getInterruptedColor() {
		return interruptedColor;
	}

	public ColorSetting getUnknownColor() {
		return unknownColor;
	}

	public ColorSetting getBackgroundColor() {
		return backgroundColor;
	}

	public ColorSetting getTextColor() {
		return textColor;
	}

	public EnumSetting<JobIconPlacement> getJobIconPlacementSetting() {
		return jobIconSetting;
	}
}
