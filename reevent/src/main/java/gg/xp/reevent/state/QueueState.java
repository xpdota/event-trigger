package gg.xp.reevent.state;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.events.EventQueue;

public class QueueState implements SubState {

	private final EventQueue eventQueue;

	public QueueState(EventQueue eventQueue) {
		this.eventQueue = eventQueue;
	}

	public int getQueueDepth() {
		return eventQueue.pendingSize();
	}

}
