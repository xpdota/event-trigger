package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.time.TimeUtils;
import org.apache.commons.lang3.time.DurationUtils;

import java.time.Duration;

/**
 * Represents something with a duration.
 */
public interface HasDuration {
	/**
	 * @return The initial duration at the time of the event happening.
	 */
	Duration getInitialDuration();

	/**
	 * @return The elapsed duration. Note that this takes {@link #getEffectiveTimeSince()} into account, so it
	 * will respect fake time sources.
	 * <p>
	 * Note that this is bounded. It will never return a value less than {@link Duration#ZERO}, nor a value greater
	 * than {@link #getInitialDuration()}.
	 */
	default Duration getEstimatedElapsedDuration() {
		Duration delta = getEffectiveTimeSince();
		// If negative, return zero. If longer than expected duration, return duration.
		return TimeUtils.clampDuration(delta, Duration.ZERO, getInitialDuration());
	}

	/**
	 * @return The elapsed duration. Note that this takes {@link #getEffectiveTimeSince()} into account, so it
	 * will respect fake time sources.
	 */
	default Duration getEstimatedRemainingDuration() {
		return getInitialDuration().minus(getEstimatedElapsedDuration());
	}

	/**
	 * Like {@link #getEstimatedRemainingDuration()}, but instead of using the normal expiry time, it offsets from the
	 * normal.
	 *
	 * @param offset The duration offset. Positive means later, negative means earlier.
	 * @return The remaining duration until the expiry time plus this offset, not to exceed 'offset' nor be negative.
	 */
	@SuppressWarnings("unused") // Used in Groovy scripts
	default Duration remainingDurationPlus(Duration offset) {
		Duration fakeDuration = getInitialDuration().plus(offset);
		return TimeUtils.clampDuration(fakeDuration.minus(getEffectiveTimeSince()), Duration.ZERO, fakeDuration);
	}

	/**
	 * @return The time between the current time, and when the duration should have expired. If called before expiry
	 * time, behavior is undefined.
	 */
	default Duration getEstimatedTimeSinceExpiry() {
		Duration elapsed = getEffectiveTimeSince();
		return elapsed.minus(getInitialDuration());
	}

	/**
	 * Must be implemented.
	 *
	 * @return The time since this event has occurred.
	 */
	Duration getEffectiveTimeSince();

	/**
	 * @return true if and only if the current time is later than this event should have hit its full duration.
	 */
	default boolean wouldBeExpired() {
		return getEstimatedRemainingDuration().isZero();
	}

	default boolean isIndefinite() {
		return getInitialDuration().toSeconds() == 9999;
	}
}
