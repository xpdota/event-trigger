package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FileEventSyncController implements EventSyncController {

	private final Class<? extends Event> eventType;
	private final Predicate<Event> predicate;
	private final Map<String, String> original;

	public FileEventSyncController(Class<? extends Event> eventType, Predicate<Event> predicate, Map<String, String> original) {
		this.eventType = eventType;
		this.predicate = predicate;
		this.original = new HashMap<>(original);
	}

	@Override
	public boolean shouldSync(Event event) {
		return predicate.test(event);
	}

	@Override
	public Class<? extends Event> eventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return eventType.getSimpleName() + original;
	}
}
