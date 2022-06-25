package gg.xp.reevent.events;

import gg.xp.reevent.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

/**
 * Marker interface for events
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

	EventHandler<?> getSourceEventHandler();

	void setSourceEventHandler(EventHandler<?> source);

	default boolean shouldSave() {
		return getParent() == null;
	}

	boolean isImported();

	void setImported(boolean imported);

	default <X> @Nullable X getThisOrParentOfType(Class<X> clazz) {
		Event current = this;
		do {
			if (clazz.isInstance(current)) {
				return clazz.cast(current);
			}
		} while ((current = current.getParent()) != null);
		return null;

	}

	default Instant getEffectiveHappenedAt() {
		return getHappenedAt();
	}

	default @Nullable Event combineWith(Event event) {
		return null;
	};
}
