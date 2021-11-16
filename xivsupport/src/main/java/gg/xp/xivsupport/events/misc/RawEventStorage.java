package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RawEventStorage {

	private static final Logger log = LoggerFactory.getLogger(RawEventStorage.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();

	private static final int MAX_EVENTS_STORED = 25_000;
	private static final int EVENTS_TO_PRUNE = 10_000;
	// TODO: cap this or otherwise manage memory
	private List<Event> events = new ArrayList<>();

	@HandleEvents(order = Integer.MIN_VALUE)
	public void storeEvent(EventContext<Event> context, Event event) {
		events.add(event);
		if (events.size() > MAX_EVENTS_STORED) {
			log.info("Pruning events");
			events = new ArrayList<>(events.subList(EVENTS_TO_PRUNE, events.size()));
			exs.submit(System::gc);
		}
	}

	@HandleEvents
	public void clear(EventContext<Event> context, DebugCommand event) {
		if ("clear".equals(event.getCommand())) {
			events = new ArrayList<>();
		}
	}

	public List<Event> getEvents() {
		// Trying new thing
		return new ProxyForAppendOnlyList<>(events);

		// I don't really like this - I'd rather not copy the whole thing, but in theory the list could be trimmed
		// while something still has an open view of the list here
		// TODO: is this even threadsafe? In theory, it should be (apart from maybe missing the most recent event or two,
		// because ArrayList.add adds the data *before* incrementing the size.
//		return new ArrayList<>(events);
		// TODO: this would be nice to get working, but current implementations don't work for it.
		// What we need is an append-only list implementation that:
		// - Supports a read-only subList view
		// - Does not invalidate iterator state on such views when the mainline list is appended to,
		//   because the iterator wouldn't see anything new anyway.
		// Quick and dirty way would be to copy ArrayList, prohibit all modifications other than a simple
		// add(), and have the sub-list iterator ignore concurrent modifications
		// Implementation idea: use nested so we never have to copy data on list growing
//		return Collections.unmodifiableList(events.subList(0, events.size()));
	}
}
