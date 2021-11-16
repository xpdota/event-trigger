package gg.xp.reevent.events;

import gg.xp.reevent.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

/**
 * Marker interface for events
 * <p>
 * TODO: make a base class that handles timestamping
 * and provenance
 *
 * TODO: figure out if also tracking child events might use too much memory.
 * Perhaps SoftReference would be appropriate?
 */
public interface Event extends Serializable {

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

	default Map<Field, Object> dumpFields() {
		return Utils.dumpAllFields(this);
	}

	Instant getHappenedAt();

	void setHappenedAt(Instant happenedAt);

	Instant getEnqueuedAt();

	void setEnqueuedAt(Instant enqueuedAt);

	Instant getPumpedAt();

	void setPumpedAt(Instant pumpedAt);

	Instant getPumpFinishedAt();

	void setPumpFinishedAt(Instant pumpedAt);
}
