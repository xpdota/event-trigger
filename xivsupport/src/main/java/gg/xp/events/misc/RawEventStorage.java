package gg.xp.events.misc;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.debug.DebugCommand;
import gg.xp.scan.HandleEvents;

import java.util.ArrayList;
import java.util.List;

public class RawEventStorage {

	// TODO: cap this or otherwise manage memory
	private final List<Event> events = new ArrayList<>();

	@HandleEvents(order = Integer.MIN_VALUE)
	public void storeEvent(EventContext<Event> context, Event event) {
		events.add(event);
	}

	@HandleEvents
	public void clear(EventContext<Event> context, DebugCommand event) {
		if ("clear".equals(event.getCommand())) {
			events.clear();
		}
	}

	public List<Event> getEvents() {
		// I don't really like this - I'd rather not copy the whole thing, but in theory the list could be trimmed
		// while something still has an open view of the list here
		// TODO: is this even threadsafe? In theory, it should be (apart from maybe missing the most recent event or two,
		// because ArrayList.add adds the data *before* incrementing the size.
		return new ArrayList<>(events);
	}
}
