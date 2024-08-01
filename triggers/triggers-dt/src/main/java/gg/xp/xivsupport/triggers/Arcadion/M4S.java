package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@CalloutRepo(name = "M4S", duty = KnownDuty.M4S)
public class M4S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M4S.class);

	public M4S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private XivState state;
	private StatusEffectRepository buffs;
	private static final ArenaPos ap = new ArenaPos(100, 100, 5, 5);
	private static final ArenaPos apOuterCorners = new ArenaPos(100, 100, 12, 12);

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M4S);
	}

	@NpcCastCallout(0x95EF)
	private final ModifiableCallout<AbilityCastStart> wrathOfZeus = ModifiableCallout.durationBasedCall("Wrath of Zeus", "Raidwide");
	// 95EF wrath of zeus raidwide

	// TODO: more IDs?
	// 9671 is "inside"
	@NpcCastCallout({0x8DEF, 0x9671})
	private final ModifiableCallout<AbilityCastStart> bewitchingFlight = ModifiableCallout.durationBasedCall("Betwitching Flight", "Avoid Lines");
	@NpcCastCallout(0x92C2)
	private final ModifiableCallout<AbilityCastStart> wickedBolt = ModifiableCallout.durationBasedCall("Wicked Bolt", "Stack, Multiple Hits");
	// bewitching flight (0x8DEF): avoid lines
	// stay in, then move out, avoid horizontal lines
	// bait

	// TODO: witch hunt
	// electrifying witch hunt 95e5: ?
	// This puts stuff on 4 people
	// other 4 have to bait
	// bait near/far based on buff

	// widening witch hunt: 95e0: out first
	// alternates between close/far
	// narrowing witch hunt: 95e1: in first
	// alternates between close/far
	private final ModifiableCallout<?> wideningOut = new ModifiableCallout<>("Widening/Narrowing Witch Hunt: Out", "Out");
	private final ModifiableCallout<?> wideningIn = new ModifiableCallout<>("Widening/Narrowing Witch Hunt: In", "In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> wideningNarrowing = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95e0, 0x95e1),
			(e1, s) -> {
				// TODO: read the buff
				boolean widening = e1.abilityIdMatches(0x95e0);
				if (widening) {
					s.updateCall(wideningOut);
				}
				else {
					s.updateCall(wideningIn);
				}
			});

	@NpcCastCallout(0x95c6)
	private final ModifiableCallout<AbilityCastStart> witchgleamBasic = ModifiableCallout.durationBasedCall("Witchgleam: Basic", "Stand on Cardinals, Multiple Hits");
	@NpcCastCallout(0x95f0)
	private final ModifiableCallout<AbilityCastStart> wickedJolt = ModifiableCallout.durationBasedCall("Wicked Jolt", "Tank Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> electropeEdgeInitial = ModifiableCallout.durationBasedCall("Electrope Edge", "Clock Positions");
	private final ModifiableCallout<?> electropeEdgeFail = new ModifiableCallout<>("Electrope Edge: Fail/Invalid", "Fail");
	private final ModifiableCallout<?> electropeEdge1long = new ModifiableCallout<>("Electrope Edge: 1 Long", "1 Long");
	private final ModifiableCallout<?> electropeEdge2long = new ModifiableCallout<>("Electrope Edge: 2 Long", "2 Long");
	private final ModifiableCallout<?> electropeEdge2short = new ModifiableCallout<>("Electrope Edge: 2 Short", "2 Short");
	private final ModifiableCallout<?> electropeEdge3short = new ModifiableCallout<>("Electrope Edge: 3 Short", "3 Short");

	private final ModifiableCallout<?> electropeSafeSpot = new ModifiableCallout<>("Electrope Edge: Nothing", "{safe} Safe");
	private final ModifiableCallout<?> electropeSides = new ModifiableCallout<>("Electrope Edge: Spark II (Sides)", "Spark 2 - {sides}");
	private final ModifiableCallout<?> electropeCorners = new ModifiableCallout<>("Electrope Edge: Spark III (Far Corners)", "Spark 3 - {corners}");

	private final ModifiableCallout<AbilityCastStart> sparkBuddies = ModifiableCallout.durationBasedCall("Sidewise Spark + Buddies", "Buddies {safeSide}");
	private final ModifiableCallout<AbilityCastStart> sparkSpread = ModifiableCallout.durationBasedCall("Sidewise Spark + Spread", "Spread {safeSide}");

	private enum SparkMech {
		Buddies,
		Spread
	}

	private @Nullable SparkMech getSparkMech() {
		XivCombatant boss = state.npcById(17322);
		if (boss == null) {
			log.error("No boss!");
			return null;
		}
		var buff = buffs.findStatusOnTarget(boss, 0xB9A);
		if (buff == null) {
			log.error("No buff!");
			return null;
		}
		switch ((int) buff.getRawStacks()) {
			case 752 -> {
				return SparkMech.Buddies;
			}
			case 753 -> {
				return SparkMech.Spread;
			}
			default -> {
				log.error("Unknown buff stacks: {}", buff.getRawStacks());
				return null;
			}
		}
	}

	//95c8 symphoniy fantastique
	@AutoFeed
	private final SequentialTrigger<BaseEvent> symphonyFantastique = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95c8),
			(e1, s) -> {
				// The cross call is handled elsewhere
				// Gather Spark II casts
				var spark2s = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95CA));
				// Gather sidewise spark cast
				AbilityCastStart sidewiseSpark = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95ED, 0x95EC));
				// Compute safe
				var safeSide = sidewiseSpark.abilityIdMatches(0x95EC) ? ArenaSector.WEST : ArenaSector.EAST;
				Set<ArenaSector> possibleSafe = EnumSet.of(safeSide.plusEighths(-1), safeSide.plusEighths(1));
				spark2s.stream().map(AbilityCastStart::getSource).map(ap::forCombatant).forEach(possibleSafe::remove);
				if (possibleSafe.size() == 1) {
					s.setParam("safeSide", possibleSafe.iterator().next());
				}
				else {
					s.setParam("safeSide", ArenaSector.UNKNOWN);
				}
				SparkMech sparkMech = getSparkMech();
				if (sparkMech == SparkMech.Buddies) {
					s.updateCall(sparkBuddies, sidewiseSpark);
				}
				else if (sparkMech == SparkMech.Spread) {
					s.updateCall(sparkSpread, sidewiseSpark);
				}
				else {
					log.error("Sparkmech null!");
				}

			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> electropeEdge = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95c5),
			(e1, s) -> {
				// TODO
				// or is this handled by other triggers already?
			},
			(e1, s) -> {
				s.updateCall(electropeEdgeInitial, e1);
				var myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xF9F) && ba.getTarget().isThePlayer());
				// Collect hits, stop when we see lightning cage cast
				List<AbilityUsedEvent> hits = s.waitEventsUntil(99, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9786),
						AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95CE));
				int myCount = (int) hits.stream().filter(hit -> hit.getTarget().isThePlayer()).count();
				// 22 short, 42 long
				boolean playerIsLong = myBuff.getInitialDuration().toSeconds() > 30;
				log.info("Electrope {} {}", myCount, playerIsLong);
				if (playerIsLong) {
					s.updateCall(switch (myCount) {
						case 1 -> electropeEdge1long;
						case 2 -> electropeEdge2long;
						default -> electropeEdgeFail;
					});
				}
				else {
					s.updateCall(switch (myCount) {
						case 2 -> electropeEdge2short;
						case 3 -> electropeEdge3short;
						default -> electropeEdgeFail;
					});
				}

				{
					var lightningCageCasts = s.waitEventsQuickSuccession(12, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95CF));
					s.waitMs(50);
					// try cast positions first
					var unsafeCorners = lightningCageCasts.stream()
							.map(AbilityCastStart::getLocationInfo)
							.filter(Objects::nonNull)
							.map(DescribesCastLocation::getPos)
							.filter(Objects::nonNull)
							.map(apOuterCorners::forPosition)
							.filter(ArenaSector::isIntercard)
							.toList();
					int limit = 5;
					while (unsafeCorners.size() != 2) {
						s.waitThenRefreshCombatants(100);
						// The safe spot is always between the unsafe corners
						unsafeCorners = lightningCageCasts.stream()
								.map(AbilityCastStart::getSource)
								.map(state::getLatestCombatantData)
								.map(apOuterCorners::forCombatant)
								.filter(ArenaSector::isIntercard)
								.toList();
						if (limit-- < 0) {
							log.error("unsafeCorners fail!");
							break;
						}
					}
					if (unsafeCorners.size() == 2) {
						ArenaSector safe = ArenaSector.tryCombineTwoQuadrants(unsafeCorners);
						if (safe == null) {
							log.error("Safe fail! unsafeCorners: {}", unsafeCorners);
						}
						else {

							s.setParam("safe", safe);
							s.setParam("sides", List.of(safe.plusEighths(-2), safe.plusEighths(2)));
							s.setParam("corners", List.of(safe.plusEighths(-3), safe.plusEighths(3)));
							if (playerIsLong) {
								// Call safe spot
								s.updateCall(electropeSafeSpot);
							}
							else {
								if (myCount == 2) {
									s.updateCall(electropeSides);
								}
								else {
									s.updateCall(electropeCorners);
								}
							}
						}
					}
					else {
						// This should be fixed now
						log.error("unsafeCorners bad! {} {}", unsafeCorners, lightningCageCasts);
					}
				}

				// The boss also does a sidewise spark 95ED (cleaving left) or 95EC (cleaving right)

				AbilityCastStart sidewiseSpark = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95ED, 0x95EC));
				s.setParam("safeSide", sidewiseSpark.abilityIdMatches(0x95EC) ? ArenaSector.WEST : ArenaSector.EAST);
				SparkMech sparkMech = getSparkMech();
				if (sparkMech == SparkMech.Buddies) {
					s.updateCall(sparkBuddies, sidewiseSpark);
				}
				else if (sparkMech == SparkMech.Spread) {
					s.updateCall(sparkSpread, sidewiseSpark);

				}
				else {
					log.error("Sparkmech null!");
				}

				{
					var lightningCageCasts = s.waitEventsQuickSuccession(12, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95CF));
					s.waitThenRefreshCombatants(100);
					// The safe spot is always between the unsafe corners
					var unsafeCorners = lightningCageCasts.stream()
							.map(AbilityCastStart::getSource)
							.map(state::getLatestCombatantData)
							.map(apOuterCorners::forCombatant)
							.filter(ArenaSector::isIntercard)
							.toList();
					if (unsafeCorners.size() == 2) {
						ArenaSector safe = ArenaSector.tryCombineTwoQuadrants(unsafeCorners);
						s.setParam("safe", safe);
						s.setParam("sides", List.of(safe.plusQuads(-1), safe.plusQuads(1)));
						s.setParam("corners", List.of(safe.plusQuads(-3), safe.plusQuads(3)));
						if (!playerIsLong) {
							// Call safe spot
							s.updateCall(electropeSafeSpot);
						}
						else {
							if (myCount == 1) {
								s.updateCall(electropeSides);
							}
							else {
								s.updateCall(electropeCorners);
							}
						}

					}
				}

				// stack marker handled elsewhere


			});

	private final ModifiableCallout<AbilityCastStart> westSafe = ModifiableCallout.durationBasedCall("Stampeding Thunder: West Safe", "West");
	private final ModifiableCallout<AbilityCastStart> eastSafe = ModifiableCallout.durationBasedCall("Stampeding Thunder: East Safe", "East");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stampedingThunderSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8E2F),
			(e1, s) -> {
				s.waitThenRefreshCombatants(200);
				boolean westHit = state.getLatestCombatantData(e1.getTarget()).getPos().x() < 100;
				if (westHit) {
					s.updateCall(eastSafe, e1);
				}
				else {
					s.updateCall(westSafe, e1);
				}
			});

	private final ModifiableCallout<AbilityCastStart> positronStream = ModifiableCallout.durationBasedCall("Positron", "Go {positive}");
	private final ModifiableCallout<AbilityCastStart> negatronStream = ModifiableCallout.durationBasedCall("Negatron", "Go {negative}");

	@PlayerStatusCallout(0xFA2)
	private final ModifiableCallout<BuffApplied> remote = ModifiableCallout.<BuffApplied>durationBasedCall("Remote Current", "Remote Current").autoIcon();
	@PlayerStatusCallout(0xFA3)
	private final ModifiableCallout<BuffApplied> proximate = ModifiableCallout.<BuffApplied>durationBasedCall("Proximate Current", "Proximate Current").autoIcon();
	@PlayerStatusCallout(0xFA4)
	private final ModifiableCallout<BuffApplied> spinning = ModifiableCallout.<BuffApplied>durationBasedCall("Spinning Conductor", "Spinning").autoIcon();
	@PlayerStatusCallout(0xFA5)
	private final ModifiableCallout<BuffApplied> roundhouse = ModifiableCallout.<BuffApplied>durationBasedCall("Roundhouse Conductor", "Roundhouse - Spread").autoIcon();
	@PlayerStatusCallout(0xFA6)
	private final ModifiableCallout<BuffApplied> collider = ModifiableCallout.<BuffApplied>durationBasedCall("Collider Conductor", "Get Hit by Protean").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> electronStream = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95D7),
			(e1, s) -> {
//				hits with positron stream (95d8) and negatron stream (95d9)
//				get hit by opposite color
//				applies to 1 of each group:
//				Collider Conductor (7s, FA6)
//				2x Spinning Conductor (5s, FA4) OR ???
//				Remote Conductor (5s, FA2) OR ??? (FA3 far tether)
//				You also get 2 stacks of the opposite of what you got hit by (e.g. positron gets negatron):
//				Positron FA0
//				Negatron FA1
//
//				Collider (FA6) - need to get hit by protean
//				Close tether (FA3) or far tether (FA2) - will shoot protean when tether condition satisfied
//				Spinning (FA4) - dynamo
//						? (FA5) - tiny chariot

				// do it again 2 more times
				int pos = 0xFA0;
				int neg = 0xFA1;


				for (int i = 0; i < 3; i++) {
					var posCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95D8));
					s.waitMs(100);
					ArenaSector positiveSide = ArenaPos.combatantFacing(state.getLatestCombatantData(posCast.getSource()));
					s.setParam("positive", positiveSide);
					s.setParam("negative", positiveSide.opposite());

					var playerBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(pos, neg));
					if (playerBuff == null) {
						log.error("Player has no buff!");
					}
					else if (playerBuff.buffIdMatches(pos)) {
						// get hit by pos
						s.updateCall(negatronStream, e1);
					}
					else {
						// get hit by neg
						s.updateCall(positronStream, e1);
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> transplantCast = ModifiableCallout.durationBasedCall("Electrope Transplant: Casted", "Dodge Proteans");
	private final ModifiableCallout<?> transplantMove = new ModifiableCallout<>("Electrope Transplant: Instant", "Move");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> electropeTransplant = SqtTemplates.sq(120_000,
			(AbilityCastStart.class), acs -> acs.abilityIdMatches(0x98D3),
			(e1, s) -> {
				log.info("Electrope Transplant: Start");
				AbilityCastStart cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x90FE));
				s.updateCall(transplantCast, cast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x90FE));
				s.updateCall(transplantMove);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
				s.updateCall(transplantMove);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
				s.updateCall(transplantMove);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
				s.updateCall(transplantMove);
				List<XivCombatant> playersThatGotHit = s.waitEventsQuickSuccession(8, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CE))
						.stream()
						.map(AbilityUsedEvent::getTarget)
						.toList();
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
				s.updateCall(transplantMove);
			});

	//	@NpcCastCallout() // TODO there is no cast for this - need to implement the mechanic before this
	private final ModifiableCallout<AbilityCastStart> transition = ModifiableCallout.durationBasedCall("Transition", "Multiple Raidwides");

	//95c6 witchgleam

	// There's a thing where you have to avoid cardinals


	/*

	// TODO: what is the tell for stack/spread here?
	// Maybe buff B9A on the boss indicates pairs?
	// Seems the raw stack count matters?
	// 752 = pairs?
	// 753 = spread?
	// wicked bolt 92c2 = stack
	from the stack/spread, you get an additional point,
	do the pattern again
	stack at end of mechanic

	lightning cage (95CF) marks unsafe squares on the 5x5 grid
	 */

	/*
	Sidewise spark: 95EC cleaving right
	 */

	//95c6 witchgleam blind hits intercards

	//95c8 symphoniy fantastique
	/*
	Spark II 95CA casts in two corners. Those are completely unsafe.
	Spark 95C9 casts in two other corners. Those are only unsafe in the spot where spark is casting.
	The boss also does a sidewise spark 95ED (cleaving left) or 95EC (cleaving right)
	 */
	/*
	electron stream = wild charge
	95D7

	 */

	/*
	Transitionproteans
	alternate/spread
	 */

	/*
	Electrope transplant 98D3
	need to block for whoever got hit
	transition:
	multiple raidwides
	get knocked south
	 */

	/*
	p2
	cross trail switch 95F3 - multiple hits

	 */
	/*
	Buff b9a for witch hunt
	These all apply at the start, so need to collect them
	759 bait far?
	758 bait close?
	 */

	// POST TRANSITION
	@NpcCastCallout(0x95F2)
	private final ModifiableCallout<AbilityCastStart> crossTailSwitch = ModifiableCallout.durationBasedCall("Cross Tail Switch", "Multiple Raidwides");
	// The two people that did nothing need to grab the tethers
	@NpcCastCallout(0x961E)
	private final ModifiableCallout<AbilityCastStart> mustardBomb = ModifiableCallout.durationBasedCall("Mustard Bombs", "Tethers");

	// azure thunmder 962f
	@NpcCastCallout(0x962F)
	private final ModifiableCallout<AbilityCastStart> azureThunder = ModifiableCallout.durationBasedCall("Azure Thunder", "Raidwide");
}
