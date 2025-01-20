package gg.xp.xivsupport.timelines.icon;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public record ActionTimelineIcon(long id) implements TimelineIcon {
	@Override
	public @Nullable URL getIconUrl() {
		ActionIcon actionIcon = ActionLibrary.iconForId(id);
		if (actionIcon == null) {
			return null;
		}
		return actionIcon.getIconUrl();
	}
}
