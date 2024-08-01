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
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
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

import java.nio.channels.AsynchronousByteChannel;
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

	// TODO: there's another mechanic after this (electrifying witch hunt)
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
				// Whether the inside is safe, else outside is safe
				boolean insideSafe = e1.abilityIdMatches(0x95DE);
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
	private final ModifiableCallout<AbilityCastStart> widening = ModifiableCallout.durationBasedCall("Widening Initial", "Inside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<AbilityCastStart> narrowing = ModifiableCallout.durationBasedCall("Narrowing Initial", "Outside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<?> wideningF = new ModifiableCallout<>("Widening Followup", "Inside, Baiters { baitOut ? 'Out' : 'In'}");
	private final ModifiableCallout<?> narrowingF = new ModifiableCallout<>("Narrowing Followup", "Outside, Baiters { baitOut ? 'Out' : 'In'}");

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
					if (isWidening ^ (i % 2 == 0)) {
						// We already called first one
						s.updateCall(wideningF);
					}
					else {
						s.updateCall(narrowingF);
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
				int pos = 0xFA0;
				int neg = 0xFA1;


				for (int i = 0; i < 3; i++) {
					var posCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x95D8));
					// TODO: use positions if this continues to be flaky

					s.waitThenRefreshCombatants(100);
					ArenaSector positiveSide = ArenaPos.combatantFacing(state.getLatestCombatantData(posCast.getSource()));
					s.setParam("positive", positiveSide);
					s.setParam("negative", positiveSide.opposite());

					var playerBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(pos, neg));
					if (playerBuff == null) {
						log.error("Player has no buff!");
					}
					else if (playerBuff.buffIdMatches(pos)) {
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
	// The two people that did nothing need to grab the tethers
	private final ModifiableCallout<?> mustardBombInitialTetherNonTank = new ModifiableCallout<>("Mustard Bombs: Initial Tether, Not Tank", "Give Tethers to Tanks");
	private final ModifiableCallout<?> mustardBombInitialTank = new ModifiableCallout<>("Mustard Bombs: Tank", "Grab Tethers");
	private final ModifiableCallout<?> mustardBombAvoidTethers = new ModifiableCallout<>("Mustard Bombs: Avoid Tethers", "Avoid Tethers");
	private final ModifiableCallout<?> mustardBombTankAfter = new ModifiableCallout<>("Mustard Bombs: Tank", "Give Tethers Away");
	private final ModifiableCallout<?> mustardBombGrabTethersAfter = new ModifiableCallout<>("Mustard Bombs: Grab Tethers", "Grab Tethers from Tanks");

	// azure thunmder 962f
	@NpcCastCallout(0x962F)
	private final ModifiableCallout<AbilityCastStart> azureThunder = ModifiableCallout.durationBasedCall("Azure Thunder", "Raidwide");

	// TODO: identify safe spots
	@NpcCastCallout(value = 0x95F5, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> saberTail = ModifiableCallout.durationBasedCall("Sabertail", "Exaflares");

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

	// Wicked special: out of middle (9610, 9611)
	// in middle 9612 + 2x 9613

	// Hitting west, east safe
	// 9602 Aetherial conversion
	// Tail thrust
	// 9606 boss
	// 960E fake
	// Have to move west, east gets hit next
	// 960E fake again
	// 9609 is water where you have to get knocked around, maybe buff tells you which side?
	// seems it can be fire (out) or water (kb), starting left or right
	// 9603 left water first
	// 9605 water right first
	// These lead to 9609?

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

	@AutoFeed
	private final SequentialTrigger<BaseEvent> aetherialConversion = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9603, 0x9605, 0x9602),
			(e1, s) -> {
				switch (((int) e1.getAbility().getId())) {
					case 0x9602 -> {
						// fire hitting west -> east ?
					}
					case 0x9603 -> {
						// water hitting west -> east
					}
					case 0x9605 -> {
						// water hitting east -> west
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> wickedFireInitial = ModifiableCallout.durationBasedCall("Wicked Fire: Initial", "Bait Middle");
	private final ModifiableCallout<?> wickedFireSafeSpot = new ModifiableCallout<>("Wicked Fire: Safe Spot", "{safe} safe");
	private final ModifiableCallout<?> wickedFireSafeSpotIn = new ModifiableCallout<>("Wicked Fire: Second Safe Spot, In", "{safe} safe, In");
	private final ModifiableCallout<?> wickedFireSafeSpotOut = new ModifiableCallout<>("Wicked Fire: Second Safe Spot, Out", "{safe} safe, Out");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> wickedFire = SqtTemplates.sq(60_000,
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
							s.updateCall(wickedFireSafeSpotOut);
						}
						else {
							s.updateCall(wickedFireSafeSpotIn);
						}
					}
				}


			});

	// concentrated burst
	// buddies into spread at 3:32PM


	// Ion Cluster
	/*
	You get positron/negatron, and have to bait a cannon, while the other two do towers
	 */
}
