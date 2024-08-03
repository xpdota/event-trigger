package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

@CalloutRepo(name = "M4S", duty = KnownDuty.M4S)
public class M4S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M4S.class);

	public M4S(XivState state, StatusEffectRepository buffs, ActiveCastRepository casts) {
		this.state = state;
		this.buffs = buffs;
		this.casts = casts;
	}

	private XivState state;
	private StatusEffectRepository buffs;
	private ActiveCastRepository casts;
	private static final ArenaPos ap = new ArenaPos(100, 100, 5, 5);
	private static final ArenaPos apOuterCorners = new ArenaPos(100, 100, 12, 12);
	private static final int positronBuff = 0xFA0;
	private static final int negatronBuff = 0xFA1;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M4S);
	}

	@NpcCastCallout(0x95EF)
	private final ModifiableCallout<AbilityCastStart> wrathOfZeus = ModifiableCallout.durationBasedCall("Wrath of Zeus", "Raidwide");

	// TODO: there's another mechanic after this (electrifying witch hunt)
	/*
	Electrifying:
	Outside safe:
	3x 95EA burst then 95E5 electrifying
	Inside safe:
	2x95EA burst then 95E5 electrifying
	 */

	private final ModifiableCallout<AbilityCastStart> electrifyingInsideSafe = ModifiableCallout.durationBasedCall("Electrifying Witch Hunt: Inside Safe", "Inside");
	private final ModifiableCallout<AbilityCastStart> electrifyingOutsideSafe = ModifiableCallout.durationBasedCall("Electrifying Witch Hunt: Outside Safe", "Outside");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> electrifyingWitchHunt = SqtTemplates.sq(30_000,
			(AbilityCastStart.class), acs -> acs.abilityIdMatches(0x95E5),
			(e1, s) -> {
				int count = casts.getActiveCastsById(0x95EA).size();
				if (count == 2) {
					s.updateCall(electrifyingInsideSafe, e1);
				}
				else if (count == 3) {
					s.updateCall(electrifyingOutsideSafe, e1);
				}
				else {
					log.error("Bad count: {}", count);
				}
			});

	@NpcCastCallout({0x8DEF, 0x9671})
	private final ModifiableCallout<AbilityCastStart> bewitchingFlight = ModifiableCallout.durationBasedCall("Betwitching Flight", "Avoid Lines");

	@NpcCastCallout(0x92C2)
	private final ModifiableCallout<AbilityCastStart> wickedBolt = ModifiableCallout.durationBasedCall("Wicked Bolt", "Stack, Multiple Hits");

	private static boolean baitOut(BuffApplied buff) {
		long rawStacks = buff.getRawStacks();
		if (rawStacks == 759) {
			return true;
		}
		else if (rawStacks == 758) {
			return false;
		}
		else {
			throw new IllegalArgumentException("Unrecognized stack count %s".formatted(rawStacks));
		}

	}

	/*
	Buff b9a for witch hunt
	These all apply at the start, so need to collect them
	759 bait far?
	758 bait close?
	 */
	// electrifying witch hunt 95e5: ?
	// This puts stuff on 4 people
	// other 4 have to bait
	// bait near/far based on buff
	private final ModifiableCallout<AbilityCastStart> witchHuntInsideNoBait = ModifiableCallout.durationBasedCall("Witch Hunt: Inside Safe, No Bait", "Inside, Stay { baitOut ? 'In' : 'Out'}");
	private final ModifiableCallout<AbilityCastStart> witchHuntInsideBait = ModifiableCallout.durationBasedCall("Witch Hunt: Inside Safe, Bait", "Inside, Bait { baitOut ? 'Out' : 'In' }");
	private final ModifiableCallout<AbilityCastStart> witchHuntOutsideNoBait = ModifiableCallout.durationBasedCall("Witch Hunt: Outside Safe, No Bait", "Outside, Stay { baitOut ? 'In' : 'Out'}");
	private final ModifiableCallout<AbilityCastStart> witchHuntOutsideBait = ModifiableCallout.durationBasedCall("Witch Hunt: Outside Safe, Bait", "Outside, Bait { baitOut ? 'Out' : 'In' }");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> witchHunt = SqtTemplates.sq(30_000,
			// TODO: other ID?
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95DE),
			(e1, s) -> {
				int count = casts.getActiveCastsById(0x95EA).size();
				// Whether the inside is safe, else outside is safe
				// If there are two bursts, inside is safe. otherwise outside is safe.
				boolean insideSafe = count == 2;
				// Whether player is baiting
				// Player should bait if they do not have the lightning buff
				boolean playerBaiting = !buffs.isStatusOnTarget(state.getPlayer(), 0x24B);
				log.info("Player baiting");
				BuffApplied bossBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().equals(e1.getSource()) && ba.buffIdMatches(0xB9A));

				// true means bait is far, false means bait is close
				boolean baitOut = baitOut(bossBuff);
				s.setParam("baitOut", baitOut);
				// TODO: got a wrong call. Called correct in/out, but bait position was wrong
				// 3:16 PM
				if (insideSafe) {
					s.updateCall(playerBaiting ? witchHuntInsideBait : witchHuntInsideNoBait, e1);
				}
				else {
					s.updateCall(playerBaiting ? witchHuntOutsideBait : witchHuntOutsideNoBait, e1);
				}
			});

	// widening witch hunt: 95e0: out first
	// alternates between close/far
	// narrowing witch hunt: 95e1: in first
	// alternates between close/far
	private final ModifiableCallout<AbilityCastStart> widening = ModifiableCallout.durationBasedCall("Widening Initial", "Outside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<AbilityCastStart> narrowing = ModifiableCallout.durationBasedCall("Narrowing Initial", "Inside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<?> wideNarrowOutF = new ModifiableCallout<>("Widening/Narrowing Outside Followup", "Outside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<?> wideNarrowInF = new ModifiableCallout<>("Widening/Narrowing Inside Followup", "Inside, Baiters { baitOut ? 'Out' : 'In'}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> wideningNarrowing = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95e0, 0x95e1),
			(e1, s) -> {
				boolean isWidening = e1.abilityIdMatches(0x95e0);
				var baitOuts = new boolean[4];
				var firstBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().equals(e1.getSource()) && ba.buffIdMatches(0xB9A));
				baitOuts[0] = baitOut(firstBuff);

				s.setParam("baitOut", baitOuts[0]);
				if (isWidening) {
					s.updateCall(widening, e1);
				}
				else {
					s.updateCall(narrowing, e1);
				}

				for (int i = 1; i <= 3; i++) {
					var nextBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xB9A));
					baitOuts[i] = baitOut(nextBuff);
				}

				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4D11, 0x4D12));

				for (int i = 1; i <= 3; i++) {
					s.setParam("baitOut", baitOuts[i]);
					// The widening/narrowing alternates each time
					if (isWidening ^ (i % 2 != 0)) {
						// We already called first one
						s.updateCall(wideNarrowOutF);
					}
					else {
						s.updateCall(wideNarrowInF);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4D11, 0x4D12));

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
						s.setParam("safe", safe);
						s.setParam("sides", List.of(safe.plusEighths(-2), safe.plusEighths(2)));
						s.setParam("corners", List.of(safe.plusEighths(-3), safe.plusEighths(3)));
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
					else {
						// This should be fixed now
						log.error("unsafeCorners bad! {} {}", unsafeCorners, lightningCageCasts);
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
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95D6, 0x95D7),
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


				for (int i = 0; i < 3; i++) {
					var posCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95D8));
					// TODO: use positions if this continues to be flaky

					s.waitThenRefreshCombatants(100);
					ArenaSector positiveSide = ArenaPos.combatantFacing(state.getLatestCombatantData(posCast.getSource()));
					s.setParam("positive", positiveSide);
					s.setParam("negative", positiveSide.opposite());

					var playerBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(positronBuff, negatronBuff));
					if (playerBuff == null) {
						log.error("Player has no buff!");
					}
					else if (playerBuff.buffIdMatches(positronBuff)) {
						// get hit by pos
						s.updateCall(negatronStream, posCast);
					}
					else {
						// get hit by neg
						s.updateCall(positronStream, posCast);
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> transplantCast = ModifiableCallout.durationBasedCall("Electrope Transplant: Casted", "Dodge Proteans");
	private final ModifiableCallout<?> transplantMove = new ModifiableCallout<>("Electrope Transplant: Instant", "Move");
	private final ModifiableCallout<?> transplantMoveFront = new ModifiableCallout<>("Electrope Transplant: Cover", "Cover");
	private final ModifiableCallout<?> transplantMoveBack = new ModifiableCallout<>("Electrope Transplant: Get Covered", "Behind");
	private final ModifiableCallout<?> transition = new ModifiableCallout<>("Transition", "Multiple Raidwides, Get Knocked South");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> electropeTransplant = SqtTemplates.sq(120_000,
			(AbilityCastStart.class), acs -> acs.abilityIdMatches(0x98D3),
			(e1, s) -> {
				log.info("Electrope Transplant: Start");
				for (int i = 0; i < 2; i++) {

					log.info("Electrope Transplant: Start round {}", i);
					AbilityCastStart cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x90FE));
					s.updateCall(transplantCast, cast);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x90FE));
					s.updateCall(transplantMove);
					s.waitMs(50);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
					s.updateCall(transplantMove);
					s.waitMs(50);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
					s.updateCall(transplantMove);
					s.waitMs(50);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
					s.updateCall(transplantMove);
					s.waitMs(50);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
					List<XivCombatant> playersThatGotHit = s.waitEventsQuickSuccession(8, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CE))
							.stream()
							.map(AbilityUsedEvent::getTarget)
							.toList();
					if (playersThatGotHit.contains(state.getPlayer())) {
						s.updateCall(transplantMoveBack);
					}
					else {
						s.updateCall(transplantMoveFront);
					}
					s.waitMs(50);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x98CD));
					s.updateCall(transplantMove);
					s.waitMs(50);
				}
				s.waitMs(2000);
				s.updateCall(transition);
			});


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

	// POST TRANSITION

	private static final ArenaPos finalAp = new ArenaPos(100, 165, 5, 5);

	@NpcCastCallout(0x95F2)
	private final ModifiableCallout<AbilityCastStart> crossTailSwitch = ModifiableCallout.durationBasedCall("Cross Tail Switch", "Multiple Raidwides");

	// TODO: identify safe spots
	@NpcCastCallout(value = 0x95F5, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> saberTail = ModifiableCallout.durationBasedCall("Sabertail", "Exaflares");

	// Wicked special: out of middle (9610, 9611)
	// in middle 9612 + 2x 9613

	private final ModifiableCallout<AbilityCastStart> wickedSpecialOutOfMiddle = ModifiableCallout.durationBasedCall("Wicked Special: Out of Middle", "Sides");
	private final ModifiableCallout<AbilityCastStart> wickedSpecialInMiddle = ModifiableCallout.durationBasedCall("Wicked Special: In Middle", "Middle");

	// The ones not run here are handled elsewhere
	@AutoFeed
	private final SequentialTrigger<BaseEvent> wickedSpecialStandalone = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9610, 0x9612),
			(e1, s) -> {
				if (e1.abilityIdMatches(0x9610)) {
					s.updateCall(wickedSpecialOutOfMiddle, e1);
				}
				else {
					s.updateCall(wickedSpecialInMiddle, e1);
				}
			});

	// The two people that did nothing need to grab the tethers
	private final ModifiableCallout<?> mustardBombInitialTetherNonTank = new ModifiableCallout<>("Mustard Bombs: Initial Tether, Not Tank", "Tethers to Tanks then Spread");
	private final ModifiableCallout<?> mustardBombInitialTank = new ModifiableCallout<>("Mustard Bombs: Tank", "Grab Tethers");
	private final ModifiableCallout<?> mustardBombAvoidTethers = new ModifiableCallout<>("Mustard Bombs: Avoid Tethers", "Avoid Tethers");
	private final ModifiableCallout<?> mustardBombTankAfter = new ModifiableCallout<>("Mustard Bombs: Tank", "Give Tethers Away");
	private final ModifiableCallout<?> mustardBombGrabTethersAfter = new ModifiableCallout<>("Mustard Bombs: Grab Tethers", "Grab Bombs from Tanks");

	// azure thunmder 962f
	@NpcCastCallout(0x962F)
	private final ModifiableCallout<AbilityCastStart> azureThunder = ModifiableCallout.durationBasedCall("Azure Thunder", "Raidwide");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> mustardBomb = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x961E),
			(e1, s) -> {
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(mustardBombInitialTank);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x961F));
					s.updateCall(mustardBombTankAfter);
				}
				else {
					s.updateCall(mustardBombInitialTetherNonTank);
					// Kindling Cauldron hits
					var kindlingCauldrons = s.waitEventsQuickSuccession(8, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9620));
					if (kindlingCauldrons.stream().anyMatch(e -> e.getTarget().isThePlayer())) {
						s.updateCall(mustardBombAvoidTethers);
					}
					else {
						s.updateCall(mustardBombGrabTethersAfter);
					}
				}
			});


	@NpcCastCallout(0x9602)
	private final ModifiableCallout<AbilityCastStart> aetherialConversionFireWE = ModifiableCallout.durationBasedCall("Aetherial Conversion Fire West->East", "Later: East Safe then West");
	@NpcCastCallout(0x9604)
	private final ModifiableCallout<AbilityCastStart> aetherialConversionFireEW = ModifiableCallout.durationBasedCall("Aetherial Conversion Fire East->West", "Later: West Safe then East");
	@NpcCastCallout(0x9603)
	private final ModifiableCallout<AbilityCastStart> aetherialConversionWaterWE = ModifiableCallout.durationBasedCall("Aetherial Conversion Water West->East", "Later: Knockback West then East");
	@NpcCastCallout(0x9605)
	private final ModifiableCallout<AbilityCastStart> aetherialConversionWaterEW = ModifiableCallout.durationBasedCall("Aetherial Conversion Water East->West", "Later: Knockback East then West");

	@NpcCastCallout(0x9606)
	private final ModifiableCallout<AbilityCastStart> tailThrustFireWE = ModifiableCallout.durationBasedCall("Tail Thrust: Fire West->East", "East Safe then West");
	@NpcCastCallout(0x9608)
	private final ModifiableCallout<AbilityCastStart> tailThrustFireEW = ModifiableCallout.durationBasedCall("Tail Thrust: Fire East->West", "West Safe then East");
	@NpcCastCallout(0x9607)
	private final ModifiableCallout<AbilityCastStart> tailThrustWaterWE = ModifiableCallout.durationBasedCall("Tail Thrust: Water West->East", "Knockback West then East");
	@NpcCastCallout(0x9609)
	private final ModifiableCallout<AbilityCastStart> tailThrustWaterEW = ModifiableCallout.durationBasedCall("Tail Thrust: Water East->West", "Knockback East then West");

	private final ModifiableCallout<AbilityCastStart> wickedFireInitial = ModifiableCallout.durationBasedCall("Wicked Fire: Initial", "Bait Middle");
	private final ModifiableCallout<?> wickedFireSafeSpot = new ModifiableCallout<>("Wicked Fire: Safe Spot", "{safe} safe");
	private final ModifiableCallout<AbilityCastStart> wickedFireSafeSpotIn = ModifiableCallout.durationBasedCall("Wicked Fire: Second Safe Spot, In", "{safe} safe, In");
	private final ModifiableCallout<AbilityCastStart> wickedFireSafeSpotOut = ModifiableCallout.durationBasedCall("Wicked Fire: Second Safe Spot, Out", "{safe} safe, Out");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twilightSabbath = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9630),
			(e1, s) -> {
				s.updateCall(wickedFireInitial, e1);
				for (int i = 0; i < 2; i++) {

					var fx = s.waitEvents(2, StatusLoopVfxApplied.class, e -> e.getTarget().npcIdMatches(17323));
					Set<ArenaSector> safeSpots = EnumSet.of(ArenaSector.NORTHWEST, ArenaSector.NORTHEAST, ArenaSector.SOUTHWEST, ArenaSector.SOUTHEAST);
					s.waitThenRefreshCombatants(50);
					fx.forEach(f -> {
						ArenaSector combatantLocation = finalAp.forCombatant(state.getLatestCombatantData(f.getTarget()));
						ArenaSector unsafe;
						// Cleaving right
						if (f.vfxIdMatches(793)) {
							// e.g. if add is S and cleaving right, E is unsafe
							unsafe = combatantLocation.plusQuads(-1);
						}
						// Cleaving left
						else if (f.vfxIdMatches(794)) {
							unsafe = combatantLocation.plusQuads(1);
						}
						else {
							log.error("Bad vfx id: {}", f.getStatusLoopVfx().getId());
							return;
						}
						log.info("Unsafe: {} -> {}", combatantLocation, unsafe);
						safeSpots.remove(unsafe.plusEighths(-1));
						safeSpots.remove(unsafe.plusEighths(1));
					});
					fx.stream()
							.map(StatusLoopVfxApplied::getTarget)
							.map(state::getLatestCombatantData)
							.map(finalAp::forCombatant)
							.forEach(safeSpots::remove);

					if (safeSpots.size() != 1) {
						log.error("Bad safeSpots spots! {}", safeSpots);
						continue;
					}
					ArenaSector safe = safeSpots.iterator().next();
					s.setParam("safe", safe);
					if (i == 0) {
						s.updateCall(wickedFireSafeSpot);
					}
					else {
						var wicked = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9610, 0x9612));
						if (wicked.abilityIdMatches(0x9610)) {
							s.updateCall(wickedFireSafeSpotOut, wicked);
						}
						else {
							s.updateCall(wickedFireSafeSpotIn, wicked);
						}
					}
				}


			});

	// concentrated burst
	// buddies into spread at 3:32PM

	@AutoFeed
	private final SequentialTrigger<BaseEvent> midnightSabbath = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9AB9),
			(e1, s) -> {
				// This is the one with eight adds around the arena, and you have to dodge in/out with either partners or spread
				/*
				Midnight Sabbath 2: Clones will spawn with either wings or guns.
				If wings, go into the first active set ((all cardinals or all intercardinals first) on your quadrant.
				If guns, start on the inactive set.

				gun vs wing is determined by weapon ID
				gun = 7
				wing = 31
				gun fired = 6

				Next question, how do we determine the first vs second set?

				Concentrated burst 962B is partners then spread
				Scattered burst 962C is spread then partners
				 */

			});

	@NpcCastCallout(0x949B)
	private final ModifiableCallout<AbilityCastStart> wickedThunder = ModifiableCallout.durationBasedCall("Wicked Thunder", "Raidwide");

	private final ModifiableCallout<BuffApplied> ionCluster2shortPos = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Short Positron", "Short Positron").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2shortNeg = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Short Negatron", "Short Negatron").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2longPos = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Long Positron", "Long Positron").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2longNeg = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Long Negatron", "Long Negatron").autoIcon();

	private final ModifiableCallout<BuffApplied> ionCluster2baitFirstSet = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Bait First Set", "Bait {baitLocations}").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2avoidFirstSet = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Take First Tower", "Soak Tower").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2baitSecondSet = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Bait Second Set", "Bait {baitLocations}").autoIcon();
	private final ModifiableCallout<BuffApplied> ionCluster2avoidSecondSet = ModifiableCallout.<BuffApplied>durationBasedCall("Ion Cluster 2: Take Second Tower", "Soak Tower").autoIcon();

	// Ion Cluster #2, aka Sunrise Sabbath
	@AutoFeed
	private final SequentialTrigger<BaseEvent> ionCluster2sq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9622),
			(e1, s) -> {
				var playerBuff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(positronBuff, negatronBuff));
				boolean playerPos = playerBuff.buffIdMatches(positronBuff);
				// 23 short, 38 long
				boolean playerLong = playerBuff.getInitialDuration().toSeconds() > 30;
				if (playerLong) {
					s.updateCall(playerPos ? ionCluster2longPos : ionCluster2longNeg, playerBuff);
				}
				else {
					s.updateCall(playerPos ? ionCluster2shortPos : ionCluster2shortNeg, playerBuff);
				}
				// Now, wait for special buff to be placed on the guns
				// Positron (FA0) needs to bait gun with B9A 757,
				// Negatron (FA1) needs to bait gun with B9A 756.
				int neededGun = playerPos ? 757 : 756;
				{
					// First round
					var gunBuffs = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0xB9A));
					if (playerLong) {
						s.updateCall(ionCluster2avoidFirstSet, playerBuff);
					}
					else {
						List<ArenaSector> acceptableGuns = gunBuffs.stream()
								.filter(ba -> ba.getRawStacks() == neededGun)
								.map(BuffApplied::getTarget)
								.map(finalAp::forCombatant)
								.toList();
						s.setParam("baitLocations", acceptableGuns);
						s.updateCall(ionCluster2baitFirstSet, playerBuff);
					}
				}
				var wicked = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9610, 0x9612));
				if (wicked.abilityIdMatches(0x9610)) {
					s.updateCall(wickedSpecialOutOfMiddle, wicked);
				}
				else {
					s.updateCall(wickedSpecialInMiddle, wicked);
				}
				{
					// First round
					var gunBuffs = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0xB9A));
					if (playerLong) {
						List<ArenaSector> acceptableGuns = gunBuffs.stream()
								.filter(ba -> ba.getRawStacks() == neededGun)
								.map(BuffApplied::getTarget)
								.map(finalAp::forCombatant)
								.toList();
						s.setParam("baitLocations", acceptableGuns);
						s.updateCall(ionCluster2baitSecondSet, playerBuff);
					}
					else {
						s.updateCall(ionCluster2avoidSecondSet, playerBuff);
					}

				}

			});
	/*
	You get positron/negatron, and have to bait a cannon, while the other two do towers
	 */
	@NpcCastCallout(0x9614)
	private final ModifiableCallout<AbilityCastStart> flameSlash = ModifiableCallout.durationBasedCall("Flame Slash", "Out of Middle, Arena Splitting");

	private final ModifiableCallout<?> rainingSwordNorthmost = new ModifiableCallout<>("Raining Swords: Northmost Safe", "North");
	private final ModifiableCallout<?> rainingSwordNorthmiddle = new ModifiableCallout<>("Raining Swords: North-middle Safe", "North-Middle");
	private final ModifiableCallout<?> rainingSwordSouthmiddle = new ModifiableCallout<>("Raining Swords: South-middle Safe", "South-Middle");
	private final ModifiableCallout<?> rainingSwordSouthmost = new ModifiableCallout<>("Raining Swords: Southmost Safe", "South");

	private final class RainingSwordSafeSpotEvent extends BaseEvent implements HasPrimaryValue {
		@Serial
		private static final long serialVersionUID = -1177380546147020596L;
		final ArenaSector side;
		// Indexed from 0, i.e. 0 = southmost, 3 = northmost
		final int safeSpot;

		private RainingSwordSafeSpotEvent(ArenaSector side, int safeSpot) {
			this.side = side;
			this.safeSpot = safeSpot;
		}

		ModifiableCallout<?> getCallout() {
			return switch (safeSpot) {
				case 3 -> rainingSwordNorthmost;
				case 2 -> rainingSwordNorthmiddle;
				case 1 -> rainingSwordSouthmiddle;
				case 0 -> rainingSwordSouthmost;
				default -> throw new IllegalArgumentException("Bad index: " + safeSpot);
			};
		}

		@Override
		public String toString() {
			return "RainingSwordSafeSpotEvent{" +
			       "side=" + side +
			       ", safeSpot=" + safeSpot +
			       '}';
		}


		@Override
		public String getPrimaryValue() {
			return "%s %s".formatted(safeSpot, side);
		}
	}

	// This trigger is ONLY responsible for collecting - not callout out!
	@AutoFeed
	private final SequentialTrigger<BaseEvent> rainingSwordsColl = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9616),
			(e1, s) -> {
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// Swords should all be present at this point
				// Normally I would do this by position, but the sword IDs seem to have stable positions
				// Lowest ID is bottom left, then up, then over and up
				// There's the initial tethers (279) then the follow up (280).
				// There are 7 follow up sets, for 8 sets in total
				int npcId = 17327;
				// Find the 8 swords
				List<XivCombatant> swords = new ArrayList<>(state.npcsById(npcId));
				swords.sort(Comparator.comparing(XivCombatant::getId));
				if (swords.size() != 8) {
					throw new RuntimeException("Expected 8 swords, there were %s".formatted(swords.size()));
				}
				// Divide into left and right
				List<XivCombatant> leftSwords = swords.subList(0, 4);
				List<XivCombatant> rightSwords = swords.subList(4, 8);
				// Get the lowest ID for each side
				long leftBaseId = leftSwords.get(0).getId();
				long rightBaseId = rightSwords.get(0).getId();
				boolean startRight = false;
				for (int i = 0; i < 8; i++) {
					// These tethers all use the 'source' field as the sword that it is jumping TO
					var tethers = s.waitEvents(3, TetherEvent.class, te -> te.eitherTargetMatches(cbt -> cbt.npcIdMatches(npcId)));
					if (i == 0) {
						// If this is the first iteration, we need to determine whether we are left or right
						startRight = rightSwords.contains(tethers.get(0).getSource());
						log.info("Starting {}", startRight ? "right" : "left");
					}
					// Alternate sides
					boolean thisSideRight = startRight ^ (i % 2 != 0);
					long baseId = thisSideRight ? rightBaseId : leftBaseId;
					Set<Integer> safe = new HashSet<>(Set.of(0, 1, 2, 3));
					tethers.forEach(tether -> {
						int index = (int) (tether.getSource().getId() - baseId);
						log.info("Tether index: {}", index);
						safe.remove(index);
					});
					if (safe.size() != 1) {
						throw new RuntimeException("Safe: " + safe);
					}
					s.accept(new RainingSwordSafeSpotEvent(thisSideRight ? ArenaSector.EAST : ArenaSector.WEST, safe.iterator().next()));

				}
			});
	// This trigger does the actual callouts
	@AutoFeed
	private final SequentialTrigger<BaseEvent> rainingSwordsCall = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9616),
			(e1, s) -> {
				Queue<@NotNull Optional<ModifiableCallout<?>>> queue = new ArrayDeque<>();
				// First collect everything
				for (int i = 0; i < 8; i++) {
					int wave = i / 2;
					var event = s.waitEvent(RainingSwordSafeSpotEvent.class);
					ArenaSector playerSide = state.getPlayer().getPos().x() > 100 ? ArenaSector.EAST : ArenaSector.WEST;
					boolean isMySide = playerSide == event.side;
					// The exception is that if this is the first wave, fire the callout immediately
					if (wave == 0) {
						if (isMySide) {
							s.updateCall(event.getCallout());
						}
						// Nothing to do
					}
					else {
						if (isMySide) {
							queue.add(Optional.of(event.getCallout()));
						}
						else {
							// If off-side, add null as a marker
							queue.add(Optional.empty());
						}
					}
				}
				// Now burn through the queue, waiting for the chain lightning hits
				for (Optional<ModifiableCallout<?>> item : queue) {
					// Wait for another round of hits
					s.waitEventsQuickSuccession(3, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x961A, 0x961B) && aue.isFirstTarget());
					// If not a null marker, fire the call
					item.ifPresent(s::updateCall);
				}
			});
}
