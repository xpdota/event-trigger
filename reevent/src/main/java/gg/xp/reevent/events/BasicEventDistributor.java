package gg.xp.reevent.events;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.scan.AutoHandler;
import gg.xp.reevent.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicEventDistributor implements EventDistributor {

	private static final Logger log = LoggerFactory.getLogger(BasicEventDistributor.class);
	private static final int MAX_EVENTS_PER_NATURAL_EVENT = 3000;

	protected final List<EventHandler<Event>> handlers = new ArrayList<>();
	private final Object handlersLock = new Object();

	private final StateStore state;
	private EventQueue queue;

	public BasicEventDistributor(StateStore state) {
		this.state = state;
	}

	// todo: sync object doesn't cover direct access as seen in subclass
	@Override
	public synchronized void registerHandler(EventHandler<Event> handler) {
		if (!(handler instanceof AutoHandler)) {
			log.info("Added manual handler: {}", handler);
		}
		synchronized (handlersLock) {
			handlers.add(handler);
			sortHandlers();
		}
	}

	protected void sortHandlers() {
		handlers.sort(Comparator.comparing(EventHandler::getOrder));
	}


	@Override
	public void setQueue(EventQueue queue) {
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
		int count = 0;
		Event next;
		while ((next = eventsForImmediateProcessing.poll()) != null) {
			count++;
			// Failsafe for infinite loops
			if (count > MAX_EVENTS_PER_NATURAL_EVENT) {
				log.error("Too many synthetic events from one natural event. Possible loop. Original event: {}", event);
				return;
			}
			final Event current = next;
			boolean updateTimes = !current.isImported();
			if (updateTimes) {
				current.setPumpedAt(TimeUtils.now());
			}
			// TODO: this doesn't work, because handlers filter it themselves
			if (handlers.isEmpty()) {
				log.warn("No handlers for event {}!", event);
			}
			List<EventHandler<Event>> handlersTmp;
			synchronized (handlersLock) {
				handlersTmp = new ArrayList<>(handlers);
			}
			if (handlersTmp.isEmpty()) {
				log.warn("No handlers!");
			}
			handlersTmp.forEach(handler -> {
				log.trace("Sending event {} to handler {} with {} immediate events", current, handler, eventsForImmediateProcessing.size());
				AtomicBoolean isDone = new AtomicBoolean();
				try {
					handler.handle(
							new EventContext() {
								@Override
								public void accept(Event e) {
									if (isDone.get()) {
										throw new IllegalStateException("You must submit new events at the time when the event handler is called. If you need async behavior, submit a new event for later processing.");
									}
									if (e == current) {
										log.error("Event {} was re-submitted by {}!", e, handler);
									}
									else {
										if (e.delayedEnqueueAt() > 0) {
											log.error("You called 'accept' on an event ({}) with a delayed enqueue - you probably meant to 'queue' it instead.", e.getClass().getSimpleName(), new RuntimeException("Dummy Exception to Generate Stack Trace"));
										}
										e.setParent(current);
										e.setEnqueuedAt(TimeUtils.now());
										e.setSourceEventHandler(handler);
										e.setHappenedAt(current.getHappenedAt());
										log.trace("Event {} triggered new event {}", current, e);
										eventsForImmediateProcessing.add(e);
									}
								}

								@Override
								public void enqueue(Event e) {
									if (isDone.get()) {
										// TODO: should this be the case? There should be no harm in enqueuing an event later.
										throw new IllegalStateException("You must submit new events at the time when the event handler is called. If you need async behavior, submit a new event for later processing.");
									}
									if (queue != null) {
										if (e == current) {
											log.error("Event {} was re-submitted by {}!", e, handler);
										}
										else {
											e.setParent(current);
											e.setSourceEventHandler(handler);
											queue.push(e);
										}
									}
									else {
										throw new IllegalStateException("Cannot push to queue if there is no queue attached to this distributor");
									}
								}

								@Override
								public StateStore getStateInfo() {
									if (isDone.get()) {
										throw new IllegalStateException("You must get state info at the time when the event handler is called. If you need async behavior, either immediately submit a new event for later processing, or snapshot the needed data before exiting your handler method.");
									}
									return state;
								}
							}, current);
					isDone.set(true);
					log.trace("Sent event {} to handler {}, now with {} immediate events", current, handler, eventsForImmediateProcessing.size());
				}
				catch (Throwable t) {
					log.error("Error pumping event {} into handler {}", event, handler, t);
				}
			});
			if (updateTimes) {
				current.setPumpFinishedAt(TimeUtils.now());
			}
		}
	}
}
