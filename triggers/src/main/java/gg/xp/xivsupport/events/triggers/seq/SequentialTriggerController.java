package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SequentialTriggerController<X extends Event> {

	private static final Logger log = LoggerFactory.getLogger(SequentialTriggerController.class);
	private static final AtomicInteger threadIdCounter = new AtomicInteger();
	private final Instant expiresAt;
	private final Thread thread;
	private final Object lock = new Object();
	private volatile X currentEvent;
	private volatile EventContext context;
	private volatile boolean done;
	private volatile boolean processing = true;
	private volatile boolean die;

	// To be called from external thread
	public SequentialTriggerController(EventContext initialEventContext, X initialEvent, BiConsumer<X, SequentialTriggerController<X>> triggerCode, int timeout) {
		expiresAt = initialEvent.getHappenedAt().plusMillis(timeout);
		context = initialEventContext;
		thread = new Thread(() -> {
			try {
				triggerCode.accept(initialEvent, this);
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

	// To be called from internal thread
	public <Y extends X> Y waitEvent(Class<Y> eventClass, Predicate<Y> eventFilter) {
		log.info("Waiting for specific event");
		while (true) {
			X event = waitEvent();
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				log.info("Done waiting for specific event, got: {}", event);
				return (Y) event;
			}
		}
	}

	// To be called from internal thread
	private X waitEvent() {
		log.info("Waiting for event");
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
			if (event.getHappenedAt().isAfter(expiresAt)) {
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
				lock.wait(100);
				if (System.currentTimeMillis() > failAt) {
					throw new SequentialTriggerTimeoutException("Cycle processing time max (100ms) exceeded");
				}
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