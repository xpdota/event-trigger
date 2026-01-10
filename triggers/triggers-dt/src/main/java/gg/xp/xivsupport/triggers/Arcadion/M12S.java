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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private final ModifiableCallout<TetherEvent> gt3tetherPartner = new ModifiableCallout<>("Grotesquerie 3: Tether", "Spread, Break Tether With {buddy}");

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

	@NpcCastCallout(0xB538)
	private final ModifiableCallout<AbilityCastStart> refreshingOverkill = ModifiableCallout.durationBasedCall("Refreshing Overkill", "Enrage");

	// PHASE 2

	@NpcCastCallout(0xB528)
	private final ModifiableCallout<AbilityCastStart> arcadiaAflame = ModifiableCallout.durationBasedCall("Arcadia Aflame", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> replHorizontalCleavingHorizontal = ModifiableCallout.durationBasedCall("Replication: Horizonal, Cleaving Horizontal");
	private final ModifiableCallout<AbilityCastStart> replHorizontalCleavingVertical = ModifiableCallout.durationBasedCall("Replication: Horizontal, Cleaving Vertical");
	private final ModifiableCallout<AbilityCastStart> replVerticalCleavingHorizontal = ModifiableCallout.durationBasedCall("Replication: Vertical, Cleaving Horizontal");
	private final ModifiableCallout<AbilityCastStart> replVerticalCleavingVertical = ModifiableCallout.durationBasedCall("Replication: Vertical, Cleaving Verical");

	private final ModifiableCallout<BuffApplied> replFireRes = ModifiableCallout.<BuffApplied>durationBasedCall("Replication: Fire Res Down", "Fire Res").autoIcon();
	private final ModifiableCallout<BuffApplied> replDarkRes = ModifiableCallout.<BuffApplied>durationBasedCall("Replication: Dark Res Down", "Dark Res").autoIcon();
	private final ModifiableCallout<BuffApplied> replNoRes = new ModifiableCallout<>("Replication: No Res Down", "Nothing");

	@NpcCastCallout(0xB527)
	private final ModifiableCallout<AbilityCastStart> snakingKick = ModifiableCallout.durationBasedCall("Snaking Kick", "Behind");

	// TODO add description
	private final ModifiableCallout<AbilityCastStart> replGetHitByFire = ModifiableCallout.durationBasedCall("Replication: Get Hit By Fire", "Stack {fireIn} In, {fireOutCard}{fireOut} Out");
	private final ModifiableCallout<AbilityCastStart> replGetHitByDark = ModifiableCallout.durationBasedCall("Replication: Get Hit By Dark", "Spread {darkIn} In, {darkOutCard}{darkOut} Out");

	private final ArenaPos tightAp = new ArenaPos(100, 100, 4, 4);

//	@AutoFeed
	private final SequentialTrigger<BaseEvent> replication = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4D8),
			(e1, s) -> {
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
				boolean fromHoriz = cleavingFrom == ArenaSector.NORTH || cleavingFrom == ArenaSector.SOUTH;
				boolean cleavingHoriz = cleavingTo == ArenaSector.NORTH || cleavingTo == ArenaSector.SOUTH;
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

				// Wait because we don't want to talk over the snaking call
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
				 */
				List<XivCombatant> lindschrats = state.npcsById(19204);
				int baseIndex = lindschrats.indexOf(neededMob) - 8;
				List<XivCombatant> neededClones = Stream.of(lindschrats.get(baseIndex), lindschrats.get(baseIndex + 1)).sorted(Comparator.comparing(clone -> clone.getPos().distanceFrom2D(Position.of2d(100, 100)))).toList();
				// Collect all the cleaves
				var newCleaves = s.waitEventsQuickSuccession(8, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4DC));
				s.waitThenRefreshCombatants(50);
				var innerClone = neededClones.get(0);
				var outerClone = neededClones.get(1);
				var innerCloneSector = tightAp.forCombatant(innerClone);
				var outerCloneSector = tightAp.forCombatant(outerClone);
				log.info("Replication 1: Inner clone is {}, outer clone is {}", innerClone, outerClone);
				log.info("Replication 1: Inner clone sector is {}, outer clone sector is {}", innerCloneSector, outerCloneSector);
				var innerCloneCard = cleavingHoriz ? innerCloneSector.closest(false, ArenaSector.WEST, ArenaSector.EAST) : innerCloneSector.closest(false, ArenaSector.NORTH, ArenaSector.SOUTH);
				var outerCloneCard = cleavingHoriz ? outerCloneSector.closest(false, ArenaSector.WEST, ArenaSector.EAST) : outerCloneSector.closest(false, ArenaSector.NORTH, ArenaSector.SOUTH);
				log.info("Replication 1: Inner clone card is {}, outer clone card is {}", innerCloneCard, outerCloneCard);
				if (needFire) {
					s.setParam("fireIn", innerCloneSector);
					s.setParam("fireInCard", innerCloneCard);
					s.setParam("fireOut", outerCloneSector);
					s.setParam("fireOutCard", outerCloneCard);
					s.updateCall(replGetHitByFire, newCleaves.get(0));
				}
				else {
					// TODO these only have one set of vars, not the other set
					s.setParam("darkIn", innerCloneSector);
					s.setParam("darkInCard", innerCloneCard);
					s.setParam("darkOut", outerCloneSector);
					s.setParam("darkOutCard", outerCloneCard);
					s.updateCall(replGetHitByDark, newCleaves.get(0));
				}

				/*

	private final ModifiableCallout<AbilityCastStart> replGetHitByFire = ModifiableCallout.durationBasedCall("Replication: Get Hit By Fire", "Fire {fireInCard} In, {fireOutCard}{fireOut} Out");
	private final ModifiableCallout<AbilityCastStart> replGetHitByDark = ModifiableCallout.durationBasedCall("Replication: Get Hit By Dark", "Dark {darkInCard} In, {darkOutCard}{darkOut} Out");
				 */

			});

	@NpcCastCallout(0xB520)
	private final ModifiableCallout<AbilityCastStart> doubleSobat = ModifiableCallout.durationBasedCall("Double Sobat", "Tankbuster");
	// TODO make this role based
	@NpcCastCallout(0xB525)
	private final ModifiableCallout<AbilityCastStart> doubleSobatSecond = ModifiableCallout.durationBasedCall("Double Sobat: Second Hit", "Tankbuster");

	private final ModifiableCallout<TetherEvent> cloneTetherN = new ModifiableCallout<>("Staging: Tethered to N Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherE = new ModifiableCallout<>("Staging: Tethered to E Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherS = new ModifiableCallout<>("Staging: Tethered to S Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherW = new ModifiableCallout<>("Staging: Tethered to W Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherNW = new ModifiableCallout<>("Staging: Tethered to NW Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherNE = new ModifiableCallout<>("Staging: Tethered to NE Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherSW = new ModifiableCallout<>("Staging: Tethered to SW Clone", "{tetheredTo} Clone");
	private final ModifiableCallout<TetherEvent> cloneTetherSE = new ModifiableCallout<>("Staging: Tethered to SE Clone", "{tetheredTo} Clone");

	private final ModifiableCallout<?> defamation = new ModifiableCallout<>("Staging: Defamation Locked In", "Defamation");
	private final ModifiableCallout<?> protean = new ModifiableCallout<>("Staging: Protean Locked In", "Protean");
	private final ModifiableCallout<?> stack = new ModifiableCallout<>("Staging: Stack Locked In", "Stack");
	private final ModifiableCallout<?> boss = new ModifiableCallout<>("Staging: Boss Tether Locked In", "Boss");

//	@AutoFeed
	private final SequentialTrigger<BaseEvent> staging = SqtTemplates.sq(
			120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB4E1),
			(e1, s) -> {

				// First, you get tethered to your clone
				var allCloneTethers = s.waitEvents(8, TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isPc));
				TetherEvent myTether = allCloneTethers.stream().filter(t -> t.getTarget().isThePlayer()).findAny()
						.orElseThrow();
				var tetheredTo = myTether.getTargetMatching(t -> !t.isPc());
				ArenaSector tetherFrom = tightAp.forCombatant(tetheredTo);
				switch (tetherFrom) {
					case NORTH -> s.updateCall(cloneTetherN, myTether);
					case EAST -> s.updateCall(cloneTetherE, myTether);
					case SOUTH -> s.updateCall(cloneTetherS, myTether);
					case WEST -> s.updateCall(cloneTetherW, myTether);
					case NORTHWEST -> s.updateCall(cloneTetherNW, myTether);
					case NORTHEAST -> s.updateCall(cloneTetherNE, myTether);
					case SOUTHWEST -> s.updateCall(cloneTetherSW, myTether);
					case SOUTHEAST -> s.updateCall(cloneTetherSE, myTether);
				}

				// Track the last TetherEvent from each mob
				Map<Long, TetherEvent> tetherTracker = new HashMap<>();
				// 16F triangle
				// 170 defa
				// 171 stack
				// 176 boss
				while (true) {
					TetherEvent tether = s.waitEventUntil(
							TetherEvent.class, te -> te.tetherIdMatches(0x16F, 0x170, 0x171, 0x176),
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
					s.updateCall(defamation);
				}
				else for (TetherEvent tether : myTethers) {
					switch ((int) tether.getId()) {
						case 0x16F -> s.updateCall(protean);
						case 0x170 -> s.updateCall(defamation);
						case 0x171 -> s.updateCall(stack);
						case 0x176 -> s.updateCall(boss);
					}
				}
				// Later, call when the stack needs to actually happen


			}
	);


}
