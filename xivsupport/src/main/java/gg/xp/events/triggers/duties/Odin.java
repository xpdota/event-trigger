package gg.xp.events.triggers.duties;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.events.AbilityCastStart;
import gg.xp.events.state.XivState;
import gg.xp.scan.FilteredEventHandler;
import gg.xp.scan.HandleEvents;
import gg.xp.speech.TtsCall;

public class Odin implements FilteredEventHandler {

	@Override
	public boolean enabled(EventContext<Event> context) {
		return context.getStateInfo().get(XivState.class).zoneIs(394);
	}

	@HandleEvents
	public void valknut(EventContext<Event> context, AbilityCastStart event) {
		// TODO: this calls at end of cast....
		if (event.getAbility().getId() == 0xC49) {
			context.accept(new TtsCall("Out"));
		}
	}


}
