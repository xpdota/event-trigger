package gg.xp.xivsupport.timelines.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;

import java.awt.*;

@ScanMe
public class TimelineBarColorProviderImpl implements TimelineBarColorProvider {

	private final ColorSetting upcoming;
	private final ColorSetting active;
	private final ColorSetting expired;
	private final ColorSetting font;

	public TimelineBarColorProviderImpl(PersistenceProvider pers) {
		this.upcoming = new ColorSetting(pers, "timeline-overlay.colors.upcoming", TimelineBarRenderer.defaultColorUpcoming);
		this.active = new ColorSetting(pers, "timeline-overlay.colors.active", TimelineBarRenderer.defaultColorActive);
		this.expired = new ColorSetting(pers, "timeline-overlay.colors.expired", TimelineBarRenderer.defaultColorExpired);
		this.font = new ColorSetting(pers, "timeline-overlay.colors.font", TimelineBarRenderer.defaultFontColor);
	}

	@Override
	public Color getFontColor() {
		return font.get();
	}

	@Override
	public Color getUpcomingColor() {
		return upcoming.get();
	}

	@Override
	public Color getActiveColor() {
		return active.get();
	}

	@Override
	public Color getExpiredColor() {
		return expired.get();
	}

	public ColorSetting getUpcomingSetting() {
		return upcoming;
	}

	public ColorSetting getActiveSetting() {
		return active;
	}

	public ColorSetting getExpiredSetting() {
		return expired;
	}

	public ColorSetting getFontSetting() {
		return font;
	}
}
