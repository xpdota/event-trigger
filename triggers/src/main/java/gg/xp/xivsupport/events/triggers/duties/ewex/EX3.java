package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@CalloutRepo("Endsinger Extreme")
public class EX3 implements FilteredEventHandler {

	// TODO: all the complex mechanics

	private final ModifiableCallout<AbilityCastStart> elegeia = ModifiableCallout.durationBasedCall("Elegeia", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> telos = ModifiableCallout.durationBasedCall("Telos", "Big Raidwide");
	// TODO: what is the tell for this?
	private final ModifiableCallout<AbilityCastStart> telomania = ModifiableCallout.durationBasedCall("Telomania", "Raidwides and Bleed");
	private final ModifiableCallout<AbilityCastStart> elenchosSides = ModifiableCallout.durationBasedCall("Elenchos (Sides)", "Sides");
	private final ModifiableCallout<AbilityCastStart> elenchosMiddle = ModifiableCallout.durationBasedCall("Elenchos (Middle)", "Middle");
	private final ModifiableCallout<AbilityCastStart> hubris = ModifiableCallout.durationBasedCall("Hubris", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> eironeia = ModifiableCallout.durationBasedCall("Eironeia", "Healer Stacks");

	private final ModifiableCallout<HasDuration> blueStar = ModifiableCallout.durationBasedCall("Blue Star", "Knockback from {safeSpot}");
	private final ModifiableCallout<HasDuration> redStar = ModifiableCallout.durationBasedCall("Red Star", "{safeSpot} safe");

	private final ModifiableCallout<AbilityUsedEvent> combinedCall = new ModifiableCallout<>("Multiple Star", "{event1.safeSpot} then {event2.safeSpot}", "{event1.safeSpot}, then {event2.safeSpot} ({event2.estimatedRemainingDuration})", Collections.emptyList());

	private final ModifiableCallout<TetherEvent> tetherCall = new ModifiableCallout<>("Tether Break", "Break Tether (with {otherTarget})");

//	private final ModifiableCallout<HeadMarkerEvent> donut = new ModifiableCallout<>("Donut Marker", "Donut");
//	private final ModifiableCallout<HeadMarkerEvent> stack = new ModifiableCallout<>("Stack Marker", "Stack");
//	private final ModifiableCallout<HeadMarkerEvent> flare = new ModifiableCallout<>("Flare Marker", "Flare");
//	private final ModifiableCallout<HeadMarkerEvent> spread = new ModifiableCallout<>("Spread Marker", "Spread");

	private final ModifiableCallout<BuffApplied> donut = ModifiableCallout.durationBasedCall("Donut Marker", "Donut");
	private final ModifiableCallout<BuffApplied> stack = ModifiableCallout.durationBasedCall("Stack Marker", "Stack");
	private final ModifiableCallout<BuffApplied> flare = ModifiableCallout.durationBasedCall("Flare Marker", "Flare");
	private final ModifiableCallout<BuffApplied> spread = ModifiableCallout.durationBasedCall("Spread Marker", "Spread");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	private final XivState state;
	private int headPhase;

	// TODO: timeline
	// TODO: what is 0x7005? Cactbot doesn't mention it, but it certainly seems to be a star collision

	public EX3(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x3e6);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		ModifiableCallout<AbilityCastStart> call;
		if (event.getSource().getType() == CombatantType.NPC) {
			int id = (int) event.getAbility().getId();
			call = switch (id) {
				case 0x6FF6 -> elegeia;
				case 0x702E -> telos;
				case 0x7020 -> elenchosMiddle;
				case 0x7022 -> elenchosSides;
				case 0x702C -> hubris;
				case 0x702F, 0x7030 -> eironeia;
				default -> null;
			};
			if (call != null) {
				context.accept(call.getModified(event));
			}
		}
	}

	@HandleEvents
	public void newPull(EventContext context, PullStartedEvent pse) {
		starEvents.clear();
		rewindPlayerBuffs.clear();
		headPhase = 0;
		lastStarEvent = null;
	}


	// Because the position is not necessarily correct the first time, we need to wait until we have a proper position
	// for the star.
	private final List<StarData> starEvents = new ArrayList<>();
	private @Nullable StarData lastStarEvent;

	// Rewind debuff collection
	private final List<BuffApplied> rewindPlayerBuffs = new ArrayList<>();

	@HandleEvents
	public void starCollisions(EventContext context, AbilityCastStart event) {
		ModifiableCallout<HasDuration> call;
		if (event.getSource().getType() == CombatantType.NPC || event.getSource().getbNpcId() == 9020) {
			int id = (int) event.getAbility().getId();
			call = switch (id) {
				case 0x6FFA -> redStar;
				case 0x6FFB -> blueStar;
				default -> null;
			};
			if (call != null) {
				starEvents.add(new StarData(event));
				recheckStar(context);
			}
		}
	}

	@HandleEvents
	public void cbtUpdate(EventContext context, XivStateRecalculatedEvent event) {
		recheckStar(context);
	}

	// This is to handle the fact that we might not have position data for a star at the time when we get the call
	private void recheckStar(EventContext context) {
		if (starEvents.isEmpty()) {
			return;
		}
		StarData event = starEvents.get(0);
		ArenaSector position = event.getSector();
		if (position == ArenaSector.UNKNOWN) {
			return;
		}
		context.accept(event.getCallout());
		starEvents.remove(0);
		recheckStar(context);
	}

	private List<StarData> pendingMultiStar = new ArrayList<>();

	@HandleEvents
	public void multiStar(EventContext context, AbilityUsedEvent event) {
		int id = (int) event.getAbility().getId();
		double delay;
		switch (id) {
			case 0x6FFE, 0x7000 -> delay = 0;
			case 0x6FFF, 0x7001 -> delay = 6.5;
			default -> {
				return;
			}
		}
		if (lastStarEvent != null && lastStarEvent.getEffectiveTimeSince().getSeconds() > 20) {
			// TODO: logic might not be right
//			delay += 2;
		}
		StarData newStarData = new StarData(event, Duration.ofMillis((long) (delay * 1000)));
		lastStarEvent = newStarData;
		pendingMultiStar.add(newStarData);
		if (pendingMultiStar.size() == 2) {
			pendingMultiStar.sort(Comparator.comparing(data -> (int) data.getEstimatedRemainingDuration().toMillis()));
			CalloutEvent call = combinedCall.getModified(event, Map.of("event1", pendingMultiStar.get(0), "event2", pendingMultiStar.get(1)));
			context.accept(call);
			pendingMultiStar.clear();
		}
	}

	@HandleEvents
	public void tether(EventContext context, TetherEvent event) {
		if (event.getId() == 0xA3) {
			XivCombatant thePlayer = event.getTargetMatching(XivCombatant::isThePlayer);
			if (thePlayer != null) {
				XivCombatant otherTarget = event.getTargetMatching(cbt -> !cbt.isThePlayer());
				context.accept(tetherCall.getModified(event, Map.of("otherTarget", otherTarget == null ? "Error" : otherTarget)));
			}
		}
	}

//	private Long firstHeadmark;
//
//	private int getHeadmarkOffset(HeadMarkerEvent event) {
//		if (firstHeadmark == null) {
//			firstHeadmark = event.getMarkerId();
//		}
//		return (int) (event.getMarkerId() - firstHeadmark);
//	}
//

	@HandleEvents
	public void buffs(EventContext context, BuffApplied event) {
		// This is done unconditionally to create the headmarker offset
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}

		// Headmarkers:
		/*
			326 (0) - Tether precursor
			344 (+18) - Tank buster
			161 (-165) - Healer Stacks

			328 (+2) - Something? All 8 people got it.

			318 (-8) - Stack?
			322 (-4) - Donut?
			327 (+1) - Flare?
			328 (+2) - Spread?

			325 (-1) - ?
			221 (-105) - ?
			323 (-3) - ?
			324 (-2) - ?

		 */
		int buffId = (int) event.getBuff().getId();

		ModifiableCallout<BuffApplied> call =
				switch (buffId) {
					case 0xBAD -> donut;
					case 0xBAE -> spread;
					case 0xBAF -> flare;
					case 0xBB0 -> stack;
					default -> null;
				};
		if (call != null) {
			rewindPlayerBuffs.add(event);
			context.accept(call.getModified(event));
		}
	}

	private enum StarType {
		AoE,
		Knockback
	}

	private final class StarData implements HasDuration {
		private final HasAbility event;
		private final long combatantId;
		private final StarType startType;
		private ArenaSector sector;
		private HasDuration durationDelegate;

		private <X extends HasAbility & HasSourceEntity> StarData(X event, HasDuration durationDelegate) {
			this.event = event;
			this.durationDelegate = durationDelegate;
			combatantId = event.getSource().getId();
			int id = (int) event.getAbility().getId();
			startType = switch (id) {
				case 0x6FF8, 0x6FFA, 0x6FFE, 0x6FFF, 0x7003 -> StarType.AoE;
				case 0x6FF9, 0x6FFB, 0x7000, 0x7001, 0x7005 -> StarType.Knockback;
				default -> throw new IllegalArgumentException("Not a valid star ability: " + event.getAbility());
			};
		}

		StarData(AbilityCastStart event) {
			this(event, event);
		}

		StarData(AbilityUsedEvent event, Duration duration) {
			this(event, new HasDuration() {
				@Override
				public Duration getInitialDuration() {
					return duration;
				}

				@Override
				public Duration getEffectiveTimeSince() {
					return event.getEffectiveTimeSince();
				}
			});

		}

		public ArenaSector getSector() {
			if (sector != null) {
				return sector;
			}
			XivCombatant newCbtData = state.getCombatants().get(combatantId);
			ArenaSector newSector;
			newSector = arenaPos.forCombatant(newCbtData);
			if (newSector == ArenaSector.CENTER || newSector == ArenaSector.UNKNOWN) {
				newSector = ArenaPos.combatantFacing(newCbtData);
			}
			if (newSector == ArenaSector.CENTER || newSector == ArenaSector.UNKNOWN) {
				return ArenaSector.UNKNOWN;
			}
			else {
				return sector = newSector;
			}
		}

		public ArenaSector getSafeSpot() {
			ArenaSector starSector = getSector();
			if (starSector == ArenaSector.UNKNOWN) {
				return ArenaSector.UNKNOWN;
			}
			else {
				if (startType == StarType.AoE) {
					return starSector.opposite();
				}
				else {
					return starSector;
				}
			}
		}


		public CalloutEvent getCallout() {
			Map<String, Object> extraArgs = Map.of("starDir", getSector(), "safeSpot", getSafeSpot());
			if (startType == StarType.AoE) {
				return redStar.getModified(durationDelegate, extraArgs);
			}
			else {
				return blueStar.getModified(durationDelegate, extraArgs);
			}
		}

		@Override
		public Duration getInitialDuration() {
			return durationDelegate.getInitialDuration();
		}

		@Override
		public Duration getEffectiveTimeSince() {
			return durationDelegate.getEffectiveTimeSince();
		}
	}

}
