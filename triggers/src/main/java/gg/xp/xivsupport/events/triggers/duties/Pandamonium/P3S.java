package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;

@CalloutRepo("P3S")
public class P3S implements FilteredEventHandler {

	private final ModifiableCallout scorchedExaltation = new ModifiableCallout("Scorched Exaltation", "Raidwide");
	private final ModifiableCallout heatOfCondemnation = new ModifiableCallout("Heat of Condemnation", "Tank Tethers");

	private final ModifiableCallout number1 = new ModifiableCallout("#1", "1");
	private final ModifiableCallout number2 = new ModifiableCallout("#2", "2");
	private final ModifiableCallout number3 = new ModifiableCallout("#3", "3");
	private final ModifiableCallout number4 = new ModifiableCallout("#4", "4");
	private final ModifiableCallout number5 = new ModifiableCallout("#5", "5");
	private final ModifiableCallout number6 = new ModifiableCallout("#6", "6");
	private final ModifiableCallout number7 = new ModifiableCallout("#7", "7");
	private final ModifiableCallout number8 = new ModifiableCallout("#8", "8");

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

	private Long firstHeadmark;

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		firstHeadmark = null;
	}

	@HandleEvents
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		ModifiableCallout call = switch (headmarkOffset) {
			case 0: yield number1;
			case 1: yield number2;
			case 2: yield number3;
			case 3: yield number4;
			case 4: yield number5;
			case 5: yield number6;
			case 6: yield number7;
			case 7: yield number8;
			default: yield null;
		};
		if (call != null) {
			context.accept(call.getModified());
		}
	}

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3EF);
	}

}
