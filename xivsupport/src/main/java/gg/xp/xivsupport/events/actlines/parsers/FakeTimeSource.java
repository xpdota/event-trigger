package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.CurrentTimeSource;

import java.time.Instant;

public class FakeTimeSource implements CurrentTimeSource {

	private volatile Instant currentTime = Instant.EPOCH;

	@Override
	public Instant now() {
		return currentTime;
	}

	public void setNewTime(Instant time) {
		currentTime = time;
	}
}
