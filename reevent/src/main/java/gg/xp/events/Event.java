package gg.xp.events;

import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for events
 * <p>
 * TODO: make a base class that handles timestamping
 * and provenance
 *
 * TODO: figure out if also tracking child events might use too much memory.
 * Perhaps SoftReference would be appropriate?
 */
public interface Event {

	@Nullable Event getParent();

	void setParent(Event parent);

	/**
	 * @return Timestamp (millis) for when the event should actually be enqueued. Used for delayed events.
	 */
	default long delayedEnqueueAt() {
		// 0 doesn't need to be handled specially - it will always be in the past
		return 0;
	}

	/**
	 * @return true if the event, when it is time for it to be enqueued, should jump the queue or not.
	 */
	default boolean delayedEnqueueAtFront() {
		return false;
	}
}
