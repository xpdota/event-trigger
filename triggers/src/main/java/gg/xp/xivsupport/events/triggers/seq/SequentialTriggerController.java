package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.HasCalloutTrackingKey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class SequentialTriggerController<X extends BaseEvent> {

	private static final Logger log = LoggerFactory.getLogger(SequentialTriggerController.class);
	private static final AtomicInteger threadIdCounter = new AtomicInteger();
	//	private final Instant expiresAt;
	private final BooleanSupplier expired;
	private final Thread thread;
	private final Object lock = new Object();
	private final X initialEvent;
	private volatile X currentEvent;
	private volatile EventContext context;
	private volatile boolean done;
	private volatile boolean processing = true;
	private volatile boolean die;
	private volatile boolean cycleProcessingTimeExceeded;
	private volatile @Nullable Predicate<X> filter;

	// To be called from external thread
	public SequentialTriggerController(EventContext initialEventContext, X initialEvent, BiConsumer<X, SequentialTriggerController<X>> triggerCode, int timeout) {
		expired = () -> initialEvent.getEffectiveTimeSince().toMillis() > timeout;
//		expiresAt = initialEvent.getHappenedAt().plusMillis(timeout);
		context = initialEventContext;
		thread = new Thread(() -> {
			try {
				triggerCode.accept(initialEvent, this);
			}
			catch (Throwable t) {
				log.error("Error in sequential trigger", t);
			}
			finally {
				synchronized (lock) {
					log.info("Sequential trigger done");
					processing = false;
					done = true;
					lock.notifyAll();
				}
			}
		}, "SequentialTrigger-" + threadIdCounter.getAndIncrement());
		this.initialEvent = initialEvent;
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
		synchronized (lock) {
			waitProcessingDone();
		}
	}

	// To be called from internal thread
	public void accept(Event event) {
		log.info("Accepting: {}", event);
		context.accept(event);
	}

	public void enqueue(Event event) {
		log.info("Enqueueing: {}", event);
		context.enqueue(event);
	}

	public void refreshCombatants(long delay) {
		accept(new RefreshCombatantsRequest());
		waitMs(delay);
	}

	@SystemEvent
	static class DelayedSqtEvent extends BaseDelayedEvent {

		@Serial
		private static final long serialVersionUID = -4212233775615486768L;

		DelayedSqtEvent(long ms) {
			super(ms);
		}
	}

	public void waitMs(long ms) {
		log.trace("in waitMs");
		if (ms <= 0) {
			log.warn("waitMs called with non-positive value: {}", ms);
		}
		// This can stop waiting when it hits the original event (due to the delay), OR any other event (which is more
		// likely when replaying)
		DelayedSqtEvent event = new DelayedSqtEvent(ms);
		enqueue(event);
		long initial = initialEvent.getEffectiveTimeSince().toMillis();
		long doneAt = initial + ms;
		waitEvent(BaseEvent.class, e -> initialEvent.getEffectiveTimeSince().toMillis() >= doneAt);
	}

	private @Nullable HasCalloutTrackingKey lastCall;

	/**
	 * Accept a new callout event, BUT mark it as "replacing" any previous call
	 * i.e. update callout text + emit a new TTS
	 *
	 * @param call The new callout
	 */
	public void updateCall(CalloutEvent call) {
		if (lastCall != null) {
			call.setReplaces(lastCall);
		}
		lastCall = call;
		accept(call);
	}

	/**
	 * @return The duration since this trigger began
	 */
	public Duration timeSinceStart() {
		return initialEvent.getEffectiveTimeSince();
	}

	/**
	 * Accept a new callout event, BUT mark it as "replacing" any previous call
	 * i.e. update callout text + emit a new TTS
	 *
	 * @param call The new callout
	 */
	public void updateCall(RawModifiedCallout<?> call) {
		if (lastCall != null) {
			call.setReplaces(lastCall);
		}
		lastCall = call;
		accept(call);
	}


	// TODO: warning/error if you try to wait for an event type that is incompatible with X
	// i.e. if both X and Y are concrete types, and Y is not a subclass of X, then it should fail-fast

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass) {
		return waitEvent(eventClass, (e) -> true);
	}

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass, Predicate<Y> eventFilter) {
		log.trace("Waiting for specific event");
		return (Y) waitEvent(event -> eventClass.isInstance(event) && eventFilter.test((Y) event));
	}

	// To be called from internal thread
	public <Y> List<Y> waitEvents(int events, Class<Y> eventClass, Predicate<Y> eventFilter) {
		return IntStream.range(0, events)
				.mapToObj(i -> waitEvent(eventClass, eventFilter))
				.toList();
	}

	/**
	 * Wait for a certain number of events, but with multiple filters in an 'or' fashion. This method returns a map
	 * of filters to a list of events that passed that filter. With 'exclusive' set to true, an event that matches
	 * more than one filter will only be assigned to the first in the return value.
	 *
	 * @param limit      Number of events
	 * @param timeoutMs  Timeout in ms to wait for events
	 * @param eventClass Class of event
	 * @param exclusive  false if you would like an event to be allowed to match multiple filters, rather than movingn
	 *                   on to the next event after a single match.
	 * @param filters    The list of filters.
	 * @param <P>        The type of filter.
	 * @param <Y>        The type of event.
	 * @return A map, where the keys are the filters, and the values are a list of events that matched that
	 * filter.
	 */
	public <P extends Predicate<? super Y>, Y> Map<P, List<Y>> groupEvents(int limit, int timeoutMs, Class<Y> eventClass, boolean exclusive, List<P> filters) {
		Duration end = timeSinceStart().plusMillis(timeoutMs);
//		log.info("End ms: {}", end.toMillis());
		DelayedSqtEvent event = new DelayedSqtEvent(timeoutMs);
		enqueue(event);
		List<Y> rawEvents = waitEventsUntil(limit, eventClass, e -> filters.stream().anyMatch(p -> p.test(e)), BaseEvent.class, unused -> {
			Duration tss = timeSinceStart();
//			log.info("TSS ms: {}", tss.toMillis());
			return tss.compareTo(end) > 0;
		});
		log.info("groupEvents: {} total", rawEvents.size());
		Map<P, List<Y>> out = new HashMap<>(filters.size());
		// Even if there were no matches for a particular filter, we should set that key's value to an empty list
		for (P filter : filters) {
			out.put(filter, new ArrayList<>());
		}
		for (Y rawEvent : rawEvents) {
			for (P filter : filters) {
				if (filter.test(rawEvent)) {
					out.get(filter).add(rawEvent);
					if (exclusive) {
						break;
					}
				}
			}
		}
		return out;
	}

	/**
	 * Like {@link #groupEvents(int, int, Class, boolean, List)}, but uses {@link EventCollector} objects to filter
	 * and collect the result. This method works exactly like groupEvents, but does not return anything. Rather, you
	 * would query each of your EventCollectors to see what was matched. Each collector specifies what should be
	 * matched, and will be populated with the matches.
	 *
	 * @param limit      Number of events
	 * @param timeoutMs  Timeout in ms to wait for events
	 * @param eventClass Class of event
	 * @param exclusive  false if you would like an event to be allowed to match multiple filters, rather than movingn
	 *                   on to the next event after a single match.
	 * @param collectors The list of collectors.
	 * @param <Y>        The type of event.
	 */
	public <Y> void collectEvents(int limit, int timeoutMs, Class<Y> eventClass, boolean exclusive, List<EventCollector<? super Y>> collectors) {
		Map<EventCollector<? super Y>, List<Y>> result = groupEvents(limit, timeoutMs, eventClass, exclusive, collectors);
		for (EventCollector<? super Y> collector : collectors) {
			collector.provideEvents(result.get(collector));
		}
	}

	public <Y extends BaseEvent> List<Y> waitEventsQuickSuccession(int limit, Class<Y> eventClass, Predicate<Y> eventFilter, Duration maxDelta) {
		List<Y> out = new ArrayList<>();
		Y last = waitEvent(eventClass, eventFilter);
		out.add(last);
		while (true) {
			X event = waitEvent(e -> true);
			// First possibility - event we're interested int
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				out.add((Y) event);
				last = (Y) event;
				// If we have reached the limit, return it now
				if (out.size() >= limit) {
					return out;
				}
			}
			// Second possibility - hit our stop trigger
			else if (last.getEffectiveTimeSince().compareTo(maxDelta) > 0) {
				log.info("Sequential trigger stopping on {}", event);
				return out;
			}
			// Third possibility - keep looking
		}
	}

	public <Y, Z> List<Y> waitEventsUntil(int limit, Class<Y> eventClass, Predicate<Y> eventFilter, Class<Z> stopOnType, Predicate<Z> stopOn) {
		List<Y> out = new ArrayList<>();
		while (true) {
			X event = waitEvent(e -> true);
			// First possibility - event we're interested int
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				out.add((Y) event);
				// If we have reached the limit, return it now
				if (out.size() >= limit) {
					return out;
				}
			}
			// Second possibility - hit our stop trigger
			else if (stopOnType.isInstance(event) && stopOn.test((Z) event)) {
				log.info("Sequential trigger stopping on {}", event);
				return out;
			}
			// Third possibility - keep looking
		}
	}

	// To be called from internal thread
	private X waitEvent(Predicate<X> filter) {
		synchronized (lock) {
			processing = false;
			currentEvent = null;
			context = null;
			this.filter = filter;
			lock.notifyAll();
			while (true) {
				if (die) {
					// Deprecated, but.......?
					// Seems better than leaving threads hanging around doing nothing.
//					thread.stop();
					throw new SequentialTriggerTimeoutException("Trigger ran out of time waiting for event");
				}
				if (cycleProcessingTimeExceeded) {
					throw new SequentialTriggerTimeoutException("Trigger exceeded max cycle time");
				}
				if (currentEvent != null) {
					X event = currentEvent;
					currentEvent = null;
					this.filter = null;
					return event;
				}

				try {
					lock.wait();
				}
				// TODO: use this as a stop condition
				catch (InterruptedException e) {
					throw new SequentialTriggerTimeoutException("Trigger was interrupted", e);
				}
			}
		}
	}

	// To be called from external thread
	public void provideEvent(EventContext ctx, X event) {
		synchronized (lock) {
			// TODO: expire on wipe?
			// Also make it configurable as to whether or not a wipe ends the trigger
			if (expired.getAsBoolean()) {
//			if (event.getHappenedAt().isAfter(expiresAt)) {
				log.warn("Sequential trigger expired by event: {}", event);
				die = true;
				lock.notifyAll();
				return;
			}
			Predicate<X> filt = filter;
			if (filt != null && !filt.test(event)) {
				return;
			}
			// First, set fields
			context = ctx;
			currentEvent = event;
			// Indicate that we are currently processing
			processing = true;
			// Then, tell it to resume
			lock.notifyAll();
			// Wait for it to be done
			waitProcessingDone();
		}
	}

	private static final int defaultCycleProcessingTime = 100;
	private static final int cycleProcessingTime;

	// Workaround for integration tests exceeding cycle time
	static {
		String prop = System.getProperty("sequentialTriggerCycleTime");
		if (prop != null) {
			int value;
			try {
				value = Integer.parseInt(prop);
			}
			catch (NumberFormatException nfe) {
				value = defaultCycleProcessingTime;
			}
//			cycleProcessingTime = value;
			// TODO
			cycleProcessingTime = 30000;
		}
		else {
			cycleProcessingTime = defaultCycleProcessingTime;
		}
	}

	private void waitProcessingDone() {
		// "done" means waiting for another event
		long startTime = System.currentTimeMillis();
		int timeoutMs = cycleProcessingTime;
		long failAt = startTime + timeoutMs;
		while (processing && !done) {
			try {
				long timeLeft = failAt - System.currentTimeMillis();
				if (timeLeft <= 0) {
					log.error("Cycle processing time max ({}ms) exceeded", timeoutMs);
					cycleProcessingTimeExceeded = true;
					return;
				}
				//noinspection WaitNotifyWhileNotSynced
				lock.wait(timeLeft);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	// To be called from external thread
	public boolean isDone() {
		return done || die;
	}
}