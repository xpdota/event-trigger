package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;

import java.util.Map;

@CalloutRepo("Dragonsong's Reprise")
public class Dragonsong implements FilteredEventHandler {

	private final ModifiableCallout<HeadMarkerEvent> firstCleaveMarker = new ModifiableCallout<>("Quad Marker", "Marker ({set} set)");
	private final ModifiableCallout<AbilityCastStart> holiestOfHoly = ModifiableCallout.durationBasedCall("Holiest of Holy", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> emptyDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Donut");
	private final ModifiableCallout<AbilityCastStart> heavensblaze = ModifiableCallout.durationBasedCall("Heavensblaze", "Stack on {event.target}");


	private final XivState state;

	public Dragonsong(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x3C8);
	}

	@HandleEvents
	public void abilityCast(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		final ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x62D4 -> call = holiestOfHoly;
			case 0x62DA -> call = emptyDimension;
			case 0x62DD -> call = heavensblaze;
			default -> {return;}
		}
		context.accept(call.getModified(event));
	}

	private Long firstHeadmark;

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@HandleEvents(order = -50_000)
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		getHeadmarkOffset(event);
		// and also feed this sequential trigger
		fourHeadMark.feed(context, event);
	}

	private final SequentialTrigger<HeadMarkerEvent> fourHeadMark = new SequentialTrigger<>(20_000, HeadMarkerEvent.class,
			e -> getHeadmarkOffset(e) == 0,
			(e1, s) -> {
				// First marker
				if (e1.getTarget().isThePlayer()) {
					s.accept(firstCleaveMarker.getModified(Map.of("set", "first")));
					return;
				}
				// 2-4 markers
				for (int count = 2; count <= 4; count++) {
					HeadMarkerEvent e = s.waitEvent(HeadMarkerEvent.class, event -> getHeadmarkOffset(event) == 0);
					if (e.getTarget().isThePlayer()) {
						s.accept(firstCleaveMarker.getModified(Map.of("set", "first")));
						return;
					}
				}
				// If you weren't one of the first four, you're one of the second four
				s.accept(firstCleaveMarker.getModified(Map.of("set", "second")));
			});

}
