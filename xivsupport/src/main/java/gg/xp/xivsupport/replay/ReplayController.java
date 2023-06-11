package gg.xp.xivsupport.replay;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.gui.imprt.EventIterator;
import gg.xp.xivsupport.gui.imprt.EventsCount;
import gg.xp.xivsupport.gui.imprt.MoreEventsType;
import gg.xp.xivsupport.persistence.Compressible;
import gg.xp.xivsupport.sys.Threading;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ReplayController {

	private static final Logger log = LoggerFactory.getLogger(ReplayController.class);

	private final ExecutorService exs = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder()
			.daemon(true)
			.namingPattern("ReplayThread-%d")
			.priority(Thread.MIN_PRIORITY)
			.build()
	);

	private final EventMaster master;
	private final List<Runnable> callbacks = new ArrayList<>();
	private final boolean decompress;
	private final EventIterator<? extends Event> eventIter;
	private static final int MIN_CHUNK = 8192;
	private static final int MAX_CHUNK = 65536;
	private final Queue<Event> evQueue = new ArrayDeque<>(MAX_CHUNK * 2);
	private static final ExecutorService feeder = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("ReplayFeed"));
	private final Object queueLock = new Object();
	private int currentIndex;
	private int currentFeed;
	private volatile boolean stop;


	public ReplayController(EventMaster master, EventIterator<? extends Event> events, boolean decompress) {
//		master.setUseLoopLock(true);
		this.master = master;
		this.eventIter = events;
		this.decompress = decompress;
		feeder.submit(this::feedLoop);
	}

	public EventsCount getCounts() {
		return new EventsCount(currentIndex, currentFeed, interatorHasMore() ? MoreEventsType.AT_LEAST : MoreEventsType.EXACTLY);
	}

	public void addCallback(Runnable r) {
		callbacks.add(r);
	}

	private void notifyCallbacks() {
		try {
			callbacks.forEach(Runnable::run);
		}
		catch (Throwable t) {
			log.error("Error running callbacks", t);
		}
	}


	/**
	 * Advance by a certain number of events. While this is synchronous in the sense that it will not
	 * return until the events have been enqueued, it is not asynchronous in the sense that events are
	 * merely enqueued rather than processed immediately.
	 *
	 * @param count Count to advance by
	 * @return Actual count advanced by, which will be less than 'count' if there were less than 'count'
	 * events remaining in the replay.
	 */
	public int advanceBy(int count) {
		int advancedBy = 0;
		while (count-- > 0 && hasMoreEvents()) {
			evQueue.poll();
			Event event = getNext();
			if (event == null) {
				break;
			}
			if (decompress && event instanceof Compressible compressedEvent) {
				compressedEvent.decompress();
			}
			preProcessEvent(event);
			// TODO: this fixes a bug (see LaunchImportedSession) but may be slightly worse on performance
			master.pushEventAndWait(event);
			advancedBy++;
		}
		notifyCallbacks();
		return advancedBy;
	}

	protected void preProcessEvent(Event event) {

	}

	public void advanceByAsync(int count) {
		exs.submit(() -> {
			advanceBy(count);
			notifyCallbacks();
		});
	}

	public Future<?> advanceByAsyncWhile(Supplier<Boolean> advWhile) {
		stop = true;
		return exs.submit(() -> {
			stop = false;
			while (!stop && advWhile.get() && hasMoreEvents()) {
				Event event = getNext();
				if (decompress && event instanceof Compressible compressedEvent) {
					compressedEvent.decompress();
				}
				preProcessEvent(event);
				master.pushEventAndWait(event);
			}
			notifyCallbacks();
		});
	}

	private volatile Event prevGet;
	private Event getNext() {
		Event next;
		synchronized (queueLock) {
			while ((next = evQueue.poll()) == null) {
				if (!hasMoreEvents()) {
					log.error("No more events!");
					return null;
				}
				else {
					feedQueueNow();
				}
			}
		}
		currentIndex++;
		if (prevGet instanceof ACTLogLineEvent prevAct && next instanceof ACTLogLineEvent curAct) {
			if (curAct.getLineNum() != prevAct.getLineNum() + 1) {
				log.error("Bad line num!");
			}
		}
		prevGet = next;
		return next;
	}

	/**
	 * Wake up the feedloop immediately
	 */
	private void feedQueueNow() {
		synchronized (queueLock) {
			queueLock.notifyAll();
//			// TODO: need to also factor in MAX_CHUNK
//			log.warn("Sync event feed");
//			while (evQueue.size() < MIN_CHUNK && eventIter.hasMore()) {
//				feedOne();
//			}
		}
	}

	private void feedLoop() {
		try {
			while (eventIter.hasMore()) {
				synchronized (queueLock) {
					while (evQueue.size() < MAX_CHUNK && eventIter.hasMore()) {
						feedOne();
					}
					try {
						queueLock.wait(2_000);
					}
					catch (InterruptedException e) {
						log.error("Interrupted", e);
					}
				}
			}
			log.info("Finished feeding events");
		} catch (Throwable t) {
			log.error("Event replay feed thread crashed!", t);
		}
	}

	private volatile Event prevFeed;

	// Must be called while holding queueLock
	private void feedOne() {
		Event next = eventIter.getNext();
		currentFeed++;
		boolean success = evQueue.offer(next);
		if (!success) {
			log.error("Event feed failure!");
		}
		if (prevFeed instanceof ACTLogLineEvent prevAct && next instanceof ACTLogLineEvent curAct) {
			if (curAct.getLineNum() != prevAct.getLineNum() + 1) {
				log.error("Bad line num!");
			}
		}
		prevFeed = next;
	}

	private boolean interatorHasMore() {
		synchronized (queueLock) {
			return eventIter.hasMore();
		}
	}

	public boolean hasMoreEvents() {
		return (!evQueue.isEmpty()) || interatorHasMore();
	}
}