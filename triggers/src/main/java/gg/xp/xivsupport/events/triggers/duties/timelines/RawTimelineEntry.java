package gg.xp.xivsupport.events.triggers.duties.timelines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record RawTimelineEntry(
		double time,
		@Nullable String name,
		@Nullable Pattern sync,
		@Nullable Double duration,
		@NotNull TimelineWindow timelineWindow,
		@Nullable Double jump
) {

	public double getMinTime() {
		return time - timelineWindow().start();
	}

	public double getMaxTime() {
		return time + timelineWindow().end();
	}

	public boolean shouldSync(double currentTime, String line) {
		if (sync == null) {
			return false;
		}
		return currentTime >= getMinTime() && currentTime <= getMaxTime() && sync.matcher(line).find();
	}

	@Override
	public String toString() {
		return "RawTimelineEntry{" +
				"time=" + time +
				", name='" + name + '\'' +
				", sync=" + sync +
				", duration=" + duration +
				", timelineWindow=" + timelineWindow +
				", jump=" + jump +
				'}';
	}
}
