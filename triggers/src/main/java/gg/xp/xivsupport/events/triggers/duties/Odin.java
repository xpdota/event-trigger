package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;

@CalloutRepo("Urth's Fount (Odin)")
public class Odin implements FilteredEventHandler {

	private final ModifiableCallout valknut = new ModifiableCallout("Valknut (Out)", "Out");
	private final ModifiableCallout dummy = new ModifiableCallout("Dummy Callout to Test UI", "Out");

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(394);
	}

	@HandleEvents(name = "Valknut (Out)")
	public void valknut(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0xC49) {
			context.accept(valknut.getModified());
		}
	}


}
