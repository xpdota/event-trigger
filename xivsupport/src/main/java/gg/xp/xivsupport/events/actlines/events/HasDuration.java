package gg.xp.xivsupport.events.actlines.events;

import java.time.Duration;

public interface HasDuration {
	Duration getInitialDuration();

	default Duration getEstimatedElapsedDuration() {
		Duration delta = getEffectiveTimeSince();
		// If negative, return zero. If longer than expected duration, return duration.
		if (delta.isNegative()) {
			return Duration.ZERO;
		}
		else {
			Duration initialDuration = getInitialDuration();
			if (delta.compareTo(initialDuration) > 0) {
				return initialDuration;
			}
			else {
				return delta;
			}
		}
	}

	default Duration getEstimatedRemainingDuration() {
		return getInitialDuration().minus(getEstimatedElapsedDuration());
	}

	default Duration getEstimatedTimeSinceExpiry() {
		Duration elapsed = getEffectiveTimeSince();
		return elapsed.minus(getInitialDuration());
	}

	Duration getEffectiveTimeSince();

	default boolean wouldBeExpired() {
		return getEstimatedRemainingDuration().isZero();
	}
}
