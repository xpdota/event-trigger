package gg.xp.events;

import java.util.ArrayDeque;
import java.util.Queue;

public class BasicEventQueue implements EventQueue<Event> {

	private final Queue<Tracker> backingQueue = new ArrayDeque<>();
	private final Object queueLock = new Object();

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


	@Override
	public void push(Event event) {
		synchronized (queueLock) {
			backingQueue.add(new Tracker(event));
			queueLock.notifyAll();
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
}
