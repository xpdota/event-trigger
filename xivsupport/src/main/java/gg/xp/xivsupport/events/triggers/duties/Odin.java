package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.speech.CalloutEvent;

public class Odin implements FilteredEventHandler {

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(394);
	}

	@HandleEvents
	public void valknut(EventContext context, AbilityCastStart event) {
		// TODO: this calls at end of cast....
		if (event.getAbility().getId() == 0xC49) {
			context.accept(new CalloutEvent("Out"));
		}
	}


}
