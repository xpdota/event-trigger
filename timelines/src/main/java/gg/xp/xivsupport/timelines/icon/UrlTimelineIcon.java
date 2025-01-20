package gg.xp.xivsupport.timelines.icon;

import org.jetbrains.annotations.Nullable;

import java.net.URL;

public record UrlTimelineIcon(URL url) implements TimelineIcon {
	@Override
	public @Nullable URL getIconUrl() {
		return url;
	}
}
