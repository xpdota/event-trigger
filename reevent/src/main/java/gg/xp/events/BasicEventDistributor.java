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

	protected final List<EventHandler<Event>> handlers = new ArrayList<>();

	private final StateStore state = new StateStore();
	private EventQueue<Event> queue;

	public void registerHandler(EventHandler<Event> handler) {
		handlers.add(handler);
	}

	public void setQueue(EventQueue<Event> queue) {
		this.queue = queue;
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
			final Event current = next;
			handlers.forEach(handler -> {
				log.trace("Sending event {} to handler {} with {} immediate events", current, handler, eventsForImmediateProcessing.size());
				handler.handle(
						new EventContext<>() {
							@Override
							public void accept(Event e) {
								if (e == current) {
									log.error("Event {} was re-submitted by {}!", e, handler);
								}
								else {
									e.setParent(current);
									log.trace("Event {} triggered new event {}", current, e);
									eventsForImmediateProcessing.add(e);
								}
							}

							@Override
							public void enqueue(Event e) {
								if (queue != null) {
									if (e == current) {
										log.error("Event {} was re-submitted by {}!", e, handler);
									}
									else {
										e.setParent(current);
										queue.push(e);
									}
								}
								else {
									throw new IllegalStateException("Cannot push to queue if there is no queue attached to this distributor");
								}
							}

							@Override
							public StateStore getStateInfo() {
								return state;
							}
						}, current);
				log.trace("Sent event {} to handler {}, now with {} immediate events", current, handler, eventsForImmediateProcessing.size());
			});
		}
	}
}
