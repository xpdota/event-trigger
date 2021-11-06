package gg.xp.events;

import gg.xp.context.StateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BasicEventDistributor implements EventDistributor<Event> {

	private static final Logger log = LoggerFactory.getLogger(BasicEventDistributor.class);

	private final List<EventHandler<Event>> handlers = new ArrayList<>();

	private final StateStore state = new StateStore();

	public void registerHandler(EventHandler<Event> handler) {
		handlers.add(handler);
	}

	// TODO: decide how to plumb this through
	@Override
	public StateStore getStateStore() {
		return state;
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
				handler.handle(
						new EventContext<>() {
							@Override
							public void accept(Event e) {
								if (e == tmpNext) {
									log.error("Event {} was re-submitted by {}!", e, handler);
								}
								else {
									log.trace("Event {} triggered new event {}", tmpNext, e);
									eventsForImmediateProcessing.add(e);
								}
							}

							@Override
							public void acceptToQueue(Event event) {
								throw new UnsupportedOperationException("Not implemented yet");
							}

							@Override
							public StateStore getStateInfo() {
								return state;
							}
						}, tmpNext);
				log.trace("Sent event {} to handler {}, now with {} immediate events", tmpNext, handler, eventsForImmediateProcessing.size());
			});
		}
	}
}
