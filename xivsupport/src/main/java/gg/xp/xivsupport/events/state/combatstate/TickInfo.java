package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.TickEvent;

import java.time.Duration;
import java.time.Instant;

/**
 * Timing info for DoT/HoT ticks
 */
public class TickInfo {

	private final int intervalMs;
	private final long basis;

	public TickInfo(Event basis, Duration interval) {
		this.basis = basis.getHappenedAt().toEpochMilli();
		intervalMs = (int) interval.toMillis();
	}

	// For unit testing
	TickInfo(long basis, int interval) {
		this.basis = basis;
		intervalMs = interval;
	}

	public Instant getNextTick(Instant now) {
		return now.plusMillis(getMsToNextTick(now));
	}

	public long getMsToNextTick(Instant now) {
		long nowMs = now.toEpochMilli();
		long delta = basis - nowMs;
		long currentIntervalDelta = delta % intervalMs;
		if (currentIntervalDelta < 0) {
			currentIntervalDelta += intervalMs;
		}
		return currentIntervalDelta;
	}

	public Instant getPrevTick(Instant now) {
		return now.minusMillis(getMsFromLastTick(now));
	}

	public long getMsFromLastTick(Instant now) {
		long nowMs = now.toEpochMilli();
		long delta = basis - nowMs;
		long currentIntervalDelta = delta % intervalMs;
		if (currentIntervalDelta > 0) {
			currentIntervalDelta -= intervalMs;
		}
		return -1 * currentIntervalDelta;
	}

	public int getIntervalMs() {
		return intervalMs;
	}
}
