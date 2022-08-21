package gg.xp.reevent.events;

import gg.xp.reevent.time.TimeUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;

public class BasicEventQueue implements EventQueue {


	private static final Logger log = LoggerFactory.getLogger(BasicEventQueue.class);
	private final Deque<Tracker> backingQueue = new ArrayDeque<>();
	private final Object queueLock = new Object();
	private final List<Event> delayedEvents = new ArrayList<>();
	private volatile boolean delayedEventsDirtyFlag;
	private static final ThreadFactory delayedEventProcessorThreadFactory = new BasicThreadFactory.Builder()
			.daemon(true)
			.namingPattern("DelayedEventThread-%d")
			.build();
	private final Thread delayedEventProcessor = delayedEventProcessorThreadFactory.newThread(this::delayedEventProcessingLoop);
	private final Object delayedEventsLock = new Object();

	private class Tracker {
		private final Event event;

		// Future functionality: track queue depth and average latency
		private final long enterTimestamp;
		private final long qdEnter;

		private long exitTimestamp;
		private long qdExit;

		Tracker(Event event) {
			this.event = event;
			enterTimestamp = System.currentTimeMillis();
			qdEnter = pendingSize();
		}

		void markExit() {
			exitTimestamp = System.currentTimeMillis();
			qdExit = pendingSize();
		}
	}

	private static final boolean enableCombine = false;

	@Override
	public void push(Event event) {
		long runAt = event.delayedEnqueueAt();
		if (runAt == 0 || runAt <= System.currentTimeMillis()) {
			event.setEnqueuedAt(TimeUtils.now());
			Tracker tracker = new Tracker(event);
			synchronized (queueLock) {
				if (enableCombine) {
					Tracker existingTracker = backingQueue.peekLast();
					Event combined;
					if (existingTracker == null || (combined = existingTracker.event.combineWith(event)) == null) {
						backingQueue.add(existingTracker);
					}
					else {
//					log.info("Combined!");
						backingQueue.removeLast();
						backingQueue.add(new Tracker(combined));
					}
				}
				else {
					backingQueue.add(tracker);
				}
				queueLock.notifyAll();
			}
		}
		else {
			queueDelayedEvent(event);
		}
	}

	@Override
	public Event pull() {
		synchronized (queueLock) {
			while (true) {
				Tracker tracker = backingQueue.poll();
				if (tracker == null) {
					try {
						queueLock.wait(5000);
					}
					catch (InterruptedException e) {
						// ignored
					}
				}
				else {
					tracker.markExit();
					queueLock.notifyAll();
					return tracker.event;
				}
			}
		}
	}

	@Override
	public int pendingSize() {
		synchronized (queueLock) {
			return backingQueue.size();
		}
	}

	// Should only be used for testing, or maybe hot reloads
	// TODO: problem here is that it waits for queue to be empty, but doesn't wait for current
	// event to be fully processed. This probably needs to be on EventMaster.
	@Override
	public void waitDrain() {
		synchronized (queueLock) {
			while (pendingSize() > 0) {
				try {
					queueLock.wait(1000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void queueDelayedEvent(Event event) {
		long delta = event.delayedEnqueueAt() - System.currentTimeMillis();
		log.info("Queueing delayed event for execution in {}ms: {}", delta, event);
		if (delayedEventProcessor.getState() == Thread.State.NEW) {
			delayedEventProcessor.start();
		}
		else if (delayedEventProcessor.getState() == Thread.State.TERMINATED) {
			throw new RuntimeException("Delayed event processing thread is dead! This event will never be processed.");
		}
		synchronized (delayedEventsLock) {
			delayedEvents.add(event);
			delayedEventsDirtyFlag = true;
			delayedEventsLock.notifyAll();
		}
	}

	private void delayedEventProcessingLoop() {
		while (true) {
			try {
				synchronized (delayedEventsLock) {
					if (delayedEventsDirtyFlag) {
						delayedEvents.sort(Comparator.comparing(Event::delayedEnqueueAt));
					}
					Event current;
					Iterator<Event> iterator = delayedEvents.iterator();
					long currentTime = System.currentTimeMillis();
					while (iterator.hasNext()) {
						current = iterator.next();
						if (current.delayedEnqueueAt() <= currentTime) {
							log.debug("Delayed event {} is ready to go", current);
							push(current);
							iterator.remove();
						}
						else {
							break;
						}
					}
					delayedEventsDirtyFlag = false;
					if (delayedEvents.isEmpty()) {
						delayedEventsLock.wait(10000);
					}
					else {
						long delta = delayedEvents.get(0).delayedEnqueueAt() - currentTime;
						if (delta > 0) {
							delayedEventsLock.wait(delta);
						}
					}
				}
			}
			catch (Throwable t) {
				log.error("Error processing events in delayed event processing thread.", t);
				try {
					//noinspection BusyWait
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}


}
