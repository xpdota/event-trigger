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
			return xivApiIconUrl(iconId);
		}
		return out;
	}

	public static URL xivApiIconUrl(int iconId) {
		int stub = (iconId / 1000) * 1000;
		// Example: https://v2.xivapi.com/api/asset?path=ui%2Ficon%2F062000%2F062140_hr1.tex&format=png
		// Normally, I'd use a proper URL library, but this trusted data
		String assetPath = "ui/icon/%06d/%06d_hr1.tex".formatted(stub, (long) iconId);
		String escapedAssetPath = assetPath.replaceAll("/", "%2F");
		String xivapiUrl = String.format("https://v2.xivapi.com/api/asset?path=%s&format=png", escapedAssetPath);
		try {
			return new URL(xivapiUrl);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static HasIconURL makeIcon(int iconId) {
		URL url = iconUrl(iconId);
		return () -> url;
	}

}
