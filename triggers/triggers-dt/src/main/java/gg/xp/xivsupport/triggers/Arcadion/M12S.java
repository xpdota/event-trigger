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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
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
import java.util.List;

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

	@NpcCastCallout(0xB4D7)
	private final ModifiableCallout<AbilityCastStart> theFixer = ModifiableCallout.durationBasedCall("The Fixer", "Raidwide");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> mortalSlayerSq = SqtTemplates.sq(
			90_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB495),
			(e1, s) -> {
				// Balls have NPC ID of either 19200 (purple) or 19201 (green)
				log.info("Mortal Slayer Start");
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
				s.updateCall(alpha ? grot2beta : grot2alpha, alphaBeta);
				// Longer delay between debuffs and first in line
				s.waitMs(14_000);
				for (int i = 1; i <= 4; i++) {
					log.info("GT2: i=={}, inLine=={}, soakOrder=={}", i, inLine, soakOrder);
					RawModifiedCallout<?> soakSoonCall = null;
					if (i == soakOrder) {
						soakSoonCall = s.call(soakSoon);
					}
					if (i == inLine) {
						log.info("GT2: {} pop soon", i);
						s.updateCall(alpha ? popSoonAlpha : popSoonBeta, alphaBeta);
					}
					s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(0x1291, 0x1293));
					if (i == inLine) {
						log.info("GT2: {} pop now", i);
						s.updateCall(alpha ? popNowAlpha : popNowBeta, alphaBeta);
					}
					log.info("GT2: {} waiting for tower cast", i);
					var tower = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4B7, 0xB4B3));
					if (i == soakOrder) {
						soakSoonCall.forceExpire();
						s.call(soakNow, tower);
					}
					s.waitMs(500);
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
	private final ModifiableCallout<AbilityCastStart> splattershed = ModifiableCallout.durationBasedCall("Splattershed", "Raidwide");

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
	private final ModifiableCallout<TetherEvent> gt3tetherPartner = new ModifiableCallout<>("Grotesquerie 3: Tether", "Break Tether With {buddy}");

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
					AbilityCastStart cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4A1));
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

				// TODO: tether safe spot

			});

	@NpcCastCallout(0xB4C6)
	private final ModifiableCallout<AbilityCastStart> slaughterShed = ModifiableCallout.durationBasedCall("Slaughtershed", "Raidwide");

	private final ModifiableCallout<HeadMarkerEvent> slaughtershedSpread = new ModifiableCallout<>("End: Spread", "Spread");
	private final ModifiableCallout<?> slaughtershedStack = new ModifiableCallout<>("End: Stack", "Stack on {stackOn}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> slaughter = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4C6),
			(e1, s) -> {
				// 317 is stack, 375 is spread
				List<HeadMarkerEvent> markers = s.waitEvents(5, HeadMarkerEvent.class, hme -> hme.markerIdMatches(317, 375));
				// If player has spread, call that
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


	// PHASE 2

	@NpcCastCallout(0xB528)
	private final ModifiableCallout<AbilityCastStart> arcadiaAflame = ModifiableCallout.durationBasedCall("Arcadia Aflame", "Raidwide");

	@NpcCastCallout(0xB527)
	private final ModifiableCallout<AbilityCastStart> snakingKick = ModifiableCallout.durationBasedCall("Snaking Kick", "Behind");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> replication = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4D8),
			(e1, s) -> {
				log.info("Replication: start");
			});

}
