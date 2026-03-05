package gg.xp.xivsupport.timelines.icon;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public record IconIdTimelineIcon(int id) implements TimelineIcon {
	@Override
	public @Nullable URL getIconUrl() {
		return IconUtils.iconUrlWithXivapiFallback(id);
	}
}
