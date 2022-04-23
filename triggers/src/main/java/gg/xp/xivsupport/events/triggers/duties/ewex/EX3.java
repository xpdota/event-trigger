package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.BaseEvent;
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
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@CalloutRepo("Endsinger Extreme")
public class EX3 implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(EX3.class);

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

	private final ModifiableCallout<AbilityUsedEvent> combinedCall = new ModifiableCallout<>("Multiple Star", "{event1.safeSpot} then {event2.safeSpot}", "{event1.safeSpot}, then {event2.safeSpot} ({event2.estimatedRemainingDuration})", ModifiableCallout.expiresIn(Duration.ofSeconds(8)));

	private final ModifiableCallout<TetherEvent> tetherCall = new ModifiableCallout<>("Tether Break", "Break Tether (with {otherTarget})");

//	private final ModifiableCallout<HeadMarkerEvent> donut = new ModifiableCallout<>("Donut Marker", "Donut");
//	private final ModifiableCallout<HeadMarkerEvent> stack = new ModifiableCallout<>("Stack Marker", "Stack");
//	private final ModifiableCallout<HeadMarkerEvent> flare = new ModifiableCallout<>("Flare Marker", "Flare");
//	private final ModifiableCallout<HeadMarkerEvent> spread = new ModifiableCallout<>("Spread Marker", "Spread");

	private final ModifiableCallout<HasDuration> donut = ModifiableCallout.durationBasedCall("Donut Marker", "Donut");
	private final ModifiableCallout<HasDuration> stack = ModifiableCallout.durationBasedCall("Stack Marker", "Stack");
	private final ModifiableCallout<HasDuration> flare = ModifiableCallout.durationBasedCall("Flare Marker", "Flare");
	private final ModifiableCallout<HasDuration> spread = ModifiableCallout.durationBasedCall("Spread Marker", "Spread");

	private final ModifiableCallout<AbilityUsedEvent> fiveHeadSafeSpots = new ModifiableCallout<>("5Head Safe Spot (First 3)", "{safeSpot1}, {safeSpot2}, {safeSpot3}");
	private final ModifiableCallout<AbilityUsedEvent> fiveHeadSafeSpotFinal = new ModifiableCallout<>("5Head Safe Spot (Final)", "{safeSpot}");
	private final ModifiableCallout<AbilityUsedEvent> fiveHeadSafeSpotError = new ModifiableCallout<>("5Head Safe Spot (Error)", "Error!");

	private final ModifiableCallout<?> sixHeadSafeSpots = new ModifiableCallout<>("6Head Safe Spots", "{safeSpot1} and {safeSpot2} safe");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 6.5, 6.5);

	private final XivState state;

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
		if (buffId == 0x95D) {
			int recallIndex = (int) (0x17E - event.getRawStacks());
			BuffApplied buff = rewindPlayerBuffs.get(recallIndex);
			ModifiableCallout<HasDuration> call = callForBuffId((int) buff.getBuff().getId());
			BuffApplied fakeEvent = new BuffApplied(buff.getBuff(), 12.0, buff.getSource(), buff.getTarget(), 0);
			fakeEvent.setParent(buff);
			fakeEvent.setHappenedAt(event.getEffectiveHappenedAt());
			//noinspection ConstantConditions
			context.accept(call.getModified(fakeEvent));
		}
		else if (rewindPlayerBuffs.size() < 3) {
			ModifiableCallout<HasDuration> call = callForBuffId(buffId);
			if (call != null) {
				rewindPlayerBuffs.add(event);
				context.accept(call.getModified(event));
			}
		}
	}

	@Nullable
	private ModifiableCallout<HasDuration> callForBuffId(int buffId) {
		return switch (buffId) {
			case 0xBAD -> donut;
			case 0xBAE -> spread;
			case 0xBAF -> flare;
			case 0xBB0 -> stack;
			default -> null;
		};
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
			XivCombatant newCbtData = state.getCombatant(combatantId);
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

	private enum FiveHeadMech {
		DONUT,
		AOE,
		CLEAVE;

		static @Nullable FiveHeadMech forAbility(XivAbility ability) {
			return switch ((int) ability.getId()) {
				case 0x6FFC, 0x7006 -> CLEAVE;
				case 0x7009 -> AOE;
				case 0x700A -> DONUT;
				default -> null;
			};
		}
	}

	private static final Set<Long> headAbilities = Set.of(0x6FFCL, 0x7006L, 0x7009L, 0x700AL);

	private final SequentialTrigger<BaseEvent> fiveHead = new SequentialTrigger<>(60_000, BaseEvent.class, (e) -> e instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x7007, (e1, s) -> {

		// Map of head to list of what it did on each set
		Map<XivCombatant, List<FiveHeadMech>> collectedMechanics = new HashMap<>();

		// Three sets of mechanics
		outer:
		for (int j = 0; j < 3; j++) {
			ArenaSector cleaveDir = null;
			// Collect 5 head casts (includes cleave)
			for (int i = 0; i < 5; i++) {
				AbilityCastStart headCast = s.waitEvent(AbilityCastStart.class, acs -> headAbilities.contains(acs.getAbility().getId()));
				FiveHeadMech mech = FiveHeadMech.forAbility(headCast.getAbility());
				collectedMechanics.computeIfAbsent(headCast.getSource(), k -> new ArrayList<>()).add(mech);
				if (mech == FiveHeadMech.CLEAVE) {
					cleaveDir = ArenaPos.combatantFacing(headCast.getSource());
				}
			}
			if (cleaveDir == null) {
				log.error("No safespot! {}", collectedMechanics);
				s.accept(fiveHeadSafeSpotError.getModified());
				continue;
			}
			ArenaSector safeDir = cleaveDir.opposite();
			for (var entry : collectedMechanics.entrySet()) {
				if (entry.getValue().get(j) != FiveHeadMech.DONUT) {
					continue;
				}
				ArenaSector pos = arenaPos.forCombatant(entry.getKey());
				if (pos.isStrictlyAdjacentTo(safeDir)) {
					if (j == 0) {
						s.accept(fiveHeadSafeSpots.getModified(Map.of("safeSpot1", pos, "safeSpot2", pos.plusQuads(1), "safeSpot3", pos.plusQuads(2))));
					}
					continue outer;
				}
			}
			log.error("No donut!");
			s.accept(fiveHeadSafeSpotError.getModified());
		}
		Map<XivCombatant, FiveHeadMech> rewoundMechs = new HashMap<>();
		ArenaSector cleaveDir = null;
		ArenaSector safeDir;
		for (int i = 0; i < 5; i++) {
			BuffApplied rewindBuff = s.waitEvent(BuffApplied.class, ba -> ba.getBuff().getId() == 0x808);
			int mechanicIndex = (int) (0x17A - rewindBuff.getRawStacks());
			XivCombatant cbt = rewindBuff.getTarget();
			FiveHeadMech mech = collectedMechanics.get(cbt).get(mechanicIndex);
			rewoundMechs.put(cbt, mech);
			if (mech == FiveHeadMech.CLEAVE) {
				cleaveDir = ArenaPos.combatantFacing(cbt).plusQuads(-2 + mechanicIndex);
			}
		}
		if (cleaveDir == null) {
			log.error("No safespot! {}", rewoundMechs);
			s.accept(fiveHeadSafeSpotError.getModified());
			return;
		}
		else {
			safeDir = cleaveDir.opposite();
		}
		for (var entry : rewoundMechs.entrySet()) {
			if (entry.getValue() != FiveHeadMech.DONUT) {
				continue;
			}
			ArenaSector pos = arenaPos.forCombatant(entry.getKey());
			if (pos.isStrictlyAdjacentTo(safeDir)) {
				s.accept(fiveHeadSafeSpotFinal.getModified(Map.of("safeSpot", pos)));
				return;
			}
		}
		log.error("No donut!");
		s.accept(fiveHeadSafeSpotError.getModified());
	});

	private XivState getState() {
		return state;
	}


	private final SequentialTrigger<BaseEvent> sixHead = new SequentialTrigger<>(25_000, BaseEvent.class, (e) -> {
		if (e instanceof AbilityUsedEvent aue) {
			long id = aue.getAbility().getId();
			return id == 0x72B1 || id == 0x701C;
		}
		else {
			return false;
		}
	}, (e1, s) -> {
		// Grab 4 tethers
		List<XivCombatant> tetheredHeads = IntStream.range(0, 4)
				.mapToObj((i) -> s.waitEvent(TetherEvent.class, t -> t.getId() == 0x00BD || t.getId() == 0x00B5))
				.map(TetherEvent::getSource)
				.toList();
		// It's okay for this to loop because it has a timeout
		while (true) {
			List<ArenaSector> occupied = tetheredHeads.stream()
					.map(head -> getState().getLatestCombatantData(head))
					.map(arenaPos::forCombatant)
					.toList();
			// CENTER means we don't have position data for that combatant yet
			if (occupied.stream().anyMatch(sector -> sector == ArenaSector.CENTER)) {
				// Just wait for literally anything, since we're really actually waiting for
				s.waitEvent(BaseEvent.class, (e) -> true);
			}
			else {
				List<ArenaSector> safeSpots = new ArrayList<>(List.of(ArenaSector.SOUTHWEST, ArenaSector.WEST, ArenaSector.NORTHWEST, ArenaSector.NORTHEAST, ArenaSector.EAST, ArenaSector.SOUTHEAST));
				occupied.forEach(safeSpots::remove);
				if (safeSpots.size() == 2) {
					s.accept(sixHeadSafeSpots.getModified(Map.of("safeSpot1", safeSpots.get(0), "safeSpot2", safeSpots.get(1))));
					return;
				}
				else {
					log.error("Expected exactly two safe spots, got: {}", safeSpots);
				}
			}
		}
	});

	@HandleEvents
	public void feedHeads(EventContext context, BaseEvent event) {
		fiveHead.feed(context, event);
		sixHead.feed(context, event);
	}

}
