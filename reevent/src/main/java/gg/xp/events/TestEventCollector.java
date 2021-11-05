package gg.xp.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestEventCollector implements EventHandler<Event> {

	private final List<Event> eventsSeen = new ArrayList<>();

	@Override
	public void handle(EventContext<Event> context, Event event) {
		eventsSeen.add(event);
	}

	public List<Event> getEvents() {
		return Collections.unmodifiableList(eventsSeen);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <X extends Event> List<X> getEventsOf(Collection<Class<? extends X>> eventClasses) {
		return (List) eventsSeen
				.stream()
				.filter(event -> eventClasses.stream().anyMatch(clazz -> clazz.isInstance(event)))
				.collect(Collectors.toList());
	}
}
