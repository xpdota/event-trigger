package gg.xp.reevent.time;

import java.time.Duration;
import java.time.Instant;

public final class TimeUtils {

	private static Instant last;

	private TimeUtils() {
	}

	// Memory saver - re-use instant objects
	public static Instant now() {
		Instant now = Instant.now();
		//noinspection StaticVariableUsedBeforeInitialization
		Instant last = TimeUtils.last;
		if (now.equals(last)) {
			return last;
		}
		return TimeUtils.last = now;
	}

	public static Duration clampDuration(Duration input, Duration min, Duration max) {
		if (input.compareTo(min) < 0) {
			return min;
		}
		else if (input.compareTo(max) > 0) {
			return max;
		}
		else {
			return input;
		}
	}
}
