package gg.xp.xivdata.data;

import java.net.URL;

public final class IconUtils {

	private IconUtils() {
	}

	public static URL iconUrl(int iconId) {
		return IconUtils.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
	}

	public static HasIconURL makeIcon(int iconId) {
		URL url = iconUrl(iconId);
		return () -> url;
	}

}
