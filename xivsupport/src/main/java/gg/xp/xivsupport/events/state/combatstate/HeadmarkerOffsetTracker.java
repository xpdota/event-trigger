package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;

public class HeadmarkerOffsetTracker {

	private Long firstId;

	@HandleEvents(order = -1_000_000)
	public void processMarker(EventContext context, HeadMarkerEvent event) {
		if (firstId == null) {
			firstId = event.getMarkerId();
		}
		else {
			event.setPullOffset((int) (event.getMarkerId() - firstId));
		}
	}

	@HandleEvents
	public void reset(EventContext context, PullStartedEvent event) {
		reset();
	}

	@HandleEvents
	public void reset(EventContext context, ZoneChangeEvent event) {
		reset();
	}

	@HandleEvents
	public void reset(EventContext context, DutyCommenceEvent event) {
		reset();
	}

	public void reset() {
		firstId = null;
	}

}
