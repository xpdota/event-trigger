package gg.xp.reevent.events;

import gg.xp.reevent.state.QueueState;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;

public class EventMaster {

	private static final Logger log = LoggerFactory.getLogger(EventMaster.class);

	// TODO: make this smarter - don't warn just because we got a big chunk all at once
	private static final int queueSizeInfoThreshold = 25;
	private static final int queueSizeWarningThreshold = 100;
	private static final int queueSizeErrorThreshold = 500;

	private static final ThreadFactory threadFactory = new BasicThreadFactory.Builder()
			.daemon(false)
			.namingPattern("EventPump-%d")
			.build();

	private final EventQueue queue;
	private final EventDistributor eventDistributor;
	private final Thread eventPumpThread = threadFactory.newThread(this::eventLoop);
	private final Thread queueSizeMonitorThread;
	private volatile boolean stop;

	public EventMaster(EventDistributor eventDistributor, EventQueue queue) {
		this.queue = queue;
		this.eventDistributor = eventDistributor;
		eventDistributor.setQueue(queue);
		queueSizeMonitorThread = new Thread(this::monitorQueueSize);
		queueSizeMonitorThread.setName(eventPumpThread.getName() + "-qsm");
		queueSizeMonitorThread.setDaemon(true);
		eventDistributor.getStateStore().putCustom(QueueState.class, new QueueState(queue));
		log.info("Created EventMaster, pump thread is '{}', qsm thread is '{}'", eventPumpThread.getName(), queueSizeMonitorThread.getName());
	}

	public void start() {
		queueSizeMonitorThread.start();
		eventPumpThread.start();
	}

	public EventQueue getQueue() {
		return queue;
	}

	public EventDistributor getDistributor() {
		return eventDistributor;
	}

	public void stop() {
		// TODO: this doesn't wake or interrupt or anything
		stop = true;
	}

	private Runnable drainCallback;

	private void eventLoop() {
		log.info("Starting event loop");
		while (!stop) {
			pumpOneEvent();
		}
		log.info("Finished event loop");
	}

	private void pumpOneEvent() {
		Event event;
		try {
			event = queue.pull();
		}
		catch (Throwable t) {
			log.error("Error pulling event", t);
			return;
		}
		try {
			eventDistributor.acceptEvent(event);
		}
		catch (Throwable t) {
			log.error("Error pumping event {}", event, t);
		}
		Runnable dc = drainCallback;
		if (dc != null && queue.pendingSize() == 0) {
			drainCallback = null;
			try {
				dc.run();
			}
			catch (Throwable t) {
				log.error("Error running callback", t);
			}
		}
	}

	public void pushEvent(Event event) {
		queue.push(event);
	}

	public void pushEventAndWait(Event event) {
		CountDownLatch latch = new CountDownLatch(1);
		drainCallback = latch::countDown;
		queue.push(event);
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
	public void monitorQueueSize() {
		log.info("Queue monitor started. Current size: {}", queue.pendingSize());
		while (true) {
			int queueSize = queue.pendingSize();
			if (queueSize > queueSizeErrorThreshold) {
				log.warn("Queue is huge: {}", queueSize);
			}
			else if (queueSize > queueSizeWarningThreshold) {
				log.warn("Queue is large: {}", queueSize);
			}
			else if (queueSize > queueSizeInfoThreshold) {
				log.info("Queue is sizeable: {}", queueSize);
			}
			else {
				log.trace("Queue is small: {}", queueSize);
			}
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
