package gg.xp.xivsupport.events.misc;

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
}
