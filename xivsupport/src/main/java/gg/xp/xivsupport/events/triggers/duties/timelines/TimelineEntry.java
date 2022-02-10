package gg.xp.xivsupport.events.triggers.duties.timelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivdata.jobs.HasIconURL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.regex.Pattern;

public interface TimelineEntry extends Comparable<TimelineEntry> {

	@JsonIgnore
	default double getMinTime() {
		return time() - timelineWindow().start();
	}

	@JsonIgnore
	default double getMaxTime() {
		return time() + timelineWindow().end();
	}


	default boolean shouldSync(double currentTime, String line) {
		Pattern sync = sync();
		if (sync == null) {
			return false;
		}
		return currentTime >= getMinTime() && currentTime <= getMaxTime() && sync.matcher(line).find();
	}

	@JsonIgnore
	default double getSyncToTime() {
		Double jump = jump();
		if (jump == null) {
			return time();
		}
		else {
			return jump;
		}

	}

	@Override
	@Nullable String toString();

	double time();

	@Nullable String name();

	@Nullable Pattern sync();

	@Nullable Double duration();

	@NotNull TimelineWindow timelineWindow();

	@Nullable Double jump();

	@Override
	default int compareTo(@NotNull TimelineEntry o) {
		return Double.compare(this.time(), o.time());
	}

	default @Nullable URL icon() {
		return null;
	}
}
