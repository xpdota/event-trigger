package gg.xp.xivsupport.events.triggers.jobs.gui;

import java.awt.*;

public final class DefaultCdTrackerColorProvider implements CdColorProvider {

	public static final DefaultCdTrackerColorProvider INSTANCE = new DefaultCdTrackerColorProvider();

	private static final Color defaultColorActive = new Color(19, 8, 201, 192);
	private static final Color defaultColorReady = new Color(55, 182, 67, 192);
	private static final Color defaultColorOnCd = new Color(192, 0, 0, 192);

	private DefaultCdTrackerColorProvider() {}


	@Override
	public Color getActiveColor() {
		return defaultColorActive;
	}

	@Override
	public Color getReadyColor() {
		return defaultColorReady;
	}

	@Override
	public Color getOnCdColor() {
		return defaultColorOnCd;
	}
}
