package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.persistence.settings.ColorSetting;

import java.awt.*;

public class SettingsCdTrackerColorProvider implements CdColorProvider {

	private final ColorSetting active;
	private final ColorSetting ready;
	private final ColorSetting onCd;

	public SettingsCdTrackerColorProvider(ColorSetting active, ColorSetting ready, ColorSetting onCd) {
		this.active = active;
		this.ready = ready;
		this.onCd = onCd;
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

	public ColorSetting getActiveSetting() {
		return active;
	}

	public ColorSetting getReadySetting() {
		return ready;
	}

	public ColorSetting getOnCdSetting() {
		return onCd;
	}
}
