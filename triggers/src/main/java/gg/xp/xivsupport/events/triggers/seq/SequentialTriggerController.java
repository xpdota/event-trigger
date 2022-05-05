package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
	private volatile X currentEvent;
	private volatile EventContext context;
	private volatile boolean done;
	private volatile boolean processing = true;
	private volatile boolean die;

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

	private @Nullable CalloutEvent lastCall;

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

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass) {
		return waitEvent(eventClass, (e) -> true);
	}

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass, Predicate<Y> eventFilter) {
		log.trace("Waiting for specific event");
		while (true) {
			X event = waitEvent();
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				log.trace("Done waiting for specific event, got: {}", event);
				return (Y) event;
			}
		}
	}

	// To be called from internal thread
	public <Y> List<Y> waitEvents(int events, Class<Y> eventClass, Predicate<Y> eventFilter) {
		return IntStream.range(0, events)
				.mapToObj(i -> waitEvent(eventClass, eventFilter))
				.toList();
	}

	public <Y, Z> List<Y> waitEventsUntil(int limit, Class<Y> eventClass, Predicate<Y> eventFilter, Class<Z> stopOnType, Predicate<Z> stopOn) {
		List<Y> out = new ArrayList<>();
		while (true) {
			X event = waitEvent();
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
				return out;
			}
			// Third possibility - keep looking
		}
	}

	// To be called from internal thread
	private X waitEvent() {
		synchronized (lock) {
			processing = false;
			currentEvent = null;
			context = null;
			lock.notifyAll();
			while (true) {
				if (die) {
					// Deprecated, but.......?
					// Seems better than leaving threads hanging around doing nothing.
//					thread.stop();
					throw new SequentialTriggerTimeoutException("Trigger ran out of time waiting for event");
				}
				if (currentEvent != null) {
					X event = currentEvent;
					currentEvent = null;
					return event;
				}

				try {
					lock.wait();
				}
				// TODO: use this as a stop condition
				catch (InterruptedException e) {
					throw new SequentialTriggerTimeoutException("Trigger was interrupted event");
				}
			}
		}
	}

	// To be called from external thread
	public void provideEvent(EventContext ctx, X event) {
		synchronized (lock) {
			if (expired.getAsBoolean()) {
//			if (event.getHappenedAt().isAfter(expiresAt)) {
				log.warn("Sequential trigger expired by event: {}", event);
				die = true;
				lock.notifyAll();
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

	private void waitProcessingDone() {
		// "done" means waiting for another event
		long startTime = System.currentTimeMillis();
		long failAt = startTime + 100;
		while (processing) {
			try {
				long timeLeft = failAt - System.currentTimeMillis();
				if (timeLeft <= 0) {
					throw new SequentialTriggerTimeoutException("Cycle processing time max (100ms) exceeded");
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