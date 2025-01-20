package gg.xp.xivdata.data;

import java.net.MalformedURLException;
import java.net.URL;

public final class IconUtils {

	private IconUtils() {
	}

	public static URL iconUrl(int iconId) {
		return IconUtils.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
	}

	public static URL iconUrlWithXivapiFallback(int iconId) {
		URL out = IconUtils.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
		if (out == null) {
			long stub = (iconId / 1000) * 1000;
			// Example: https://beta.xivapi.com/api/1/asset/ui/icon/218000/218443.tex?format=png
			String xivapiUrl = String.format("https://beta.xivapi.com/api/1/asset/ui/icon/%06d/%06d_hr1.tex?format=png", stub, iconId);
			try {
				return new URL(xivapiUrl);
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return out;
	}

	public static HasIconURL makeIcon(int iconId) {
		URL url = iconUrl(iconId);
		return () -> url;
	}

}
