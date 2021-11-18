package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeInEvent;

public class WipeDetector {

	@HandleEvents
	public void wipe(EventContext context, FadeInEvent event) {
		context.accept(new WipeEvent());
	}

}
