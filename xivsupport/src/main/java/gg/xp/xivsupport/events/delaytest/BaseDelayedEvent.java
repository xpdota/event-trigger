package gg.xp.xivsupport.events.delaytest;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public abstract class BaseDelayedEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 4516450176008003145L;
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
