package gg.xp.xivdata.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class StatusEffectIcon implements HasIconURL {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectIcon.class);

	private final URL url;

	StatusEffectIcon(URL url) {
		this.url = url;
	}

	@Override
	public URL getIconUrl() {
		return url;
	}
}
