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

	public static Instant clampInstant(Instant input, Instant min, Instant max) {
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

	/**
	 * Converts a duration to a double representing seconds and ms.
	 *
	 * e.g. 1200ms becomes 1.2. 1.33333.... becomes 1.333.
	 *
	 * @param input A duration
	 * @return      A double representing seconds, with 3 digits of precision past the decimal.
	 */
	public static double durationToDouble(Duration input) {
		return input.toMillis() / 1000.0;
	}
}
