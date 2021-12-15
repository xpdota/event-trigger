package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.models.XivEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CalloutRepo("The Weapon's Refrain")
public class Uwu implements FilteredEventHandler {

	@Override
	public boolean enabled(EventContext context) {
		return true;
	}

	private final ModifiableCallout slipStream = new ModifiableCallout("Slipstream (Cleave)", "Slipstream");

	@HandleEvents
	public void slipStream(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x2B53) {
			context.accept(slipStream.getModified());
		}
	}

	// TODO: since these come in pairs, might be a good idea to finally do instance replacement
	private final ModifiableCallout mistral = new ModifiableCallout("Mistral (Headmark)", "Mistral");
	private final List<XivEntity> mistralTargets = new ArrayList<>();

	@HandleEvents
	public void mistral(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x10) {
			mistralTargets.add(event.getTarget());
		}
		if (mistralTargets.size() >= 2) {
			context.accept(mistral.getModified(Map.of("target1", mistralTargets.get(0), "target2", mistralTargets.get(1))));
			mistralTargets.clear();
		}
	}

	// TODO: we will need better combatant add/remove support


	private final ModifiableCallout searingWind = new ModifiableCallout("Searing Wind", "Searing Wind on {target}");

	@HandleEvents
	public void searingWind(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x2B5B) {
			context.accept(searingWind.getModified(Map.of("target", event.getTarget())));
		}
	}

}
