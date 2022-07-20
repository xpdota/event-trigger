package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.XivEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "The Weapon's Refrain", duty = KnownDuty.UWU)
public class Uwu implements FilteredEventHandler {

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivStateImpl.class).zoneIs(0x309L);
	}


	private final List<XivEntity> mistralTargets = new ArrayList<>();

	@HandleEvents
	public void reset(EventContext context, PullStartedEvent pull) {
		mistralTargets.clear();
	}

	private final ModifiableCallout<AbilityCastStart> slipStream = ModifiableCallout.durationBasedCall("Slipstream (Cleave)", "Slipstream");

	@HandleEvents
	public void slipStream(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x2B53) {
			context.accept(slipStream.getModified(event));
		}
	}

	// TODO: since these come in pairs, might be a good idea to finally do instance replacement
	private final ModifiableCallout<HeadMarkerEvent> mistral = new ModifiableCallout<>("Mistral (Headmark)", "Mistral");

	@HandleEvents
	public void mistral(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x10) {
			mistralTargets.add(event.getTarget());
		}
		if (mistralTargets.size() >= 2) {
			context.accept(mistral.getModified(event, Map.of("target1", mistralTargets.get(0), "target2", mistralTargets.get(1))));
			mistralTargets.clear();
		}
	}

	// TODO: we will need better combatant add/remove support


	private final ModifiableCallout<AbilityCastStart> searingWind = ModifiableCallout.durationBasedCall("Searing Wind", "Searing Wind on {target}");
	// TODO: display debuff duration after

	@HandleEvents
	public void searingWind(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x2B5B) {
			context.accept(searingWind.getModified(event, Map.of("target", event.getTarget())));
		}
	}

	// TODO: this shouldn't call out Titan
//	private final ModifiableCallout woken = new ModifiableCallout("Woken", "{target} awoken");
//	@HandleEvents
//	public void searingWind(EventContext context, BuffApplied event) {
//		if (event.getBuff().getId() == 0x5F9) {
//			context.accept(woken.getModified(Map.of("target", event.getTarget())));
//		}
//	}


}
