package gg.xp.xivsupport.timelines.icon;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public record StatusTimelineIcon(long id, int stacks) implements TimelineIcon {
	@Override
	public @Nullable URL getIconUrl() {
		int max = StatusEffectLibrary.getMaxStacks(id);
		int stacks = this.stacks;
		if (stacks > max) {
			stacks = max;
		}
		@Nullable StatusEffectIcon statusIcon = StatusEffectLibrary.iconForId(id, stacks);
		if (statusIcon == null) {
			return null;
		}
		return statusIcon.getIconUrl();
	}
}
