package gg.xp.reevent.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestEventCollector implements EventHandler<Event> {

	private final Object lock = new Object();
	private final List<Event> eventsSeen = new ArrayList<>();

	@Override
	public void handle(EventContext context, Event event) {
		synchronized (lock) {
			eventsSeen.add(event);
		}
	}

	public List<Event> getEvents() {
		synchronized (lock) {
			return new ArrayList<>(eventsSeen);
		}
	}

	public <X extends Event> List<X> getEventsOf(Class<X> eventClass) {
		return getEvents()
				.stream()
				.filter(eventClass::isInstance)
				.map(eventClass::cast)
				.collect(Collectors.toList());
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	public <X extends Event> List<X> getEventsOf(Collection<Class<? extends X>> eventClasses) {
		return (List) getEvents()
				.stream()
				.filter(event -> eventClasses.stream().anyMatch(clazz -> clazz.isInstance(event)))
				.collect(Collectors.toList());
	}

	public void clear() {
		synchronized (lock) {
			eventsSeen.clear();
		}
	}
}
