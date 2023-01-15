package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with the offset headmarkers in newer content.
 */
public class HeadmarkerOffsetTracker {

	private static final Logger log = LoggerFactory.getLogger(HeadmarkerOffsetTracker.class);

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
		resetOffset();
	}

	@HandleEvents
	public void reset(EventContext context, ZoneChangeEvent event) {
		resetOffset();
	}

	@HandleEvents
	public void reset(EventContext context, DutyCommenceEvent event) {
		resetOffset();
	}

	/**
	 * Manually reset the tracking. This is global, so use sparingly. This would
	 * mostly be used for phase transitions.
	 */
	public void reset() {
		log.info("Headmarker offset tracking manually reset");
		resetOffset();
	}

	private void resetOffset() {
		firstId = null;
	}

}
