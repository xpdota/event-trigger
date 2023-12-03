package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.Event;

public interface EventSyncController {
	boolean shouldSync(Event event);

	Class<? extends Event> eventType();

	default boolean isEditable() {
		return false;
	};
}
