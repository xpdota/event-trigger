package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.time.CurrentTimeSource;
import gg.xp.xivsupport.events.ACTLogLineEvent;

import java.time.Instant;

public class ActTimeKeeper implements CurrentTimeSource {

	private Instant time = Instant.EPOCH;

	@HandleEvents(order = Integer.MAX_VALUE)
	public void handleEvents(EventContext context, ACTLogLineEvent line) {
		time = line.getHappenedAt();
		line.setTimeSource(this);
	}

	@Override
	public Instant now() {
		return time;
	}
}
