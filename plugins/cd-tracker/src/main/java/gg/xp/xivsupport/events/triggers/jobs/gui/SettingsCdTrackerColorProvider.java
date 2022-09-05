package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.persistence.settings.ColorSetting;

import java.awt.*;

public class SettingsCdTrackerColorProvider implements CdColorProvider {

	private final ColorSetting active;
	private final ColorSetting ready;
	private final ColorSetting onCd;
	private final ColorSetting preapp;
	private final ColorSetting font;

	public SettingsCdTrackerColorProvider(ColorSetting active, ColorSetting ready, ColorSetting onCd, ColorSetting preapp, ColorSetting font) {
		this.active = active;
		this.ready = ready;
		this.onCd = onCd;
		this.preapp = preapp;
		this.font = font;
	}

	@Override
	public Color getActiveColor() {
		return active.get();
	}

	@Override
	public Color getReadyColor() {
		return ready.get();
	}

	@Override
	public Color getOnCdColor() {
		return onCd.get();
	}

	@Override
	public Color getPreappColor() {
		return preapp.get();
	}

	@Override
	public Color getFontColor() {
		return font.get();
	}

	public ColorSetting getActiveSetting() {
		return active;
	}

	public ColorSetting getReadySetting() {
		return ready;
	}

	public ColorSetting getOnCdSetting() {
		return onCd;
	}

	public ColorSetting getPreappSetting() {
		return preapp;
	}

	public ColorSetting getFontSetting() {
		return font;
	}

}
