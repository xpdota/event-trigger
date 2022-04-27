package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CalloutRepo("Dragonsong's Reprise")
public class Dragonsong implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(Dragonsong.class);

	private final ModifiableCallout<HeadMarkerEvent> p1_firstCleaveMarker = new ModifiableCallout<>("Quad Marker", "Marker ({set} set)");
	private final ModifiableCallout<AbilityCastStart> p1_holiestOfHoly = ModifiableCallout.durationBasedCall("Holiest of Holy", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> p1_emptyDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Donut");
	private final ModifiableCallout<AbilityCastStart> p1_fullDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Out");
	private final ModifiableCallout<AbilityCastStart> p1_heavensblaze = ModifiableCallout.durationBasedCall("Heavensblaze", "Stack on {event.target}");
	private final ModifiableCallout<AbilityCastStart> p1_holiestHallowing = ModifiableCallout.durationBasedCall("Holiest Hallowing", "Interrupt {event.source}");
	private final ModifiableCallout<BuffApplied> p1_brightwing = ModifiableCallout.durationBasedCall("Brightwing", "Pair Cleaves");

	private final ModifiableCallout<HeadMarkerEvent> p1_playstationMarkers = new ModifiableCallout<>("PS Markers", "{marker} marker with {partner}");

	private final ModifiableCallout<TetherEvent> p1_genericTether = new ModifiableCallout<>("P1 Generic Tethers", "Tether on you", "Tether on you {event.id}", Collections.emptyList());

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
			case 0x62D4 -> call = p1_holiestOfHoly;
			case 0x62DA -> call = p1_emptyDimension;
			case 0x62DB -> call = p1_fullDimension;
			case 0x62DD -> call = p1_heavensblaze;
			case 0x62D0 -> {
				//noinspection ConstantConditions
				if (state.getPlayerJob().caresAboutInterrupt()) {
					call = p1_holiestHallowing;
				}
				else {
					return;
				}
			}
			// TODO: what should this call actually be?
//			case 0x62D6 -> call = p1_hyper;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@HandleEvents
	public void buffApplied(EventContext context, BuffApplied event) {
		// Brightwing
		if (event.getBuff().getId() == 0x6316) {
			context.accept(p1_brightwing.getModified(event));
		}
	}

	private Long firstHeadmark;

	@HandleEvents
	public void reset(EventContext context, PullStartedEvent event) {
		firstHeadmark = null;
	}

	// TODO: need a secondary reset trigger for final boss

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@HandleEvents(order = -50_000)
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		getHeadmarkOffset(event);
	}


	@HandleEvents
	public void p1_genericTether(EventContext context, TetherEvent event) {
		long id = event.getId();
		if (event.eitherTargetMatches(XivCombatant::isThePlayer)
				&& (id == 0x54 || id == 0x1)) {
			context.accept(p1_genericTether.getModified(event));
		}
	}

	@HandleEvents
	public void feedSeq(EventContext context, BaseEvent event) {
		p1_fourHeadMark.feed(context, event);
		p1_pairsOfMarkers.feed(context, event);
	}

	private enum P1PsMarker {
		Circle,
		Triangle,
		Square,
		Cross;

		static @Nullable P1PsMarker forOffset(int offset) {
			if (offset >= 47 && offset <= 50) {
				return values()[offset - 47];
			}
			else {
				return null;
			}
		}
	}

	private final SequentialTrigger<BaseEvent> p1_fourHeadMark = new SequentialTrigger<>(30_000, BaseEvent.class,
			e -> (e instanceof AbilityCastStart acs) && acs.getAbility().getId() == 0x62DD,
			(e1, s) -> {
				// 2-4 markers
				String set;
				// If you aren't one of the first 4, you're one of the second four
				if (s.waitEvents(4, HeadMarkerEvent.class, event -> getHeadmarkOffset(event) == 0)
						.stream().anyMatch(e -> e.getTarget().isThePlayer())) {
					set = "first";
				}
				else {
					set = "second";
				}
				s.accept(p1_firstCleaveMarker.getModified(Map.of("set", set)));
			});

	private final SequentialTrigger<BaseEvent> p1_pairsOfMarkers = new SequentialTrigger<>(20_000, BaseEvent.class,
			e -> e instanceof AbilityUsedEvent acs && acs.getAbility().getId() == 0x62D5,
			(e1, s) -> {
				List<HeadMarkerEvent> marks = s.waitEventsUntil(8, HeadMarkerEvent.class, e -> {
					int headmarkOffset = getHeadmarkOffset(e);
					return headmarkOffset >= 47 && headmarkOffset <= 50;
				}, AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x62DE);
				marks.stream().filter(e -> e.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(myMark -> {
							Optional<HeadMarkerEvent> partnerMarker = marks.stream().filter(e -> !e.getTarget().isThePlayer() && e.getMarkerId() == myMark.getMarkerId())
									.findAny();
							int adjustedId = getHeadmarkOffset(myMark);
							P1PsMarker marker = P1PsMarker.forOffset(adjustedId);
							XivCombatant partner = partnerMarker.map(HeadMarkerEvent::getTarget).orElse(null);
							//noinspection ConstantConditions
							s.accept(p1_playstationMarkers.getModified(Map.of(
									"marker", marker,
									"partner", partner == null ? "nobody" : partner)));
						}, () -> log.error("No personal headmarker! Collected: [{}]", marks));
			}
	);

}
