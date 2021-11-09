package gg.xp.events.state;

import gg.xp.context.SubState;
import gg.xp.events.EventQueue;

public class QueueState implements SubState {

	private final EventQueue eventQueue;

	public QueueState(EventQueue eventQueue) {
		this.eventQueue = eventQueue;
	}

	public int getQueueDepth() {
		return eventQueue.pendingSize();
	}

}
