package gg.xp.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestEventCollector implements EventHandler<Event> {

	private final List<Event> eventsSeen = new ArrayList<>();

	@Override
	public void handle(EventContext<Event> context, Event event) {
		eventsSeen.add(event);
	}

	public List<Event> getEvents() {
		return Collections.unmodifiableList(eventsSeen);
	}
}
