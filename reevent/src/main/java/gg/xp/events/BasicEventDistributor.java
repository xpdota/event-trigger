package gg.xp.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BasicEventDistributor implements EventDistributor<Event> {

	private static final Logger log = LoggerFactory.getLogger(BasicEventDistributor.class);

	private final List<EventHandler<Event>> handlers = new ArrayList<>();

	public void registerHandler(EventHandler<Event> handler) {
		handlers.add(handler);
	}

	@Override
	public void acceptEvent(Event event) {
		Queue<Event> eventsForImmediateProcessing = new ArrayDeque<>();
		eventsForImmediateProcessing.add(event);
		Event next;
		while ((next = eventsForImmediateProcessing.poll()) != null) {
			final Event tmpNext = next;
			handlers.forEach(handler -> {
				log.trace("Sending event {} to handler {} with {} immediate events", tmpNext, handler, eventsForImmediateProcessing.size());
				handler.handle(e -> {
					log.trace("Event {} triggered new event {}", tmpNext, e);
					eventsForImmediateProcessing.add(e);
				}, tmpNext);
				log.trace("Sent event {} to handler {}, now with {} immediate events", tmpNext, handler, eventsForImmediateProcessing.size());
			});
		}
	}
}
