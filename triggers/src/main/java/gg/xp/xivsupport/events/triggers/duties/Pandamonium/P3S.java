package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;

@CalloutRepo("P3S")
public class P3S implements FilteredEventHandler {

	private final ModifiableCallout scorchedExaltation = new ModifiableCallout("Scorched Exaltation", "Raidwide");
	private final ModifiableCallout heatOfCondemnation = new ModifiableCallout("Heat of Condemnation", "Tank Tethers");

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout call;
			if (id == 0x6706) {
				call = scorchedExaltation;
			}
			else if (id == 0x6700) {
				call = heatOfCondemnation;
			}
			else {
				return;
			}
			context.accept(call.getModified());
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3EF);
	}

}
