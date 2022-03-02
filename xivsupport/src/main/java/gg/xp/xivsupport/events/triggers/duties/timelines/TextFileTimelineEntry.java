package gg.xp.xivsupport.events.triggers.duties.timelines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record TextFileTimelineEntry(
		double time,
		@Nullable String name,
		@Nullable Pattern sync,
		@Nullable Double duration,
		@NotNull TimelineWindow timelineWindow,
		@Nullable Double jump
) implements TimelineEntry {
	@Override
	public String toString() {
		return "TextFileTimelineEntry{" +
				"time=" + time +
				", name='" + name + '\'' +
				", sync=" + sync +
				", duration=" + duration +
				", timelineWindow=" + timelineWindow +
				", jump=" + jump +
				'}';
	}
}
