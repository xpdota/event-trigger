package gg.xp.xivdata.data;

import java.net.URL;
import java.util.Objects;

public class URLIcon implements HasIconURL {
	private final URL url;

	public URLIcon(URL url) {
		this.url = url;
	}

	@Override
	public URL getIconUrl() {
		return url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		URLIcon urlIcon = (URLIcon) o;
		return Objects.equals(url.toString(), urlIcon.url.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(url.toString());
	}
}
