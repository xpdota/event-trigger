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
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@CalloutRepo(name = "M12S", duty = KnownDuty.M12S)
public class M12S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M12S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M12S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M12S);
	}

	// 16F triangle
	// 170 defa
	// 171 stack
	// 176 boss
	private static final int PROTEAN_TETHER = 0x16F;
	private static final int DEFA_TETHER = 0x170;
	private static final int STACK_TETHER = 0x171;
	private static final int BOSS_TETHER = 0x176;

	private final ArenaPos tightAp = new ArenaPos(100, 100, 4, 4);


	@NpcCastCallout(0xB4D7)
	private final ModifiableCallout<AbilityCastStart> theFixer = ModifiableCallout.durationBasedCall("The Fixer", "Raidwide");

	private final ModifiableCallout<?> mortalSlayerBothPurpleLeft = new ModifiableCallout<>("Mortal Slayer: Both Purple Left, Initial", "Both Purple Left");
	private final ModifiableCallout<?> mortalSlayerBothPurpleRight = new ModifiableCallout<>("Mortal Slayer: Both Purple Right, Initial", "Both Purple Right");
	private final ModifiableCallout<?> mortalSlayerPurpleBothSides = new ModifiableCallout<>("Mortal Slayer: Purple Both Sides, Initial", "Purple Both Sides");

	private final ModifiableCallout<?> mortalSlayerHit = new ModifiableCallout<>("Mortal Slayer: Hitting", "{ leftPurple ? 'Purple' : 'Green' } { rightPurple ? 'Purple' : 'Green' }", "{ i } { leftPurple ? 'Purple' : 'Green' }/{ rightPurple ? 'Purple' : 'Green' }");

	private static boolean isPurple(XivCombatant orb) {
		return orb.npcIdMatches(19200);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mortalSlayerSq = SqtTemplates.sq(
			90_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB495),
			(e1, s) -> {
				// Balls have NPC ID of either 19200 (purple tank orb) or 19201 (green normal orb)
				log.info("Mortal Slayer Start");
				List<XivCombatant> orbs = new ArrayList<>();
				List<XivCombatant> left = new ArrayList<>();
				List<XivCombatant> right = new ArrayList<>();
				List<XivCombatant> purples = new ArrayList<>();
				while (orbs.size() < 8) {
					s.waitThenRefreshCombatants(150);
					List<XivCombatant> allOrbs = state.npcsByIds(19200, 19201);
					for (XivCombatant orb : allOrbs) {
						Position pos = orb.getPos();
						if (pos.y() > 80 || Math.abs(pos.x() - 100) < 2) {
							continue;
						}
						if (!orbs.contains(orb)) {
							orbs.add(orb);
							if (pos.x() < 100) {
								left.add(orb);
							}
							else {
								right.add(orb);
							}
							if (isPurple(orb)) {
								purples.add(orb);
								if (purples.size() == 2) {
									var x1 = purples.get(0).getPos().x();
									var x2 = purples.get(1).getPos().x();
									if (x1 < 100 && x2 < 100) {
										s.updateCall(mortalSlayerBothPurpleLeft);
									}
									else if (x1 > 100 && x2 > 100) {
										s.updateCall(mortalSlayerBothPurpleRight);
									}
									else {
										s.updateCall(mortalSlayerPurpleBothSides);
									}
								}
							}
						}
					}
				}
				s.waitMs(2_000);
				for (int i = 1; i <= 4; i++) {
					XivCombatant leftOrb = left.get(i - 1);
					XivCombatant rightOrb = right.get(i - 1);
					boolean leftPurple = isPurple(leftOrb);
					boolean rightPurple = isPurple(rightOrb);
					s.setParam("i", i);
					s.setParam("leftPurple", leftPurple);
					s.setParam("rightPurple", rightPurple);
					s.updateCall(mortalSlayerHit);
					s.waitMs(1_000);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB496, 0xB498));
				}
			});

	/*
	129A Shared Grotesquerie: stack
	1299 Busrting Grotesquerie: spread
	1370 Directed Grotesquerie: everyone?
	 */

	private final ModifiableCallout<BuffApplied> grotesquerie1stack = ModifiableCallout.<BuffApplied>durationBasedCall("Grotesquerie 1: Stack", "Stack on {event.target}").autoIcon();
	private final ModifiableCallout<BuffApplied> grotesquerie1spread = ModifiableCallout.<BuffApplied>durationBasedCall("Grotesquerie 1: Spread", "Spread").autoIcon();
	private final ModifiableCallout<AbilityCastStart> grotesquerie1safeSpot = ModifiableCallout.<AbilityCastStart>durationBasedCall("Grotesquerie 1: Safe Spot", "{safe} Safe, Aim Cone").statusIcon(0x1370);
	private final ModifiableCallout<HeadMarkerEvent> grotesquerie1tb = new ModifiableCallout<>("Grotesquerie 1: Tankbuster", "Tankbuster");
	private final ModifiableCallout<?> grotesquerie1stackAfter = new ModifiableCallout<>("Grotesquerie 1: Stack", "Stack on {stackOn}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> grotesquerie1sq = SqtTemplates.sq(
			90_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBEBD),
			(e1, s) -> {
				var spreads = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0x1299));
				spreads.stream().filter(spread -> spread.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(mySpread -> s.updateCall(grotesquerie1spread, mySpread),
								() -> {
									BuffApplied stackBuff = s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(0x129A));
									s.updateCall(grotesquerie1stack, stackBuff);
								});
				// TODO: also maybe B46D? doesn't seem like it
				var dragonCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xB49D), false);
				ArenaSector dragonHitting = ArenaPos.combatantFacing(dragonCast.getLocationInfo().getBestHeading());
				var safe = dragonHitting.opposite();
				s.setParam("safe", safe);
				// Don't replace the spread/stack call
				s.call(grotesquerie1safeSpot, dragonCast);

				List<HeadMarkerEvent> markers = s.waitEvents(3, HeadMarkerEvent.class, hme -> hme.markerIdMatches(161, 344));
				markers.stream().filter(m -> m.markerIdMatches(344) && m.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(myTb -> {
									// Player has TB
									s.updateCall(grotesquerie1tb, myTb);
								},
								() -> {
									// No TB, stack instead
									markers.stream().filter(hme -> hme.markerIdMatches(161)).findAny().ifPresent(m -> s.setParam("stackOn", m.getTarget()));
									s.updateCall(grotesquerie1stackAfter);
								});
				// Stack sfafe spot?
			}
	);


	// When Alpha/beta debuff expires, it is replaced with a different version that has an 18 second timer
	private final ModifiableCallout<BuffApplied> grot2alpha = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Grotesquerie Act 2: Alpha", "Alpha {numInLine} with {buddy}", Duration.ofSeconds(18)).autoIcon();
	private final ModifiableCallout<BuffApplied> grot2beta = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Grotesquerie Act 2: Beta", "Beta {numInLine} with {buddy}", Duration.ofSeconds(18)).autoIcon();

	private final ModifiableCallout<BuffApplied> popSoonAlpha = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Grotesquerie Act 2: Pop Next (Alpha)", "Pop Next Outside", Duration.ofSeconds(18)).autoIcon();
	private final ModifiableCallout<BuffApplied> popSoonBeta = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Grotesquerie Act 2: Pop Next (Beta)", "Pop Next Inside", Duration.ofSeconds(18)).autoIcon();
	private final ModifiableCallout<BuffApplied> popNowAlpha = ModifiableCallout.<BuffApplied>durationBasedCall("Grotesquerie Act 2: Pop Now (Alpha)", "Pop Outside").autoIcon();
	private final ModifiableCallout<BuffApplied> popNowBeta = ModifiableCallout.<BuffApplied>durationBasedCall("Grotesquerie Act 2: Pop Now (Beta)", "Pop Inside").autoIcon();
	private final ModifiableCallout<?> soakSoon = new ModifiableCallout<>("Grotesquerie Act 2: Soak Soon", "Soak Soon");
	private final ModifiableCallout<AbilityCastStart> soakNow = ModifiableCallout.durationBasedCall("Grotesquerie Act 2: Soak Now", "Soak");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> grotesquerie2sq = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBEBE),
			(e1, s) -> {
				var lineBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xBBC, 0xBBD, 0xBBE, 0xD7B));
				var alphaBeta = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0x1290, 0x1292));
				boolean alpha = alphaBeta.buffIdMatches(0x1290);
				int inLine = switch ((int) lineBuff.getBuff().getId()) {
					case 0xBBC -> 1;
					case 0xBBD -> 2;
					case 0xBBE -> 3;
					case 0xD7B -> 4;
					default -> {
						log.error("Unexpected line buff: {}", lineBuff.getBuff().getId());
						yield 0;
					}
				};
				int soakOrder = switch (inLine) {
					case 1 -> 3;
					case 2 -> 4;
					case 3 -> 1;
					case 4 -> 2;
					default -> {
						log.error("Unexpected line buff: {}", lineBuff.getBuff().getId());
						yield 0;
					}
				};
				s.setParam("numInLine", inLine);
				var buddyBuff = s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(lineBuff.getBuff().getId()) && !ba.getTarget().isThePlayer());
				s.setParam("buddy", buddyBuff.getTarget());
				s.updateCall(alpha ? grot2alpha : grot2beta, alphaBeta);
				// Longer delay between debuffs and first in line
				s.waitMs(14_000);
				for (int i = 1; i <= 4; i++) {
					s.setParam("i", i);
					log.info("GT2: i=={}, inLine=={}, soakOrder=={}", i, inLine, soakOrder);
					RawModifiedCallout<?> soakSoonCall = null;
					if (i == soakOrder) {
						soakSoonCall = s.call(soakSoon);
					}
					int lineBuffId = switch (i) {
						case 1 -> 0xBBC;
						case 2 -> 0xBBD;
						case 3 -> 0xBBE;
						case 4 -> 0xD7B;
						default -> {
							log.error("Unexpected line buff: {}", i);
							yield 0;
						}
					};
					var currentLineBuff = buffs.findBuffById(lineBuffId);
					if (currentLineBuff != null) {
						s.waitBuffRemoved(buffs, currentLineBuff);
					}
					s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(0x1291, 0x1293));
					if (i == inLine) {
						// Popping on this iteration
						log.info("GT2: {} pop now", i);
						s.updateCall(alpha ? popNowAlpha : popNowBeta, alphaBeta);
					}
					else if (i + 1 == inLine) {
						// Popping on next iteration
						log.info("GT2: {} pop soon", i);
						s.updateCall(alpha ? popSoonAlpha : popSoonBeta, alphaBeta);
					}
					log.info("GT2: {} waiting for tower cast", i);
					var tower = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4B7, 0xB4B3));
					if (i == soakOrder) {
						soakSoonCall.forceExpire();
						s.call(soakNow, tower);
					}
//					s.waitMs(500);
					log.info("GT2: {} waiting for tower soak", i);
//					s.waitCastFinished(casts, tower);
					log.info("GT2: {} finished", i);
					log.info("GT2 finished loop {}", i);
				}
				// TODO: GTFO call
				// TODO: maybe this is better if it were broken into two triggers?
			}
	);

	@NpcCastCallout(0xB9C4)
	private final ModifiableCallout<AbilityCastStart> splattershed = ModifiableCallout.durationBasedCallWithExtraCastTime("Splattershed", "Raidwide");

	private final ModifiableCallout<BuffApplied> gt3mitotic = ModifiableCallout.<BuffApplied>durationBasedCall("Grotesquerie 3: Mitotic Debuff Direction", "Hitting {mitoticDir}").autoIcon();
	private final ModifiableCallout<?> gt3cardSafe = new ModifiableCallout<>("Grotesquerie 3: Cardinal Safe", "Cardinals");
	private final ModifiableCallout<?> gt3intercardSafe = new ModifiableCallout<>("Grotesquerie 3: Cardinal Safe", "Intercard");

	private final ModifiableCallout<?> gt3tankBait = new ModifiableCallout<>("Grotesquerie 3: Tank Bait", "Bait");
	private final ModifiableCallout<?> gt3tankBaitNonTank = new ModifiableCallout<>("Grotesquerie 3: Tank Bait, Not Tank", "Avoid Tanks");

	private final ModifiableCallout<?> gt3spreadBait = new ModifiableCallout<>("Grotesquerie 3: Spread Bait", "Out, Bait Spreads");
	private final ModifiableCallout<?> gt3spreadBaitTank = new ModifiableCallout<>("Grotesquerie 3: Spread Bait, Tank", "In");

	private final ModifiableCallout<?> gt3baitPuddles = new ModifiableCallout<>("Grotesquerie 3: Bait Puddles", "Bait Puddles");
	private final ModifiableCallout<AbilityCastStart> grotesquerie3safeSpot = ModifiableCallout.<AbilityCastStart>durationBasedCall("Grotesquerie 3: Safe Spot", "Spread {safe}").statusIcon(0x1299);
	private final ModifiableCallout<AbilityCastStart> grotesquerie3getHit = ModifiableCallout.<AbilityCastStart>durationBasedCall("Grotesquerie 3: Get Hit", "Spread {unsafe}").statusIcon(0x129b);

	private final ModifiableCallout<?> gt3middleGetTether = new ModifiableCallout<>("Grotesquerie 3: Wait Middle for Tether", "Wait Middle");
	private final ModifiableCallout<TetherEvent> gt3tetherPartner = new ModifiableCallout<>("Grotesquerie 3: Tether", "{spreadSafe} With {buddy}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> grotesquerie3sq = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBEBF),
			(e1, s) -> {
				log.info("GT3 start");
				var dirBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDE6));
				long rawStacks = dirBuff.getRawStacks();
				/*
				Raw stacks is from 1078 to 1081

				1078 = pointing north
				1079 = pointing east
				1080 = pointing south
				1081 = pointing west
				 */
				ArenaSector mitoticDir = switch ((int) rawStacks) {
					case 1078 -> ArenaSector.NORTH;
					case 1079 -> ArenaSector.EAST;
					case 1080 -> ArenaSector.SOUTH;
					case 1081 -> ArenaSector.WEST;
					default -> ArenaSector.UNKNOWN;
				};
				s.setParam("mitoticDir", mitoticDir);
				// This one has the timer
				var mitoticBuff = s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(0x129C) && ba.getTarget().isThePlayer());
				s.updateCall(gt3mitotic, mitoticBuff);

				boolean isNwCornerGettingHit = false;
				for (int i = 0; i < 4; i++) {
					AbilityCastStart cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4A0, 0xB4A1, 0xB4A2));
					DescribesCastLocation<AbilityCastStart> li = s.waitForCastLocation(cast);
					boolean isNwCorner = li.getPos().distanceFrom2D(Position.of2d(82.5, 87.5)) < 2;
					if (isNwCorner) {
						isNwCornerGettingHit = true;
						break;
					}
				}
				if (isNwCornerGettingHit) {
					s.call(gt3cardSafe);
				}
				else {
					s.call(gt3intercardSafe);
				}
				// Tower cast
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB923));
				s.updateCall(state.playerJobMatches(Job::isTank) ? gt3tankBait : gt3tankBaitNonTank);
				// TODO: there is also a b4a7 a bit sooner but not sure if it has locked target at that point
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4AB));
				s.updateCall(state.playerJobMatches(Job::isTank) ? gt3spreadBaitTank : gt3spreadBait);

				// Spread
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4A8));
				s.updateCall(gt3baitPuddles);

				var dragonCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xB49D), false);
				ArenaSector dragonHitting = ArenaPos.combatantFacing(dragonCast.getLocationInfo().getBestHeading());
				var safe = dragonHitting.opposite();
				s.setParam("unsafe", dragonHitting);
				s.setParam("safe", safe);
				// Don't replace the spread/stack call
				boolean getHit = buffs.isStatusOnTarget(state.getPlayer(), 0x129b);
				s.updateCall(getHit ? grotesquerie3getHit : grotesquerie3safeSpot, dragonCast);
				// Wait to get hit
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x1299, 0x129b));
				s.updateCall(gt3middleGetTether);

				var badSpotEvents = s.waitEvents(5, ActorControlExtraEvent.class, acee -> !acee.getTarget().isPc());
				Set<ArenaSector> spreadSafe = EnumSet.of(ArenaSector.NORTHWEST, ArenaSector.NORTHEAST, ArenaSector.SOUTHWEST, ArenaSector.SOUTHEAST);

				for (ActorControlExtraEvent badSpotEvent : badSpotEvents) {
					spreadSafe.remove(tightAp.forCombatant(badSpotEvent.getTarget()));
				}
				s.setParam("spreadSafe", spreadSafe);

				// Wait for the 4 player tethers in quick succession, find ours, and call out who we are tethered to
				var tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isPc));
				tethers.stream()
						.filter(t -> t.eitherTargetMatches(XivCombatant::isThePlayer))
						.findAny()
						.ifPresent(myTether -> {
							XivCombatant buddy = myTether.getTargetMatching(cbt -> !cbt.isThePlayer());
							s.setParam("buddy", buddy);
							s.updateCall(gt3tetherPartner, myTether);
						});


			});

	// TODO: left/right mechanic before this

	@NpcCastCallout(0xB4C6)
	private final ModifiableCallout<AbilityCastStart> slaughterShed = ModifiableCallout.durationBasedCall("Slaughtershed", "Raidwide");

	private final ModifiableCallout<HeadMarkerEvent> slaughtershedSpread = new ModifiableCallout<>("End: Spread", "Spread");
	private final ModifiableCallout<?> slaughtershedStack = new ModifiableCallout<>("End: Stack", "Stack on {stackOn}");

	// TODO: knockback thing

	@AutoFeed
	private final SequentialTrigger<BaseEvent> slaughter = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4C6),
			(e1, s) -> {
				// 317 is stack, 375 is spread
				List<HeadMarkerEvent> markers = s.waitEvents(5, HeadMarkerEvent.class, hme -> hme.markerIdMatches(317, 375));
				// If player has spread, call that
				// TODO: call safe spots
				markers.stream()
						.filter(m -> m.markerIdMatches(375) && m.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(
								mySpread -> s.updateCall(slaughtershedSpread, mySpread),
								() -> {
									// Otherwise, call to stack on the player who has the stack marker
									markers.stream().filter(hme -> hme.markerIdMatches(317)).findAny()
											.ifPresent(m -> s.setParam("stackOn", m.getTarget()));
									s.updateCall(slaughtershedStack);
								}
						);
			});

	@NpcCastCallout(value = 0xB538, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> refreshingOverkill = ModifiableCallout.durationBasedCall("Refreshing Overkill", "Enrage");

	// PHASE 2

	@NpcCastCallout(0xB528)
	private final ModifiableCallout<AbilityCastStart> arcadiaAflame = ModifiableCallout.durationBasedCall("Arcadia Aflame", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> replHorizontalCleavingHorizontal = ModifiableCallout.durationBasedCall("Replication: Horizonal, Cleaving Horizontal", "Horizontal");
	private final ModifiableCallout<AbilityCastStart> replHorizontalCleavingVertical = ModifiableCallout.durationBasedCall("Replication: Horizontal, Cleaving Vertical", "Vertical");
	private final ModifiableCallout<AbilityCastStart> replVerticalCleavingHorizontal = ModifiableCallout.durationBasedCall("Replication: Vertical, Cleaving Horizontal", "Horizontal");
	private final ModifiableCallout<AbilityCastStart> replVerticalCleavingVertical = ModifiableCallout.durationBasedCall("Replication: Vertical, Cleaving Vertical", "Vertical");

	private final ModifiableCallout<BuffApplied> replFireRes = ModifiableCallout.<BuffApplied>durationBasedCall("Replication: Fire Res Down", "Fire Res").autoIcon();
	private final ModifiableCallout<BuffApplied> replDarkRes = ModifiableCallout.<BuffApplied>durationBasedCall("Replication: Dark Res Down", "Dark Res").autoIcon();
	private final ModifiableCallout<BuffApplied> replNoRes = new ModifiableCallout<>("Replication: No Res Down", "Nothing");

	@NpcCastCallout(0xB527)
	private final ModifiableCallout<AbilityCastStart> snakingKick = ModifiableCallout.durationBasedCall("Snaking Kick", "Behind");

	private final ModifiableCallout<?> replGetHitByFire = new ModifiableCallout<>("Replication: Get Hit By Fire", "Stack {fireInCard} In, {fireOutCard} Out")
			.extendedDescription("""
					It is recommended that you remove whichever half of this callout you are not using.
					The variables you can use are fireIn and fireOut (where the inner and outer fire clones are), and fireInCard and fireOutCard (the adjacent safe cardinal based on the cleaves).""");
	private final ModifiableCallout<?> replGetHitByDark = new ModifiableCallout<>("Replication: Get Hit By Dark", "Spread {darkInCard} In, {darkOutCard} Out").extendedDescription("See above, but replace 'fire' with 'dark'");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> replication = SqtTemplates.selfManagedMultiInvocation(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4D8),
			(e1, s, i) -> {
				// Only care about the first cast
				if (i > 0) {
					return;
				}
				log.info("Replication: start");
				/*
				Winged Scourge: B4DC cone (plus fake B4DA)
				Top-tier slam B4DD -> B4DE actual hit, applies fire res down?
				Mighty Magic B4DF -> B4E0, applies dark res down
				 */

				var winged = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4DC));
				// One winged scourge is enough to tell us the initial pattern
				DescribesCastLocation<AbilityCastStart> wingedLoc = s.waitForCastLocation(winged);
				var cleavingFrom = tightAp.forPosition(wingedLoc.getPos());
				var cleavingTo = ArenaPos.combatantFacing(wingedLoc.getBestHeading());
				log.info("winged: {}", winged);
				log.info("wingedLoc: {}", wingedLoc);
				log.info("Cleaving from {}, to {}", cleavingFrom, cleavingTo);
				boolean fromHoriz = cleavingFrom == ArenaSector.WEST || cleavingFrom == ArenaSector.EAST;
				boolean cleavingHoriz = cleavingTo == ArenaSector.WEST || cleavingTo == ArenaSector.EAST;
				log.info("fromHoriz={}, cleavingHoriz={}", fromHoriz, cleavingHoriz);
				if (fromHoriz) {
					s.updateCall(cleavingHoriz ? replHorizontalCleavingHorizontal : replHorizontalCleavingVertical, winged);
				}
				else {
					s.updateCall(cleavingHoriz ? replVerticalCleavingHorizontal : replVerticalCleavingVertical, winged);
				}
				var mmMob = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB4DF), false).getSource();
				var ttsMob = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB4DD), false).getSource();
				log.info("Replication: mmMob={}, ttsMob={}", mmMob, ttsMob);


				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xCFB));

				s.waitMs(1_000);
				// At this point, player has Fire Rest Down (B79), Dark Resistance Down (CFB), or nothing
				var myFire = buffs.findStatusOnTarget(state.getPlayer(), 0xB79);
				var myDark = buffs.findStatusOnTarget(state.getPlayer(), 0xCFB);
				// If you have no debuff or fire, go to dark
				// If you have dark debuff, get hit by fire
				// This variable tracks which mob's clones you need to get hit by
				XivCombatant neededMob;
				boolean needFire;
				if (myFire != null) {
					log.info("Replication 1: Player has Fire Res Down");
					s.updateCall(replFireRes, myFire);
					neededMob = mmMob;
					needFire = false;
				}
				else if (myDark != null) {
					log.info("Replication 1: Player has Dark Res Down");
					s.updateCall(replDarkRes, myDark);
					neededMob = ttsMob;
					needFire = true;
				}
				else {
					log.info("Replication 1: Player has Nothing");
					s.updateCall(replNoRes);
					neededMob = mmMob;
					needFire = false;
				}
				log.info("Replication 1: Need to get hit by {}", neededMob);
				// Wait for cleave
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB527));
				// Wait for mobs to position
//				s.waitMs(3_000);
				s.waitThenRefreshCombatants(100);
//				// We want to find the nearest two Lindschrat mobs that are not the exact same mob.
//				List<XivCombatant> neededClones = state.npcsById(19204).stream().filter(mob -> mob != neededMob)
//						.sorted(Comparator.comparing(mob -> mob.getPos().distanceFrom2D(neededMob.getPos())))
//						.limit(2)
//						.toList();
				// We can associate by IDs, which might be better if the jump gets messed up
				// It's something like:
				/*
				[Clone 1a, Clone 1b, Clone 2a..., Clone 4b, Original 1, Original 2, ..., Original 4]
				This, to get from "Original X", we take the index, subtract 8, then multiply by 2.
				 */
				// Collect all the cleaves
//				var newCleaves = s.waitEventsQuickSuccession(8, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4DC));
				// I don't know what this unknown action is but it seems to line up well enough for timing
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4D9));
				s.waitMs(1000);
				XivCombatant innerClone;
				XivCombatant outerClone;
				ArenaSector innerCloneSector;
				ArenaSector outerCloneSector;
				do {
					s.waitThenRefreshCombatants(50);
					List<XivCombatant> lindschrats = state.npcsById(19204);
					int baseIndex = (lindschrats.indexOf(neededMob) - 8) * 2;
					List<XivCombatant> neededClones = Stream.of(lindschrats.get(baseIndex), lindschrats.get(baseIndex + 1)).sorted(Comparator.comparing(clone -> clone.getPos().distanceFrom2D(Position.of2d(100, 100)))).toList();
					innerClone = (neededClones.get(0));
					outerClone = (neededClones.get(1));
					innerCloneSector = tightAp.forCombatant(innerClone);
					outerCloneSector = tightAp.forCombatant(outerClone);
				} while (innerCloneSector.opposite() != outerCloneSector);
				log.info("Replication 1: Inner clone is {}, outer clone is {}", innerClone, outerClone);
				log.info("Replication 1: Inner clone sector is {}, outer clone sector is {}", innerCloneSector, outerCloneSector);
				// For inside, the direction of the cleave is "safe" - if the cleaves are going E/W then the safespot is horizontal
				var innerCloneCard = cleavingHoriz ? innerCloneSector.closest(false, ArenaSector.WEST, ArenaSector.EAST) : innerCloneSector.closest(false, ArenaSector.NORTH, ArenaSector.SOUTH);
				// For outside, it's the opposite, if the cleaves are E/W then the safe spot is N/S
				var outerCloneCard = cleavingHoriz ? outerCloneSector.closest(false, ArenaSector.NORTH, ArenaSector.SOUTH) : outerCloneSector.closest(false, ArenaSector.WEST, ArenaSector.EAST);
				log.info("Replication 1: Inner clone card is {}, outer clone card is {}", innerCloneCard, outerCloneCard);
				if (needFire) {
					s.setParam("fireIn", innerCloneSector);
					s.setParam("fireInCard", innerCloneCard);
					s.setParam("fireOut", outerCloneSector);
					s.setParam("fireOutCard", outerCloneCard);
					s.updateCall(replGetHitByFire);
				}
				else {
					// TODO these only have one set of vars, not the other set
					s.setParam("darkIn", innerCloneSector);
					s.setParam("darkInCard", innerCloneCard);
					s.setParam("darkOut", outerCloneSector);
					s.setParam("darkOutCard", outerCloneCard);
					s.updateCall(replGetHitByDark);
				}

				/*

	private final ModifiableCallout<AbilityCastStart> replGetHitByFire = ModifiableCallout.durationBasedCall("Replication: Get Hit By Fire", "Fire {fireInCard} In, {fireOutCard}{fireOut} Out");
	private final ModifiableCallout<AbilityCastStart> replGetHitByDark = ModifiableCallout.durationBasedCall("Replication: Get Hit By Dark", "Dark {darkInCard} In, {darkOutCard}{darkOut} Out");
				 */

			});

	@NpcCastCallout(0xB520)
	private final ModifiableCallout<AbilityCastStart> doubleSobat = ModifiableCallout.durationBasedCall("Double Sobat", "Tankbuster");
	// TODO make this role based - also make the callout better in general
	@NpcCastCallout(0xB525)
	private final ModifiableCallout<AbilityCastStart> doubleSobatSecond = ModifiableCallout.durationBasedCall("Double Sobat: Second Hit", "Tankbuster");

	private final ModifiableCallout<TetherEvent> stgCloneTetherN = new ModifiableCallout<>("Staging: Tethered to N Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherE = new ModifiableCallout<>("Staging: Tethered to E Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherS = new ModifiableCallout<>("Staging: Tethered to S Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherW = new ModifiableCallout<>("Staging: Tethered to W Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherNW = new ModifiableCallout<>("Staging: Tethered to NW Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherNE = new ModifiableCallout<>("Staging: Tethered to NE Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherSW = new ModifiableCallout<>("Staging: Tethered to SW Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> stgCloneTetherSE = new ModifiableCallout<>("Staging: Tethered to SE Clone", "{tetherFrom} Clone");

	private final ModifiableCallout<?> stgDefamation = new ModifiableCallout<>("Staging: Defamation Locked In", "Defamation");
	private final ModifiableCallout<?> stgProtean = new ModifiableCallout<>("Staging: Protean Locked In", "Protean");
	private final ModifiableCallout<?> stgStack = new ModifiableCallout<>("Staging: Stack Locked In", "Stack On You");
	private final ModifiableCallout<?> stgBoss = new ModifiableCallout<>("Staging: Boss Tether Locked In", "Boss");

	private final ModifiableCallout<?> stgStacks = new ModifiableCallout<>("Staging: Stacks Now", "Light Parties");
	private final ModifiableCallout<?> stgStacksWithCone = new ModifiableCallout<>("Staging: Stacks Now", "Light Parties, Face Out");

	private final ModifiableCallout<AbilityCastStart> netherwrathNear = ModifiableCallout.durationBasedCall("Netherwrath Near", "Near");
	private final ModifiableCallout<AbilityCastStart> netherwrathFar = ModifiableCallout.durationBasedCall("Netherwrath Far", "Far");

	private final ModifiableCallout<?> stgRecollectionMechs = new ModifiableCallout<>("Staging: Reenactment Mechanics Pair", "{['Protean', 'Defamation', 'Stack', 'Boss'][mech1]} {sector1}, {['Protean', 'Defamation', 'Stack', 'Boss'][mech2]} {sector2}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> staging = SqtTemplates.selfManagedMultiInvocation(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4E1),
			(e1, s, inv) -> {
				// This shows up again in later mechs but those can handle it themselves
				if (inv != 0) {
					return;
				}

				// First, you get tethered to your clone
				var playerCloneTethers = s.waitEvents(8, TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isPc));
				TetherEvent myTether = playerCloneTethers.stream().filter(t -> t.getTarget().isThePlayer()).findAny()
						.orElseThrow();
				var tetheredTo = myTether.getTargetMatching(t -> !t.isPc());
				ArenaSector tetherFrom = tightAp.forCombatant(tetheredTo);
				s.setParam("tetheredTo", tetheredTo);
				s.setParam("tetherFrom", tetherFrom);
				switch (tetherFrom) {
					case NORTH -> s.updateCall(stgCloneTetherN, myTether);
					case EAST -> s.updateCall(stgCloneTetherE, myTether);
					case SOUTH -> s.updateCall(stgCloneTetherS, myTether);
					case WEST -> s.updateCall(stgCloneTetherW, myTether);
					case NORTHWEST -> s.updateCall(stgCloneTetherNW, myTether);
					case NORTHEAST -> s.updateCall(stgCloneTetherNE, myTether);
					case SOUTHWEST -> s.updateCall(stgCloneTetherSW, myTether);
					case SOUTHEAST -> s.updateCall(stgCloneTetherSE, myTether);
				}

				// Track the last TetherEvent from each mob. ACT doesn't tell us when a tether is removed, so we just
				// rely on the fact that each mob only has one tether.
				Map<Long, TetherEvent> tetherTracker = new HashMap<>();
				while (true) {
					TetherEvent tether = s.waitEventUntil(
							TetherEvent.class, te -> te.tetherIdMatches(PROTEAN_TETHER, DEFA_TETHER, STACK_TETHER, BOSS_TETHER),
							AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4E3)
					);
					if (tether == null) {
						break;
					}
					tetherTracker.put(tether.getSource().getId(), tether);
				}
				List<TetherEvent> myTethers = tetherTracker.values()
						.stream()
						.filter(tetherEvent -> tetherEvent.getTarget().isThePlayer())
						.toList();
				// No tether = defa
				if (myTethers.isEmpty()) {
					s.updateCall(stgDefamation);
				}
				else for (TetherEvent tether : myTethers) {
					switch ((int) tether.getId()) {
						case PROTEAN_TETHER -> s.updateCall(stgProtean);
						case DEFA_TETHER -> s.updateCall(stgDefamation);
						case STACK_TETHER -> s.updateCall(stgStack);
						case BOSS_TETHER -> s.updateCall(stgBoss);
					}
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4E7));

				if (myTethers.stream().anyMatch(t -> t.getId() == PROTEAN_TETHER)) {
					s.updateCall(stgStacksWithCone);
				}
				else {
					s.updateCall(stgStacks);
				}

				// TODO: near/far? slide 13-14 on https://raidplan.io/plan/hxub7q7ptpzgpq6h
				AbilityCastStart netherWrathCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB52E, 0xB52F));
				if (netherWrathCast.abilityIdMatches(0xB52E)) {
					s.updateCall(netherwrathNear, netherWrathCast);
				}
				else {
					s.updateCall(netherwrathFar, netherWrathCast);
				}
				s.waitMs(2_000);
//				s.waitCastFinished(casts, netherWrathCast);
				// calling the rest of the mechanics is non-trivial since we need to figure out what order they will happen in
				// how is the boss itself factored in here?
				for (int i = 0; i < 4; i++) {
					ArenaSector sector1 = ArenaSector.values()[i];
					ArenaSector sector2 = sector1.opposite();
					// The first clones to go off are N/S. So we start by looking at what player was tethered to that clone,
					// and then what mob the player was tethered to.
					XivCombatant player1 = playerCloneTethers.stream().filter(tether -> tightAp.forCombatant(tether.getSource()) == sector1).map(tether -> tether.getTargetMatching(XivCombatant::isPc)).findAny().orElse(null);
					XivCombatant player2 = playerCloneTethers.stream().filter(tether -> tightAp.forCombatant(tether.getSource()) == sector2).map(tether -> tether.getTargetMatching(XivCombatant::isPc)).findAny().orElse(null);
					TetherEvent tether1 = tetherTracker.values().stream().filter(tether -> tether.eitherTargetMatches(player1)).findAny().orElse(null);
					TetherEvent tether2 = tetherTracker.values().stream().filter(tether -> tether.eitherTargetMatches(player2)).findAny().orElse(null);
					long mech1 = tether1 == null ? 1 : (tether1.getId() == BOSS_TETHER ? 3 : tether1.getId() - PROTEAN_TETHER);
					long mech2 = tether2 == null ? 1 : (tether2.getId() == BOSS_TETHER ? 3 : tether2.getId() - PROTEAN_TETHER);
					s.setParam("mech1", mech1);
					s.setParam("mech2", mech2);
					s.setParam("sector1", sector1);
					s.setParam("sector2", sector2);
					s.updateCall(stgRecollectionMechs);
					// Debounce
					s.waitMs(2_000);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4EE, 0xB4ED, 0xB922, 0xBE5D));
				}
			}
	);

	/**
	 * Based on the list of tethers, compute a map from arena sectors to what player was tethered to an npc in that sector.
	 *
	 * @param tethers The tethers.
	 * @return A map from clone sectors to players.
	 */
	private Map<ArenaSector, XivCombatant> mapTethersToSectors(List<TetherEvent> tethers) {
		Map<ArenaSector, XivCombatant> sectorToPlayer = new EnumMap<>(ArenaSector.class);
		for (TetherEvent tether : tethers) {
			XivCombatant npcTarget = tether.getTargetMatching(cbt -> !cbt.isPc());
			if (npcTarget == null) {
				log.error("Tether {} has no NPC target", tether);
				continue;
			}
			XivCombatant pcTarget = tether.getTargetMatching(XivCombatant::isPc);
			if (pcTarget == null) {
				log.error("Tether {} has no player target", tether);
				continue;
			}
			sectorToPlayer.put(tightAp.forCombatant(npcTarget), pcTarget);
		}
		return sectorToPlayer;

	}

	private final ModifiableCallout<BuffApplied> mutationAlpha = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Alpha", "Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> mutationBeta = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Beta", "Beta").autoIcon();

	private final ModifiableCallout<BuffApplied> mutatingFireDonutAlpha = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Fire + Dynamo, Initial Alpha", "Fire and Donut, {firstSide} Safe").autoIcon();
	private final ModifiableCallout<BuffApplied> mutatingLightningChariotAlpha = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Lightning + Chariot, Initial Alpha", "Lightning and Orb, {firstSide.opposite()} Safe").autoIcon();
	private final ModifiableCallout<BuffApplied> mutatingFireDonutBeta = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Fire + Dynamo, Initial Beta", "Pop Fire and Donut, {firstSide} Safe").autoIcon();
	private final ModifiableCallout<BuffApplied> mutatingLightningChariotBeta = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Lightning + Chariot, Initial Beta", "Pop Lightning and Orb, {firstSide.opposite()} Safe").autoIcon();

	private final ModifiableCallout<BuffApplied> mutatingFireDonutAlpha2 = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Fire + Dynamo: Second Pop, Second Alpha", "{firstSide} Safe").autoIcon()
			.extendedDescription("""
					Please note that this callout and the ones below it are based on your CURRENT alpha/beta debuff, not initial.""");
	private final ModifiableCallout<BuffApplied> mutatingLightningChariotAlpha2 = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Lightning + Chariot, Second Alpha", "{firstSide.opposite()} Safe").autoIcon();
	private final ModifiableCallout<BuffApplied> mutatingFireDonutBeta2 = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Fire + Dynamo, Second Beta", "Pop Fire and Donut, {firstSide} Safe").autoIcon();
	private final ModifiableCallout<BuffApplied> mutatingLightningChariotBeta2 = ModifiableCallout.<BuffApplied>durationBasedCall("Mutation: Lightning + Chariot, Second Beta", "Pop Lightning and Orb, {firstSide.opposite()} Safe").autoIcon();

	private final ModifiableCallout<?> mutatingFirstSafe = new ModifiableCallout<>("Mutation: First Safe", "{firstSafeSide} North/South").autoIcon();
	private final ModifiableCallout<?> mutatingSecondSafe = new ModifiableCallout<>("Mutation: First Safe", "{secondSafeSide} North/South").autoIcon();


	private final ModifiableCallout<AbilityCastStart> netherworldNearAlpha = ModifiableCallout.<AbilityCastStart>durationBasedCall("Mutation: Netherworld Near, Alpha", "Stay Far").statusIcon(0x12A1);
	private final ModifiableCallout<AbilityCastStart> netherworldFarAlpha = ModifiableCallout.<AbilityCastStart>durationBasedCall("Mutation: Netherworld Far, Alpha", "Stay Near").statusIcon(0x12A1);
	private final ModifiableCallout<AbilityCastStart> netherworldNearBeta = ModifiableCallout.<AbilityCastStart>durationBasedCall("Mutation: Netherworld Near, Beta", "Bait Stack Near").statusIcon(0x12A3);
	private final ModifiableCallout<AbilityCastStart> netherworldFarBeta = ModifiableCallout.<AbilityCastStart>durationBasedCall("Mutation: Netherworld Far, Beta", "Bait Stack Far").statusIcon(0x12A3);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mutatingCells = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB505),
			(e1, s) -> {
				log.info("Mutation start");
				var myBuff = s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(0x12A1, 0x12A3) && ba.getTarget().isThePlayer());
				boolean alpha = myBuff.buffIdMatches(0x12A1);
				s.setParam("alpha", alpha);
				log.info("Mutation start: Alpha = {}", alpha);
				s.updateCall(alpha ? mutationAlpha : mutationBeta, myBuff);


				/*
				19205 = initially spawned orb, turns into silver ball
				19206 = blue ball?
				19207 = donut
				19208 = proteans?
				19209 = bowtie

				general plan for callout:
				1. Call out which side is initially safe - the side that does not have chariot + cleave?
					What can the initial combinations be?
					If one of the close mechs is a donut, that side is safe.
					If one of the close mechs is chariot, that side is unsafe.
				2. Call out for people to pop
				3. Wait for debuff swap
				4. Call out for second pop
				5. Call out safe spot
				6. Call out alpha go far/near
				 */

				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB4FD));
				// TODO: play with timing - better yet, loop until the configuration is valid
				s.waitMs(4_000);
				s.waitThenRefreshCombatants(50);
				List<XivCombatant> baseNpcs = state.npcsByIds(19205);
				List<XivCombatant> mechNpcs = state.npcsByIds(19206, 19207, 19208, 19209);
				XivCombatant firstBase = null;
				List<XivCombatant> closeNpcs = new ArrayList<>();
				List<XivCombatant> farNpcs = new ArrayList<>();
				outer:
				for (XivCombatant mechNpc : mechNpcs) {
					for (XivCombatant baseNpc : baseNpcs) {
						if (mechNpc.getPos().distanceFrom2D(baseNpc.getPos()) < 8) {
							closeNpcs.add(mechNpc);
							firstBase = baseNpc;
							continue outer;
						}
					}
					farNpcs.add(mechNpc);
				}
				if (firstBase == null) {
					log.error("Couldn't determine order!");
					return;
				}
				// Which side is doing mechanics first?
				ArenaSector firstSide = tightAp.forCombatant(firstBase);
				s.setParam("firstSide", firstSide);
				boolean isFireDonut = closeNpcs.stream().anyMatch(close -> close.npcIdMatches(19207));
				log.info("isFireDonut: {}", isFireDonut);
				if (isFireDonut) {
					// Donut
					s.updateCall(alpha ? mutatingFireDonutAlpha : mutatingFireDonutBeta, myBuff);
				}
				else {
					// Not donut
					s.updateCall(alpha ? mutatingLightningChariotAlpha : mutatingLightningChariotBeta, myBuff);
				}
				var secondBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x12A1, 0x12A3) && ba.getTarget().isThePlayer());
				boolean secondAlpha = secondBuff.buffIdMatches(0x12A1);
				log.info("secondAlpha: {}", secondAlpha);
				s.setParam("alpha", secondAlpha);
				if (isFireDonut) {
					// Donut
					s.updateCall(secondAlpha ? mutatingFireDonutAlpha2 : mutatingFireDonutBeta2, secondBuff);
				}
				else {
					// Not Donut
					s.updateCall(secondAlpha ? mutatingLightningChariotAlpha2 : mutatingLightningChariotBeta2, secondBuff);
				}
				s.waitMs(6_000);

				ArenaSector firstSafeSide = isFireDonut ? firstSide : firstSide.opposite();
				s.setParam("firstSafeSide", firstSafeSide);
				s.setParam("secondSafeSide", firstSafeSide.opposite());
				s.updateCall(mutatingFirstSafe);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x501, 0xB502, 0xB503, 0xB504));
				s.updateCall(mutatingSecondSafe);

				AbilityCastStart netherworldCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB52B, 0xB52C));
				if (netherworldCast.abilityIdMatches(0xB52B)) {
					// Near
					s.updateCall(secondAlpha ? netherworldNearAlpha : netherworldNearBeta, netherworldCast);
				}
				else {
					// Far
					s.updateCall(secondAlpha ? netherworldFarAlpha : netherworldFarBeta, netherworldCast);
				}

			});

	private final ModifiableCallout<AbilityCastStart> idyllicDream = ModifiableCallout.durationBasedCall("Idyllic Dream", "Raidwide");

	private final ModifiableCallout<?> idyllicCardFirst = new ModifiableCallout<>("Idyllic: Cardinals First", "Cardinals First");
	private final ModifiableCallout<?> idyllicIntercardFirst = new ModifiableCallout<>("Idyllic: Intercardinals First", "Intercards First");

	private final ModifiableCallout<TetherEvent> idyllicCloneTetherN = new ModifiableCallout<>("Idyllic: Tethered to N Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherE = new ModifiableCallout<>("Idyllic: Tethered to E Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherS = new ModifiableCallout<>("Idyllic: Tethered to S Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherW = new ModifiableCallout<>("Idyllic: Tethered to W Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherNW = new ModifiableCallout<>("Idyllic: Tethered to NW Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherNE = new ModifiableCallout<>("Idyllic: Tethered to NE Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherSW = new ModifiableCallout<>("Idyllic: Tethered to SW Clone", "{tetherFrom} Clone");
	private final ModifiableCallout<TetherEvent> idyllicCloneTetherSE = new ModifiableCallout<>("Idyllic: Tethered to SE Clone", "{tetherFrom} Clone");

	private final ModifiableCallout<?> idyllicSafeSpotLaterOut = new ModifiableCallout<>("Idyllic: Safe Spots Stock, Further Out", "Later: {safeSpots} Safe, Out");
	private final ModifiableCallout<?> idyllicSafeSpotLater = new ModifiableCallout<>("Idyllic: Safe Spots Stock, Normal", "Later: {safeSpots} Safe");

	private final ModifiableCallout<?> idyllicDefaFirst = new ModifiableCallout<>("Idyllic: Defamation First", "Defa First").extendedDescription("""
			This tells you whether defamation would be first if tethers are not swapped.""");
	private final ModifiableCallout<?> idyllicStackFirst = new ModifiableCallout<>("Idyllic: Stack First", "Stack First").extendedDescription("""
			This tells you whether stack would be first if tethers are not swapped.""");

	private final ModifiableCallout<?> idyllicSafeSpotOut = new ModifiableCallout<>("Idyllic: Safe Spots Now, Further Out", "{safeSpots} Safe, Out");
	private final ModifiableCallout<?> idyllicSafeSpot = new ModifiableCallout<>("Idyllic: Safe Spots Now, Normal", "{safeSpots} Safe");

	@NpcCastCallout(0xB4F2)
	private final ModifiableCallout<AbilityCastStart> lindwurmMeteor = ModifiableCallout.durationBasedCall("Lindwurm's Meteor", "Raidwide");

	private final ModifiableCallout<?> idyllicNoTether = new ModifiableCallout<>("Idyllic: No Tether (Failed)", "No Tether");
	private final ModifiableCallout<?> idyllicDefaNum = new ModifiableCallout<>("Idyllic: Defa Order (Early Call)", "Defa {myTetherOrder}");
	private final ModifiableCallout<?> idyllicStackNum = new ModifiableCallout<>("Idyllic: Stack Order (Early Call)", "Stack {myTetherOrder}");
	private final ModifiableCallout<?> idyllicDefaNumAgain = new ModifiableCallout<>("Idyllic: Defa Order (Late Call)", "Defa {myTetherOrder}");
	private final ModifiableCallout<?> idyllicStackNumAgain = new ModifiableCallout<>("Idyllic: Stack Order (Late Call)", "Stack {myTetherOrder}");

	@NpcCastCallout(0xB529)
	private final ModifiableCallout<AbilityCastStart> arcadianArcanum = ModifiableCallout.durationBasedCall("Arcadian Arcanum", "Spread");

	private final ModifiableCallout<?> idyllicTakePlainTowerLater = new ModifiableCallout<>("Idyllic: Take Plain Tower Later", "Fire/Earth Tower Later").statusIcon(0x1044);
	private final ModifiableCallout<?> idyllicTakeLightningTowerLater = new ModifiableCallout<>("Idyllic: Take Lightning Tower Later", "Lightning Tower Later");
	private final ModifiableCallout<?> idyllicTakePlainTowerNow = new ModifiableCallout<>("Idyllic: Take Plain Tower", "Fire/Earth Tower").statusIcon(0x1044);
	private final ModifiableCallout<?> idyllicTakeLightningTowerNow = new ModifiableCallout<>("Idyllic: Take Lightning Tower", "Lightning Tower");

	private final ModifiableCallout<?> idyllicBothStacks = new ModifiableCallout<>("Idyllic: Both Stacks", "Stacks on {stackTargets}", "{i}: Stacks on {stackTargets}");
	private final ModifiableCallout<?> idyllicBothDefa = new ModifiableCallout<>("Idyllic: Both Defa", "Defa on {defaTargets}", "{i}: Defas on {defaTargets}");
	private final ModifiableCallout<?> idyllicStackAndDefa = new ModifiableCallout<>("Idyllic: Stack+Defa", "Stack on {stackTarget}, Defa on {defaTarget}", "Stack on {stackTarget}, Defa on {defaTarget}");

	private final ModifiableCallout<BuffApplied> idyllicFarPortent = ModifiableCallout.<BuffApplied>durationBasedCall("Idyllic: Far Portent", "Far Portent").autoIcon();
	private final ModifiableCallout<BuffApplied> idyllicNearPortent = ModifiableCallout.<BuffApplied>durationBasedCall("Idyllic: Near Portent", "Near Portent").autoIcon();
	private final ModifiableCallout<BuffApplied> idyllicPyretic = ModifiableCallout.<BuffApplied>durationBasedCall("Idyllic: Pyretic", "Don't Move").autoIcon();
	private final ModifiableCallout<BuffApplied> idyllicPyreticAfter = new ModifiableCallout<BuffApplied>("Idyllic: Pyretic", "Move").autoIcon();
	private final ModifiableCallout<AbilityCastStart> idyllicStone = ModifiableCallout.<AbilityCastStart>durationBasedCall("Idyllic: Stone Tower", "Out of Tower").autoIcon();

	private final ModifiableCallout<?> idyllicLaterSafePlatform = new ModifiableCallout<>("Idyllic: Later Safe Platform", "Later: {safePlatform} Platform, {safePlatformDirs} Safe").autoIcon();

	private final ModifiableCallout<?> idyllicReenactmentStacks1 = new ModifiableCallout<>("Idyllic: Reenactment Stacks 1", "Stacks {stacksAt}").extendedDescription("""
			You can also use {defasAt} to indicate where the defamations are.""");

	private final ModifiableCallout<?> idyllicSafePlatform = new ModifiableCallout<>("Idyllic: Safe Platform", "{safePlatform} Platform, {safePlatformDirs} Safe").autoIcon();

	private final ModifiableCallout<?> idyllicReenactmentStacks2 = new ModifiableCallout<>("Idyllic: Reenactment Stacks 2", "Stacks {stacksAt}").extendedDescription("""
			You can also use {defasAt} to indicate where the defamations are.""");

	private final ModifiableCallout<?> idyllicPortalSafeDirs = new ModifiableCallout<>("Idyllic: Portal Safe Directions", "{portalSafeDirs} Safe").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> idyllicDreamSq = SqtTemplates.sq(
			240_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB509),
			(e1, s) -> {
				// reference: https://raidplan.io/plan/h5fcfzw229yty84q
				s.updateCall(idyllicDream, e1);
				var firstClone = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.getTarget().npcIdMatches(19210)).getTarget();
				s.waitThenRefreshCombatants(100);
				ArenaSector firstCloneAt = tightAp.forCombatant(state.getLatestCombatantData(firstClone));
				log.info("firstClone: {}", firstClone);
				log.info("firstCloneAt: {}", firstCloneAt);
				boolean cardFirst = firstCloneAt.isCardinal();
				s.updateCall(cardFirst ? idyllicCardFirst : idyllicIntercardFirst);
				var playerCloneTethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.eitherTargetMatches(target -> target.npcIdMatches(19210)));
				TetherEvent myCloneTether = playerCloneTethers.stream().filter(t -> t.getTarget().isThePlayer()).findAny()
						.orElseThrow();
				var tetheredTo = myCloneTether.getTargetMatching(t -> !t.isPc());
				ArenaSector tetherFrom = tightAp.forCombatant(tetheredTo);
				s.setParam("tetheredTo", tetheredTo);
				s.setParam("tetherFrom", tetherFrom);
				// Is the player in the first or second tether group?
				boolean myTetherFirst = tetherFrom.isCardinal() == cardFirst;
				s.setParam("myTetherFirst", myTetherFirst);
				switch (tetherFrom) {
					case NORTH -> s.updateCall(idyllicCloneTetherN, myCloneTether);
					case EAST -> s.updateCall(idyllicCloneTetherE, myCloneTether);
					case SOUTH -> s.updateCall(idyllicCloneTetherS, myCloneTether);
					case WEST -> s.updateCall(idyllicCloneTetherW, myCloneTether);
					case NORTHWEST -> s.updateCall(idyllicCloneTetherNW, myCloneTether);
					case NORTHEAST -> s.updateCall(idyllicCloneTetherNE, myCloneTether);
					case SOUTHWEST -> s.updateCall(idyllicCloneTetherSW, myCloneTether);
					case SOUTHEAST -> s.updateCall(idyllicCloneTetherSE, myCloneTether);
				}
				// We care about Power Gusher B512 x4, and Snaking Kick B511
				// Actually, easier to use B510 (vertical) and B50F (horizontal)?
				var snaking = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB511), false);
				var snakingAt = tightAp.forCombatant(state.getLatestCombatantData(snaking.getTarget()));
				s.setParam("snakingAt", snakingAt);
				var vertGusher = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB510), false);
				var vertGusherAt = tightAp.forCombatant(state.getLatestCombatantData(vertGusher.getTarget()));
				List<ArenaSector> safeSpots = List.of(vertGusherAt.plusEighths(-1), vertGusherAt.plusEighths(1));
				s.setParam("safeArea", vertGusherAt);
				s.setParam("safeSpots", safeSpots);
				// If the snaking is near the vertical gusher, you have to move a bit further out
				boolean safeOut = vertGusherAt == snakingAt;
				s.setParam("safeOut", safeOut);
				s.updateCall(safeOut ? idyllicSafeSpotLaterOut : idyllicSafeSpotLater);

				// There is another one of these - we need to wait
				s.waitEvent(AbilityUsedEvent.class, acs -> acs.abilityIdMatches(0xBBE2));

				Map<Long, TetherEvent> tetherTracker = new HashMap<>();
				// 170 defa
				// 171 stack
				boolean calledInitial = false;
				boolean defamationFirst = false;
				while (true) {
					TetherEvent tether = s.waitEventUntil(
							TetherEvent.class, te -> te.tetherIdMatches(DEFA_TETHER, STACK_TETHER),
							// Locks in after Twisted Vision BBE2 cast
							AbilityUsedEvent.class, acs -> acs.abilityIdMatches(0xBBE2)
					);
					log.info("Processing tether: {}", tether);
					if (tether == null) {
						break;
					}
					tetherTracker.put(tether.getSource().getId(), tether);
					if (!calledInitial) {
						ArenaSector thisTetherFrom = tightAp.forCombatant(state.getLatestCombatantData(tether.getTargetMatching(t -> !t.isPc())));
						log.info("Initial tether: {} from {}", tether, thisTetherFrom);
						boolean tetherIsDefa = tether.tetherIdMatches(DEFA_TETHER);
						// Technically the "first" is N/S, but since the mechanics alternate, that implies it would also have the same mechanic E/W .
						boolean tetherIsFirst = thisTetherFrom.isCardinal();
						log.info("tetherIsDefa: {}, tetherIsFirst: {}, cardFirst: {}", tetherIsDefa, tetherIsFirst, cardFirst);
						defamationFirst = tetherIsFirst == tetherIsDefa;
						log.info("defamationFirst: {}", defamationFirst);
						s.updateCall(defamationFirst ? idyllicDefaFirst : idyllicStackFirst);
						calledInitial = true;
					}
				}
				List<TetherEvent> myTethers = tetherTracker.values()
						.stream()
						.filter(tetherEvent -> tetherEvent.getTarget().isThePlayer())
						.toList();
				Map<ArenaSector, TetherEvent> tetherSectors = new EnumMap<>(ArenaSector.class);
				log.info("Tether tracker");
				tetherTracker.forEach((id, tether) -> {
					log.info("Tether on {}: {}", Long.toString(id, 16), tether);
				});
				tetherTracker.forEach((id, tether) -> tetherSectors.put(tightAp.forCombatant(state.getCombatant(id)), tether));
				Map<XivCombatant, TetherEvent> playerLindschratMap = new HashMap<>();
				tetherTracker.forEach((id, tether) -> {
					XivCombatant pc = tether.getTargetMatching(XivCombatant::isPc);
					if (pc == null) {
						log.error("Tether has no player: {}", tether);
						return;
					}
					playerLindschratMap.put(pc, tether);
				});
				// No tether = defa
				Optional<TetherEvent> myTetherMaybe = myTethers.stream().findAny();
				if (myTetherMaybe.isEmpty()) {
					log.info("No tether");
					s.updateCall(idyllicNoTether);
					return;
				}

				// TODO: timing might be awkward - split into two triggers?
				s.call(safeOut ? idyllicSafeSpotOut : idyllicSafeSpot);

				TetherEvent myMechanicTether = myTetherMaybe.get();
				boolean haveDefa = myTethers.stream().noneMatch(te -> te.tetherIdMatches(STACK_TETHER));
				ArenaSector myTetherSector = tightAp.forCombatant(state.getLatestCombatantData(myMechanicTether.getTargetMatching(te -> !te.isPc())));
				int myTetherOrder = switch (myTetherSector) {
					case NORTH, SOUTH -> 1;
					case NORTHEAST, SOUTHWEST -> 2;
					case EAST, WEST -> 3;
					case NORTHWEST, SOUTHEAST -> 4;
					default -> throw new IllegalStateException("Unexpected value: " + myTetherSector);
				};
//				boolean myTetherCard = myTetherSector.isCardinal();
				s.setParam("myTetherOrder", myTetherOrder);
				s.setParam("haveDefa", haveDefa);
				s.setParam("myTetherSector", myTetherSector);
				s.waitMs(6_000);
				// TODO: re-call this later
				s.updateCall(haveDefa ? idyllicDefaNum : idyllicStackNum);

				s.waitEvents(4, AbilityResolvedEvent.class, aue -> aue.abilityIdMatches(0xB9D9) && aue.isLastTarget());
				s.waitMs(100);
				var myLightResDown = buffs.findStatusOnTarget(state.getPlayer(), 0x1044);
				if (myLightResDown == null) {
					// Take triple tower
					s.updateCall(idyllicTakeLightningTowerLater);
				}
				else {
					// Take plain earth/fire tower
					s.updateCall(idyllicTakePlainTowerLater);
				}

				for (int i = 1; i <= 4; i++) {
					var sector1 = ArenaSector.values()[i - 1];
					var sector2 = sector1.opposite();
					var tether1 = tetherSectors.get(sector1);
					var tether2 = tetherSectors.get(sector2);
					log.info("Tether mechanics {}: {} has {}, {} has {}", i, sector1, tether1.getId(), sector2, tether2.getId());
				}
				s.waitMs(3_000);
				s.updateCall(haveDefa ? idyllicDefaNumAgain : idyllicStackNumAgain);
				s.waitMs(2_000);

				for (int i = 1; i <= 4; i++) {
					s.setParam("i", i);
					var sector1 = ArenaSector.values()[i - 1];
					var sector2 = sector1.opposite();
					var tether1 = tetherSectors.get(sector1);
					var tether2 = tetherSectors.get(sector2);
					if (tether1.getId() == STACK_TETHER && tether2.getId() == STACK_TETHER) {
						// both stacks
						s.setParam("stackTargets", List.of(tether1.getTargetMatching(XivCombatant::isPc), tether2.getTargetMatching(XivCombatant::isPc)));
						s.updateCall(idyllicBothStacks);
					}
					else if (tether1.getId() == DEFA_TETHER && tether2.getId() == DEFA_TETHER) {
						// both defa
						s.setParam("defaTargets", List.of(tether1.getTargetMatching(XivCombatant::isPc), tether2.getTargetMatching(XivCombatant::isPc)));
						s.updateCall(idyllicBothDefa);
					}
					else {
						TetherEvent stackTether;
						TetherEvent defaTether;
						if (tether1.getId() == STACK_TETHER) {
							stackTether = tether1;
							defaTether = tether2;
						}
						else {
							stackTether = tether2;
							defaTether = tether1;
						}
						s.setParam("stackTarget", stackTether.getTargetMatching(XivCombatant::isPc));
						s.setParam("defaTarget", defaTether.getTargetMatching(XivCombatant::isPc));
						s.updateCall(idyllicStackAndDefa);
					}
					log.info("Tether mechanics {}: {} has {}, {} has {}", i, sector1, tether1.getId(), sector2, tether2.getId());
					s.waitMs(1_000);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB518, 0xB519));
				}
				if (myLightResDown == null) {
					// Take triple tower
					s.updateCall(idyllicTakeLightningTowerNow);
				}
				else {
					// Take plain earth/fire tower
					s.updateCall(idyllicTakePlainTowerNow);
				}

				/*
				Now you do the towers for real

				Fire tower: Pyretic (stillness)
				Rock tower: No debuff, but you need to move out of the tower because there is a followup AoE in the tower
				Wind triple tower: Knockup - aim across the arena
				Dark triple tower: Doom bean - aim out

				Next mechanics:
				Near/Far shoots a beam at nearest/farthest.
				You also have close/far tethers.
				 */

				// Redundant?
				int PORTENT = 0x129D;
				int FAR_PORTENT = 0x129E;
				int NEAR_PORTENT = 0x129F;
				int HOT_BLOODED = 0x12A0;

				/*
				stone cast: B4F7 (move out of tower)
				doom laser: B4F6 (inflicts doom D24 for 8 seconds on people)
				jump across is too fast
				 */
				// TODO: could *technically* record the tower positions and have a visual callout to tell you what you're about to get
				var towerCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4F7));
				var myDebuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(FAR_PORTENT, NEAR_PORTENT, HOT_BLOODED));
				if (myDebuff == null) {
					// Got nothing - move out
					s.updateCall(idyllicStone, towerCast);
				}
				else {
					if (myDebuff.buffIdMatches(HOT_BLOODED)) {
						s.updateCall(idyllicPyretic, myDebuff);
						s.waitBuffRemoved(buffs, myDebuff);
						s.updateCall(idyllicPyreticAfter, myDebuff);
					}
					else if (myDebuff.buffIdMatches(FAR_PORTENT)) {
						s.updateCall(idyllicFarPortent, myDebuff);
					}
					else {
						s.updateCall(idyllicNearPortent, myDebuff);
					}
				}

				/*
				One mob teleports to the portal.
				The other two teleport to platforms.
				Later, the two platform mobs come back - one platform will have N/S or E/W cleave, the other will have a chariot.
				Then, the mob jumps out of the portal and does its E/W or N/S cleave.
				 */
				// Only the real cleaver does this.
				var platformCleave = s.findOrWaitForCast(casts, acs -> {
					return acs.abilityIdMatches(0xB50F, 0xB510);
				}, false);
				s.waitThenRefreshCombatants(100);
				var safePlatform = tightAp.forCombatant(state.getLatestCombatantData(platformCleave.getTarget()));
				// B510 is vertical power gusher, so horizontal is safe
				// B50F is horizontal, so north/south is safe
				var platformSafeDirs = platformCleave.abilityIdMatches(0xB50F) ? List.of(ArenaSector.NORTH, ArenaSector.SOUTH) : List.of(ArenaSector.EAST, ArenaSector.WEST);
				s.setParam("safePlatform", safePlatform);
				s.setParam("safePlatformDirs", platformSafeDirs);
				s.updateCall(idyllicLaterSafePlatform);

				/*
				After that, you get reenactment.
				The mobs do the mechanic of what you tethered to, but they do it in card/intercard order


				You'll have two defas and two stacks (most likely but depends on strat)
				 */

				s.waitMs(2_500);

				Map<ArenaSector, XivCombatant> playerCloneTetherMap = mapTethersToSectors(playerCloneTethers);

				{
					List<ArenaSector> sectors = cardFirst ? ArenaSector.cardinals : ArenaSector.quadrants;
					List<ArenaSector> stacksAt = new ArrayList<>();
					List<ArenaSector> defasAt = new ArrayList<>();
					for (ArenaSector sector : sectors) {
						// 1. Look at what player was originally tethered to that sector's player clone
						XivCombatant tetheredPlayer = playerCloneTetherMap.get(sector);
						// 2. Look at what Lindschrat NPC that player grabbed a tether from
						TetherEvent tetherEvent = playerLindschratMap.get(tetheredPlayer);
						// 3. Look at what mechanic that lindschrat was doing
						if (tetherEvent.tetherIdMatches(STACK_TETHER)) {
							stacksAt.add(sector);
						}
						else {
							defasAt.add(sector);
						}
					}
					s.setParam("stacksAt", stacksAt);
					s.setParam("defasAt", defasAt);
					s.updateCall(idyllicReenactmentStacks1);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBE5D, 0xB4EF, 0xB4EE));
				s.updateCall(idyllicSafePlatform);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB514, 0xB515, 0xBE95));
				{
					// Inverted since this is the second set.
					List<ArenaSector> sectors = !cardFirst ? ArenaSector.cardinals : ArenaSector.quadrants;
					List<ArenaSector> stacksAt = new ArrayList<>();
					List<ArenaSector> defasAt = new ArrayList<>();
					for (ArenaSector sector : sectors) {
						// 1. Look at what player was originally tethered to that sector's player clone
						XivCombatant tetheredPlayer = playerCloneTetherMap.get(sector);
						// 2. Look at what Lindschrat NPC that player grabbed a tether from
						TetherEvent tetherEvent = playerLindschratMap.get(tetheredPlayer);
						// 3. Look at what mechanic that lindschrat was doing
						if (tetherEvent.tetherIdMatches(STACK_TETHER)) {
							stacksAt.add(sector);
						}
						else {
							defasAt.add(sector);
						}
					}
					s.setParam("stacksAt", stacksAt);
					s.setParam("defasAt", defasAt);
					s.updateCall(idyllicReenactmentStacks2);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBE5D, 0xB4EF, 0xB4EE));
				var portalSafeDirs = platformSafeDirs.stream().map(as -> as.plusQuads(1)).toList();
				s.setParam("portalSafeDirs", portalSafeDirs);
				s.updateCall(idyllicPortalSafeDirs);
			});

	@NpcCastCallout(0xB533)
	private final ModifiableCallout<AbilityCastStart> arcadianHell = ModifiableCallout.durationBasedCall("Arcadian Hell", "Raidwide");

	@NpcCastCallout(0xB537)
	private final ModifiableCallout<AbilityCastStart> arcadianHellEnrage = ModifiableCallout.durationBasedCall("Arcadian Hell (Enrage)", "Enrage");

}
