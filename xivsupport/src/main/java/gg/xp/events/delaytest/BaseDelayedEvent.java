package gg.xp.events.delaytest;

import gg.xp.events.BaseEvent;

public abstract class BaseDelayedEvent extends BaseEvent {

	private final long runAt;

	protected BaseDelayedEvent(long delayMs) {
		this.runAt = System.currentTimeMillis() + delayMs;
	}

	@Override
	public long delayedEnqueueAt() {
		return this.runAt;
	}

	// true by default, but client can override
	@Override
	public boolean delayedEnqueueAtFront() {
		return true;
	}
}
