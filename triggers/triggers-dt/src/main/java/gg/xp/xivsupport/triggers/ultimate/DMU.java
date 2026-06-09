package gg.xp.xivsupport.triggers.ultimate;

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
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
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
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gg.xp.xivsupport.models.ArenaSector.CENTER;
import static gg.xp.xivsupport.models.ArenaSector.EAST;
import static gg.xp.xivsupport.models.ArenaSector.NORTH;
import static gg.xp.xivsupport.models.ArenaSector.NORTHEAST;
import static gg.xp.xivsupport.models.ArenaSector.NORTHWEST;
import static gg.xp.xivsupport.models.ArenaSector.SOUTH;
import static gg.xp.xivsupport.models.ArenaSector.SOUTHEAST;
import static gg.xp.xivsupport.models.ArenaSector.SOUTHWEST;
import static gg.xp.xivsupport.models.ArenaSector.WEST;

@CalloutRepo(name = "DMU Triggers", duty = KnownDuty.DMU)
public class DMU extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(DMU.class);

	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public DMU(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.DMU);
	}

	/*
	673 0x2A1 fake fire spread (should actually stack)
	674 0x2A2 real fire spread (should really spread)
	675 0x2A3 fake ice cleave (go in cones)
	676 0x2A4 real ice cleave (avoid cones)
	677 0x2A5 fake thunder (go in the lines)
	678 0x2A6 real thunder (avoid lines)
	 */

	private static final int FIRE_SPREAD = 127;
	private static final int FIRE_STACK = 128;
	private static final int FAKE_FIRE = 673;
	private static final int REAL_FIRE = 674;
	private static final int FAKE_ICE = 675;
	private static final int REAL_ICE = 676;
	private static final int FAKE_THUNDER = 677;
	private static final int REAL_THUNDER = 678;

	@NpcCastCallout(0xC403)
	private final ModifiableCallout<AbilityCastStart> revoltingRuinIII = ModifiableCallout.durationBasedCall("Revolting Ruin III", "Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> gravenImage = ModifiableCallout.durationBasedCall("Graven Image");

	private final ModifiableCallout<TetherEvent> graven1Tether = new ModifiableCallout<>("Graven Image 1: Tether", "Tether");
	private final ModifiableCallout<?> graven1NoTether = new ModifiableCallout<>("Graven Image 1: No Tether", "No Tether");
//
//	private final ModifiableCallout<HeadMarkerEvent> gravenImageFirstMarker = new ModifiableCallout<>("Graven Image: Initial Marker", "Marker");
//	private final ModifiableCallout<?> gravenImageFirstNoMarker = new ModifiableCallout<>("Graven Image: No Initial Marker", "No Marker");

	private final ModifiableCallout<HeadMarkerEvent> gravenRealIceSpread = new ModifiableCallout<>("Graven Image 1: Real Ice, Spread", "Spread out of Cones");
	private final ModifiableCallout<HeadMarkerEvent> gravenRealIceStack = new ModifiableCallout<>("Graven Image 1: Real Ice, Stack", "Stacks out of Cones");
	private final ModifiableCallout<HeadMarkerEvent> gravenFakeIceSpread = new ModifiableCallout<>("Graven Image 1: Fake Ice, Spread", "Spread in Cones");
	private final ModifiableCallout<HeadMarkerEvent> gravenFakeIceStack = new ModifiableCallout<>("Graven Image 1: Fake Ice, Stack", "Stacks in Cones");

	private final ModifiableCallout<?> gravenSpreadForLaser = new ModifiableCallout<>("Graven Image 1: Spread For Laser", "Line Spread");
	private final ModifiableCallout<AbilityUsedEvent> gravenAvoidTower = new ModifiableCallout<>("Graven Image 1: Got Hit by Laser", "Avoid Tower");
	private final ModifiableCallout<AbilityCastStart> gravenTakeTower = ModifiableCallout.durationBasedCall("Graven Image 1: Take Tower", "Take Tower");

	private final ModifiableCallout<BuffApplied> gravenConfetti = ModifiableCallout.<BuffApplied>durationBasedCall("Graven Image 1: Confetti on You", "Confetti").autoIcon();
	private final ModifiableCallout<BuffApplied> gravenNoConfetti = ModifiableCallout.<BuffApplied>durationBasedCall("Graven Image 1: Confetti on You", "Confetti on {confettiPlayers}");

	private final ModifiableCallout<HeadMarkerEvent> gravenRealIceRealThunder = new ModifiableCallout<>("Graven Image 1: Real Ice, Real Thunder", "Avoid Both");
	private final ModifiableCallout<HeadMarkerEvent> gravenRealIceFakeThunder = new ModifiableCallout<>("Graven Image 1: Real Ice, Fake Thunder", "Out of Cones, In Lines");
	private final ModifiableCallout<HeadMarkerEvent> gravenFakeIceRealThunder = new ModifiableCallout<>("Graven Image 1: Fake Ice, Real Thunder", "In Cones, Out of Lines");
	private final ModifiableCallout<HeadMarkerEvent> gravenFakeIceFakeThunder = new ModifiableCallout<>("Graven Image 1: Fake Ice, Fake Thunder", "Stand in Both");

	private final ModifiableCallout<HeadMarkerEvent> graven2realIceStone = new ModifiableCallout<>("Graven Image 2: Real Ice, Stone", "Avoid Ice, Stone");
	private final ModifiableCallout<HeadMarkerEvent> graven2fakeIceStone = new ModifiableCallout<>("Graven Image 2: Fake Ice, Stone", "Fake Ice, Stone");
	private final ModifiableCallout<HeadMarkerEvent> graven2realIceDark = new ModifiableCallout<>("Graven Image 2: Real Ice, Dark", "Avoid Ice, Dark");
	private final ModifiableCallout<HeadMarkerEvent> graven2fakeIceDark = new ModifiableCallout<>("Graven Image 2: Fake Ice, Dark", "Fake Ice, Dark");

	private final ModifiableCallout<?> graven2dropFirstStone = new ModifiableCallout<>("Graven Image 2: Drop First Stone", "Drop Stone");
	private final ModifiableCallout<?> graven2avoidFirstStone = new ModifiableCallout<>("Graven Image 2: Drop First Stone", "Avoid Stone and Puddle");

	private final ModifiableCallout<?> graven2westSafe1 = new ModifiableCallout<>("Graven Image 2: West Safe", "West Safe");
	private final ModifiableCallout<?> graven2eastSafe1 = new ModifiableCallout<>("Graven Image 2: East Safe", "East Safe");

	private final ModifiableCallout<?> graven2stone2 = new ModifiableCallout<>("Graven Image 2: Second Stone", "Stone");
	private final ModifiableCallout<?> graven2dark2 = new ModifiableCallout<>("Graven Image 2: Second Dark", "Dark");

	private final ModifiableCallout<?> graven2dropSecondStone = new ModifiableCallout<>("Graven Image 2: Drop Second Stone", "Drop Stone");
	private final ModifiableCallout<?> graven2avoidSecondStone = new ModifiableCallout<>("Graven Image 2: Drop Second Stone", "Avoid Stone and Puddle");

	private final ModifiableCallout<BuffApplied> gravenConfetti2 = ModifiableCallout.<BuffApplied>durationBasedCall("Graven Image 1: Confetti on You", "{safeSpot2} Safe, Confetti").autoIcon();
	private final ModifiableCallout<BuffApplied> gravenNoConfetti2 = ModifiableCallout.durationBasedCall("Graven Image 1: Confetti on You", "{safeSpot2} Safe, Confetti on {confettiPlayers}");

	private final ModifiableCallout<?> gravenFinalSoaks = new ModifiableCallout<>("Graven Image 2: Final Soaks", "Final Soaks");
	/*
	Graven 1: https://raidplan.io/plan/wjcpqydbevej4kpk#6https://raidplan.io/plan/wjcpqydbevej4kpk#6
	Four people get tether. All DPS or all support.
	Boss does a two-element mechanic. Can be real or fake.
	Four lasers hit 2 DPS and 2 supports. Hit players need to drop tower which will be soaked by non-hit player.

	2 players receive confetti debuff.

	Confetti debuff is player stack on the debuffed player that will KB them.

	Another 2 elements while confetti going off, but it can be real or fake
	 */

	@AutoFeed
	private final SequentialTrigger<BaseEvent> gravenImageSq = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBCF2),
			(e1, s) -> {
				s.updateCall(gravenImage, e1);
				List<TetherEvent> initialTethers = s.waitEventsQuickSuccession(4, TetherEvent.class, hme -> true);
				Optional<TetherEvent> myTether = initialTethers.stream().filter(t -> t.eitherTargetMatches(XivCombatant::isThePlayer)).findAny();
				if (myTether.isPresent()) {
					s.updateCall(graven1Tether, myTether.get());
				}
				else {
					s.updateCall(graven1NoTether);
				}

				// These two presumably indicate mechanics
				/*
				Example 1: 4:09PM fake ice, spread
					2 double fake

				Example 2: 4:32 fake fake (stack) 675 and 673 on boss, 127 on all players
						Fake ice (BA9E, BA9B), mystery magic BA94
						4s hit with fire BAA3
				2 all real 676 and 678, thunder ba9f, blizzard ba 98

				4:38PM all fake (stand in both and stack) fake spread
					675, 673, 8x 127
				second set 676 678


				fake ice + real spread

				fake lightning

				based on this:
				673 0x2A1 fake fire spread (should actually stack)
				674 0x2A2 real fire spread (should really spread)
				675 0x2A3 fake ice cleave (go in cones)
				676 0x2A4 real ice cleave (avoid cones)
				677 0x2A5 fake thunder (go in the lines)
				678 0x2A6 real thunder (avoid lines)
				 */

				// 4:59PM wrong call - should have been spread
				// 5:13PM ice was right but not stack/spread - players had a stack marker, so it was fake stack i.e. spread
				// so we do need the player HM after all
				// stack is HM 128, spread is 127

				{
					List<HeadMarkerEvent> kafkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					var playerHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.markerIdMatches(127, 128));
					boolean fakeFire = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(673));
					boolean fakeIce = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(675));
					boolean presentSpread = playerHm.markerIdMatches(127);
					boolean actuallySpread = presentSpread != fakeFire;
					s.setParam("fakeFire", fakeFire);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kafkaHM.get(0);
					if (actuallySpread) {
						s.updateCall(fakeIce ? gravenFakeIceSpread : gravenRealIceSpread, hm1);
					}
					else {
						s.updateCall(fakeIce ? gravenFakeIceStack : gravenRealIceStack, hm1);
					}
				}

				s.waitMs(6_000);
				s.updateCall(gravenSpreadForLaser);
				List<AbilityUsedEvent> laserTargets = s.waitEventsQuickSuccession(4,
						AbilityUsedEvent.class,
						aue -> aue.abilityIdMatches(0xBAA8) && aue.isFirstTarget());
				laserTargets.stream().filter(lt -> lt.getTarget().isThePlayer()).findAny().ifPresentOrElse(
						myLaser -> {
							s.updateCall(gravenAvoidTower, myLaser);
						}, () -> {
							var towerCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xBAAA), false);
							s.updateCall(gravenTakeTower, towerCast);
						}
				);
				var confettis = s.waitEventsQuickSuccession(2, BuffApplied.class, ba -> ba.buffIdMatches(0x13D6));
				// TODO: sort this with self first
				var confettiPlayers = confettis.stream().map(BuffApplied::getTarget).toList();
				s.setParam("confettiPlayers", confettiPlayers);
				confettis.stream().filter(cf -> cf.getTarget().isThePlayer()).findAny().ifPresentOrElse(
						myCf -> s.updateCall(gravenConfetti, myCf),
						() -> s.updateCall(gravenNoConfetti, confettis.get(0)));

				{
					List<HeadMarkerEvent> kafkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					boolean fakeThunder = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(677));
					boolean fakeIce = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(675));
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kafkaHM.get(0);
					if (fakeThunder) {
						s.updateCall(fakeIce ? gravenFakeIceFakeThunder : gravenRealIceFakeThunder, hm1);
					}
					else {
						s.updateCall(fakeIce ? gravenFakeIceRealThunder : gravenRealIceRealThunder, hm1);
					}
				}
			}, (e1, s) -> {
				log.info("Graven 2: Start");
				// These carry over from before the cast
				List<BuffApplied> confettis = buffs.findBuffsById(0x13D6);
				s.setParam("confettis", confettis);
				// TODO: sort this with self first
				var confettiPlayers = confettis.stream().map(BuffApplied::getTarget).toList();
				s.setParam("confettiPlayers", confettiPlayers);

				// The problem here is that the tethers come from dummy NPCs which are co-located with a non-combatant NPC which tells us which mechanic is which, but which don't on their
				// own have identifying features.
				boolean playerStone;
				{
					var rawTethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.tetherIdMatches(45));
					s.waitThenRefreshCombatants(100);
					var myTether = rawTethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findAny().orElseThrow();
					var myTetherFrom = state.getLatestCombatantData(myTether.getTargetMatching(cbt -> !cbt.isPc()));
					playerStone = myTetherFrom.getPos().x() > 120;
					s.setParam("playerStone", playerStone);
				}

				// Same fake/real ice
				var bossHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
				if (bossHm.markerIdMatches(FAKE_ICE)) {
					s.updateCall(playerStone ? graven2fakeIceStone : graven2fakeIceDark);
				}
				else {
					s.updateCall(playerStone ? graven2realIceStone : graven2realIceDark);
				}
				// 2 stone players would split off but we need to figure out how to identify stone tethers
				// Then, half room cleave + tankbuster.
				// Half room cleave is which hand up north starts glowing. But what is this? ActorControlExtra?
				// West safe had ACEE 19D 40:80:0:0 on 2015165 @ (116, 43, 6.5)
				// East safe had ACEE 19D 40:80:0:0 on 2015165 @ (92, 27, 15)

				// North is always dark
				// Then, tethers again
				// Then, stack again, but not on the original stack.
				// Stones split off again
				// Finally, confetti stacks resolve in the bad spots

				// Gravitas hits 4 players
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAAC));
				s.updateCall(playerStone ? graven2dropFirstStone : graven2avoidFirstStone);
				// BAB0 vitrophyre hits stone players
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAB0));

				// Buster already handled by another trigger. Ignore it.
				var glowingHand1 = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x40, 0x80, 0, 0));
				s.waitThenRefreshCombatants(100);
				boolean westSafe1 = state.getLatestCombatantData(glowingHand1.getTarget()).getPos().x() > 100;

				s.updateCall(westSafe1 ? graven2westSafe1 : graven2eastSafe1);
				{
					// Tethers again
					var rawTethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.tetherIdMatches(45));
					s.waitThenRefreshCombatants(100);
					var myTether = rawTethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findAny().orElseThrow();
					var myTetherFrom = state.getLatestCombatantData(myTether.getTargetMatching(cbt -> !cbt.isPc()));
					playerStone = myTetherFrom.getPos().x() > 120;
					s.setParam("playerStone", playerStone);
				}
				// No ice with this set
				s.updateCall(playerStone ? graven2stone2 : graven2dark2);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAAC));
				s.updateCall(playerStone ? graven2dropSecondStone : graven2avoidSecondStone);


				var glowingHand2 = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x40, 0x80, 0, 0));
				s.waitThenRefreshCombatants(200);
				boolean westSafe2 = state.getLatestCombatantData(glowingHand2.getTarget()).getPos().x() > 100;

				s.setParam("safeSpot2", westSafe2 ? WEST : ArenaSector.EAST);

				confettis.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(ba -> s.updateCall(gravenConfetti2, ba), () -> {
							s.updateCall(gravenNoConfetti2, confettis.get(0));
						});
				s.waitMs(9_000);
				s.updateCall(gravenFinalSoaks);
			});


	@NpcCastCallout({0xC622, 0xBABD})
	private final ModifiableCallout<AbilityCastStart> lightOfJudgment = ModifiableCallout.durationBasedCall("Light of Judgment", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> ttInitial = ModifiableCallout.durationBasedCall("Tele-trouncing", "Arrows");

	private enum ArrowDirection {
		UP,
		DOWN,
		RIGHT,
		LEFT,
	}

	private final ModifiableCallout<BuffApplied> ttNN = ModifiableCallout.durationBasedCall("TT: Double N", "Double North");
	private final ModifiableCallout<BuffApplied> ttSS = ModifiableCallout.durationBasedCall("TT: Double S", "Double South");
	private final ModifiableCallout<BuffApplied> ttEE = ModifiableCallout.durationBasedCall("TT: Double E", "Double East");
	private final ModifiableCallout<BuffApplied> ttWW = ModifiableCallout.durationBasedCall("TT: Double W", "Double West");

	private final ModifiableCallout<BuffApplied> ttNW = ModifiableCallout.<BuffApplied>durationBasedCall("TT: N -> W", "North West", "North -> West")
			.extendedDescription("""
					Note that these by default call the order in which the arrows will expire, i.e. right-to-left on the HUD.""");
	private final ModifiableCallout<BuffApplied> ttNE = ModifiableCallout.durationBasedCall("TT: N -> E", "North East", "North -> East");
	private final ModifiableCallout<BuffApplied> ttSW = ModifiableCallout.durationBasedCall("TT: S -> W", "South West", "South -> West");
	private final ModifiableCallout<BuffApplied> ttSE = ModifiableCallout.durationBasedCall("TT: S -> E", "South East", "South -> East");
	private final ModifiableCallout<BuffApplied> ttEN = ModifiableCallout.durationBasedCall("TT: E -> N", "East North", "East -> North");
	private final ModifiableCallout<BuffApplied> ttES = ModifiableCallout.durationBasedCall("TT: E -> S", "East South", "East -> South");
	private final ModifiableCallout<BuffApplied> ttWN = ModifiableCallout.durationBasedCall("TT: W -> N", "West North", "West -> North");
	private final ModifiableCallout<BuffApplied> ttWS = ModifiableCallout.durationBasedCall("TT: W -> S", "West South", "West -> South");
	private final ModifiableCallout<BuffApplied> ttError = ModifiableCallout.durationBasedCall("TT: Error", "Error");

	private final ModifiableCallout<BuffApplied> ttConfettiOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Confetti on You", "Confetti on {confettiPlayers}").autoIcon();
	private final ModifiableCallout<BuffApplied> ttConfettiNotOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Confetti not on You", "Confetti on {confettiPlayers}").autoIcon();

	private final ModifiableCallout<TetherEvent> ttStoneTetherInitial = new ModifiableCallout<>("TT: Stone Tether (Initial)", "Stone");
	private final ModifiableCallout<TetherEvent> ttDarkTetherInitial = new ModifiableCallout<>("TT: Dark Tether (Initial)", "Dark");

	private final ModifiableCallout<TetherEvent> ttStoneTether = new ModifiableCallout<>("TT: Stone Tether (After Confetti)", "Spread for Stone");
	private final ModifiableCallout<TetherEvent> ttDarkTether = new ModifiableCallout<>("TT: Dark Tether (After Confetti)", "Spread for Dark");

	private final ModifiableCallout<ActorControlExtraEvent> ttEarlyFakeGaze = new ModifiableCallout<>("TT: Fake Gaze (Early Call)", "Fake Gaze");
	private final ModifiableCallout<ActorControlExtraEvent> ttEarlyRealGaze = new ModifiableCallout<>("TT: Real Gaze (Early Call)", "Real Gaze");
	private final ModifiableCallout<?> ttElementMechanic = new ModifiableCallout<>("TT: Element Mechanics", "{actualSpread ? 'Spread' : 'Stack'} {fakeThunder ? 'In Thunder' : 'In Safe'}, Look {fakeGaze ? 'Towards' : 'Away'}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ttSq = SqtTemplates.sq(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAB9),
			(e1, s) -> {
				s.updateCall(ttInitial, e1);
				// I thought it had a consistent ordering at first, but I think they intentionally try to confuse you
				// by using multiple different versions with different party list ordering.
				var shortBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer()
				                                                 && ba.buffIdMatches(0x130C, 0x130D, 0x130E, 0x130F, 0x13D7, 0x13D8, 0x13D9, 0x13DA)
				                                                 && ba.getInitialDuration().toMillis() < 8_500);
				var longBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer()
				                                                && ba.buffIdMatches(0x130C, 0x130D, 0x130E, 0x130F, 0x13D7, 0x13D8, 0x13D9, 0x13DA)
				                                                && ba.getInitialDuration().toMillis() > 8_500);

				var dir1 = switch ((int) shortBuff.getBuff().getId()) {
					case 0x130C, 0x13D7 -> ArrowDirection.UP;
					case 0x130D, 0x13D8 -> ArrowDirection.DOWN;
					case 0x130E, 0x13D9 -> ArrowDirection.RIGHT;
					case 0x130F, 0x13DA -> ArrowDirection.LEFT;
					default -> {
						log.error("Bad dir1 ID: {}", shortBuff);
						yield null;
					}
				};

				var dir2 = switch ((int) longBuff.getBuff().getId()) {
					case 0x130C, 0x13D7 -> ArrowDirection.UP;
					case 0x130D, 0x13D8 -> ArrowDirection.DOWN;
					case 0x130E, 0x13D9 -> ArrowDirection.RIGHT;
					case 0x130F, 0x13DA -> ArrowDirection.LEFT;
					default -> {
						log.error("Bad dir2 ID: {}", longBuff);
						yield null;
					}
				};

				ModifiableCallout<BuffApplied> call = (dir1 == null || dir2 == null) ? null : switch (dir1) {
					case UP -> switch (dir2) {
						case UP -> ttNN;
						case RIGHT -> ttNE;
						case LEFT -> ttNW;
						default -> null;
					};
					case DOWN -> switch (dir2) {
						case DOWN -> ttSS;
						case RIGHT -> ttSE;
						case LEFT -> ttSW;
						default -> null;
					};
					case RIGHT -> switch (dir2) {
						case RIGHT -> ttEE;
						case UP -> ttEN;
						case DOWN -> ttES;
						default -> null;
					};
					case LEFT -> switch (dir2) {
						case LEFT -> ttWW;
						case UP -> ttWN;
						case DOWN -> ttWS;
						default -> null;
					};
				};
				if (call == null) {
					log.error("TT error: shortBuff {} longBuff {} dir1 {} dir2 {}", shortBuff, longBuff, dir1, dir2);
					s.updateCall(ttError, shortBuff);
				}
				else {
					s.updateCall(call, shortBuff);
				}
				s.waitBuffRemoved(buffs, longBuff);

				// TODO: sort this with self first
				var confettis = buffs.findBuffsById(0x13D6);
				s.setParam("confettis", confettis);
				var confettiPlayers = confettis.stream().map(BuffApplied::getTarget).toList();
				s.setParam("confettiPlayers", confettiPlayers);
				confettis.stream().filter(cf -> cf.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(cf -> s.updateCall(ttConfettiOnYou, cf),
								() -> s.updateCall(ttConfettiNotOnYou, confettis.get(0)));

				boolean playerStone;
				{
					var rawTethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.tetherIdMatches(45));
					s.waitThenRefreshCombatants(100);
					var myTether = rawTethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findAny().orElseThrow();
					var myTetherFrom = state.getLatestCombatantData(myTether.getTargetMatching(cbt -> !cbt.isPc()));
					playerStone = myTetherFrom.getPos().x() > 120;
					s.setParam("playerStone", playerStone);
				}
				// This call will not overwrite the confetti call
				var tetherCall = s.call(playerStone ? ttStoneTetherInitial : ttDarkTetherInitial);
				s.waitBuffRemoved(buffs, confettis.get(0));
				tetherCall.forceExpire();
				s.updateCall(playerStone ? ttStoneTether : ttDarkTether);

				var lookMechanic = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x40, 0x80, 0, 0));
				s.waitThenRefreshCombatants(100);
				var lookFrom = state.getLatestCombatantData(lookMechanic.getTarget());
				boolean fakeGaze = lookFrom.getPos().x() < 100;
				s.setParam("fakeGaze", fakeGaze);
				// This call is also in parallel
				var gazeCall = s.call(fakeGaze ? ttEarlyFakeGaze : ttEarlyRealGaze, lookMechanic);

				{
					List<HeadMarkerEvent> kafkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					var playerHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.markerIdMatches(FIRE_SPREAD, FIRE_STACK));
					boolean fakeFire = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_FIRE));
					boolean fakeThunder = kafkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
					boolean presentSpread = playerHm.markerIdMatches(FIRE_SPREAD);
					boolean actuallySpread = presentSpread != fakeFire;
					s.setParam("fakeFire", fakeFire);
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("actualSpread", actuallySpread);
					gazeCall.forceExpire();
					s.updateCall(ttElementMechanic);
				}

				/*
				Look towards/away:
				towards: ACEE 19D 40:80:0:0 from 2015167 @ (95, 25, 12.5)
				away: ACEE 19D 40:80:0:) from 2015166 @ (105.25, 34, 13.5)
				 */


			});

	@NpcCastCallout(0xC24C)
	private final ModifiableCallout<AbilityCastStart> ultimateEmbrace = ModifiableCallout.durationBasedCall("Ultimate Embrace", "Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> forsaken = ModifiableCallout.durationBasedCall("Forsaken", "Raidwide");

	@PlayerStatusCallout(value = 0x13DB, cancellable = true)
	private final ModifiableCallout<BuffApplied> forsakenDebuffReminder = ModifiableCallout.<BuffApplied>durationBasedCall("Forsaken: Debuff Tracker", "", "{event.stacks} Stacks").autoIcon()
			.extendedDescription("""
					This callout shows only your debuff stacks. You should NOT add TTS to this, because the game continuously refreshes this debuff.""");

	private final ModifiableCallout<?> forsakenFirstCone = new ModifiableCallout<>("Forsaken: Initial Cone", "Cone, {supportsCone ? 'Supports' : 'DPS'} have cone");
	private final ModifiableCallout<?> forsakenFirstCircle = new ModifiableCallout<>("Forsaken: Initial Circle", "Circle, {supportsCone ? 'Supports' : 'DPS'} have cone");
	private final ModifiableCallout<?> forsakenFirstStack = new ModifiableCallout<>("Forsaken: Initial Stack", "Stack, {supportsCone ? 'Supports' : 'DPS'} have cone");
	private final ModifiableCallout<?> forsakenFirstNothing = new ModifiableCallout<>("Forsaken: Initial Nothing", "Error, {supportsCone ? 'Supports' : 'DPS'} have cone");

//	private final ModifiableCallout<?> forsakenFollowupCone = new ModifiableCallout<>("Forsaken: Followup Cone", "Cone");
//	private final ModifiableCallout<?> forsakenFollowupCircle = new ModifiableCallout<>("Forsaken: Followup Circle", "Circle");
//	private final ModifiableCallout<?> forsakenFollowupStack = new ModifiableCallout<>("Forsaken: Followup Stack", "Stack");
//	private final ModifiableCallout<?> forsakenFollowupNothing = new ModifiableCallout<>("Forsaken: Followup Nothing", "Nothing");

	// TODO These seem to fire too quickly after the previous call (forsakenTowerNpPfXyz)
	private final ModifiableCallout<?> forsakenTowerCone = new ModifiableCallout<>("Forsaken: Followup Cone + Past/Future (Tower Call)", "Cone, Baits")
			.extendedDescription("""
					The 'Tower Call' variants of these call remind you of which tower pattern you will need to do. These indicate that the tower set will be accompanied with Past/Future cast bar, while the other four indicate that it will be accompanied with All Things Ending resolving.
					
					In addition, the `towerSet` variable will let you see which set of towers it is (starting at 1 then incrementing each time towers go off). You can use expressions like `{towerSet % 2 == 0 ? 'even' : 'odd'}` to have conditional logic based on whether it is even or odd towers, or for doing things like swap callouts.""");
	private final ModifiableCallout<?> forsakenTowerCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Past/Future (Tower Call)", "Circle, Baits");
	private final ModifiableCallout<?> forsakenTowerStack = new ModifiableCallout<>("Forsaken: Followup Stack + Past/Future (Tower Call)", "Stack, Baits");
	private final ModifiableCallout<?> forsakenTowerNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + Past/Future (Tower Call)", "Nothing, Baits");

	private final ModifiableCallout<?> forsakenTowerNoPfCone = new ModifiableCallout<>("Forsaken: Followup Cone + No Past/Future (Tower Call)", "Cone");
	private final ModifiableCallout<?> forsakenTowerNoPfCircle = new ModifiableCallout<>("Forsaken: Followup Circle + No Past/Future (Tower Call)", "Circle");
	private final ModifiableCallout<?> forsakenTowerNoPfStack = new ModifiableCallout<>("Forsaken: Followup Stack + No Past/Future (Tower Call)", "Stack");
	private final ModifiableCallout<?> forsakenTowerNoPfNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + No Past/Future (Tower Call)", "Nothing");

	private final ModifiableCallout<?> forsakenFollowupPastCone = new ModifiableCallout<>("Forsaken: Followup Cone + Past", "Cone, Bait Between")
			.extendedDescription("""
					This set of eight calls tells you that you need to bait for All Things Ending.""");
	private final ModifiableCallout<?> forsakenFollowupPastCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Past", "Circle, Bait Between");
	private final ModifiableCallout<?> forsakenFollowupPastStack = new ModifiableCallout<>("Forsaken: Followup Stack + Past", "Stack, Bait Between");
	private final ModifiableCallout<?> forsakenFollowupPastNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + Past", "Nothing, Bait Between");

	private final ModifiableCallout<?> forsakenFollowupFutureCone = new ModifiableCallout<>("Forsaken: Followup Cone + Future", "Cone, Bait Away");
	private final ModifiableCallout<?> forsakenFollowupFutureCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Future", "Circle, Bait Away");
	private final ModifiableCallout<?> forsakenFollowupFutureStack = new ModifiableCallout<>("Forsaken: Followup Stack + Future", "Stack, Bait Away");
	private final ModifiableCallout<?> forsakenFollowupFutureNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + Future", "Nothing, Bait Away");

	private final ModifiableCallout<?> forsakenFinalFuture = new ModifiableCallout<>("Forsaken: Final Future Nothing", "Bait Away");
	private final ModifiableCallout<?> forsakenFinalPast = new ModifiableCallout<>("Forsaken: Final Future Nothing", "Bait Between");

	private enum ForsakenMech {
		CONE,
		CIRCLE,
		STACK,
		NONE
	}

	private static ForsakenMech mechForHm(HeadMarkerEvent hm) {
		return switch ((int) hm.getMarkerId()) {
			case 715 -> ForsakenMech.STACK;
			case 716 -> ForsakenMech.CIRCLE;
			case 717 -> ForsakenMech.CONE;
			default -> {
				log.error("Unknown forsaken marker: {}", hm.getMarkerId());
				yield null;
			}
		};
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> forsakenSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBABC),
			(e1, s) -> {
				s.updateCall(forsaken, e1);
				// wtf happens here
				long stack = 715L;
				long circle = 716L;
				long cone = 717L;
				// 715 stack?
				// 716 circle?
				// 717 pizza?
				// Example at 6/7 16:49:52 log
				// 0. set of 8 (2/3/3)
				// - should first call what you have and who has what
				// 1. set of 4 (0/2/2) + past (between)/future (far)
				// - you need to be in "bait pattern" for the tower
				// - wait for towers, then move based on whether it is past or future
				// 2. set of 4 (2/1/1) + all things ending
				// - stand in "stack pattern" in the towers
				// 3. set of 4 (0/2/2) + past/future
				// 4. set of 4 (2/1/1) + all things ending
				// 5. set of 4 (0/2/2) + past/future
				// 6. set of 4 (2/1/1) + all things ending
				// 7. no more markers + past/future
				// 8. no more towers + all things ending resolves
				// All things ending is just the resolving of the baits
				// initial set - call what you have, and which group has what
				{
					var markers = s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hme -> hme.markerIdMatches(stack, circle, cone));
					Map<Long, List<HeadMarkerEvent>> markerMap = markers.stream().collect(Collectors.groupingBy(HeadMarkerEvent::getMarkerId));
					var myMech = markers.stream().filter(hm -> hm.getTarget().isThePlayer()).findAny()
							.map(DMU::mechForHm)
							.orElseGet(() -> {
								log.error("No initial forsaken marker!");
								return ForsakenMech.NONE;
							});
					s.setParam("myMech", myMech);
					long supportsWithCone = markerMap.get(717L).stream().filter(hm -> hm.getTarget() instanceof XivPlayerCharacter pc && pc.getJob().isSupport()).count();
					long dpsWithCone = markerMap.get(717L).size() - supportsWithCone;
					boolean supportsHaveCone = supportsWithCone > dpsWithCone;
					s.setParam("supportsCone", supportsHaveCone);

					s.setParam("stackPlayers", markerMap.get(stack).stream().map(HeadMarkerEvent::getTarget).toList());
					s.setParam("conePlayers", markerMap.get(cone).stream().map(HeadMarkerEvent::getTarget).toList());
					s.setParam("circledPlayers", markerMap.get(circle).stream().map(HeadMarkerEvent::getTarget).toList());
					s.setParam("towerSet", 1);

					s.updateCall(switch (myMech) {
						case NONE -> forsakenFirstNothing;
						case CONE -> forsakenFirstCone;
						case CIRCLE -> forsakenFirstCircle;
						case STACK -> forsakenFirstStack;
					});
				}

				Boolean nextFuture = null;

				for (int i = 2; i <= 8; i++) {
					// the Path of Light - tower resolving
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBABE));
					// Wait for another set of markers
					List<HeadMarkerEvent> markers;
					if (i < 8) {
						markers = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.markerIdMatches(stack, circle, cone));
					}
					else {
						markers = List.of();
					}
					var myMech = markers.stream().filter(hm -> hm.getTarget().isThePlayer()).findAny()
							.map(DMU::mechForHm)
							.orElse(ForsakenMech.NONE);
					// towerSet is which set of towers are about to go off. Initial towers were 1. First towers after that are 2.
					// So even numbers are where we see the past/future cast (and require bait pattern), odd numbers are where we need to first bait the cleaves, and then get in "stack pattern".
					s.setParam("towerSet", i);
					boolean hasPastFuture = i % 2 == 0;
					s.setParam("hasPastFuture", hasPastFuture);
					if (hasPastFuture) {
						// If this is a tower set with past/future, then what we need to do is:
						// First call the "early call bait pattern"
						s.updateCall(switch (myMech) {
							case CONE -> forsakenTowerCone;
							case CIRCLE -> forsakenTowerCircle;
							// Shouldn't actually be possible but leaving just in case
							case STACK -> forsakenTowerStack;
							case NONE -> forsakenTowerNothing;
						});
						// BAD2 future, BAD3 past, BADC All Things Ending
						var pastFuture = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAD2, 0xBAD3));
						// future
						nextFuture = pastFuture.abilityIdMatches(0xBAD2);
					}
					else {
						// If this is a tower set with all things ending, then what we need to do is:
						// First call the correct future/past bait spot
						if (nextFuture) {
							s.updateCall(switch (myMech) {
								case CONE -> forsakenFollowupFutureCone;
								case CIRCLE -> forsakenFollowupFutureCircle;
								case STACK -> forsakenFollowupFutureStack;
								case NONE -> forsakenFollowupFutureNothing;
							});
						}
						else {
							s.updateCall(switch (myMech) {
								case CONE -> forsakenFollowupPastCone;
								case CIRCLE -> forsakenFollowupPastCircle;
								case STACK -> forsakenFollowupPastStack;
								case NONE -> forsakenFollowupPastNothing;
							});
						}
						s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBADC, 0xBADD));
						s.updateCall(switch (myMech) {
							case CONE -> forsakenTowerNoPfCone;
							case CIRCLE -> forsakenTowerNoPfCircle;
							case STACK -> forsakenTowerNoPfStack;
							case NONE -> forsakenTowerNoPfNothing;
						});

					}

				}
				if (nextFuture) {
					s.updateCall(forsakenFinalFuture);
				}
				else {
					s.updateCall(forsakenFinalPast);
				}
			});

	private final ModifiableCallout<AbilityCastStart> trinesInitial = ModifiableCallout.durationBasedCall("Trines (Initial)", "Trines");
	private final ModifiableCallout<AbilityCastStart> wingsOfDestruction = ModifiableCallout.durationBasedCall("Trines: Wings of Destruction 1", "{wingsSafe} Safe");

	private final ArenaPos ap = new ArenaPos(100.0, 100.0, 4.0, 4.0);
	private static final Set<ArenaSector> trinePositions = Collections.unmodifiableSet(EnumSet.of(CENTER, NORTH, SOUTHEAST, NORTHEAST, SOUTH, SOUTHWEST, NORTHWEST));

	private final ModifiableCallout<?> trinesSafe = new ModifiableCallout<>("{bestStart} to {firstTrineLocations}")
			.extendedDescription("""
					This call will provide a starting position (prefers center if available, else one that is adjacent to one or more safe spots) and all of
					the safe spots.""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinesSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBADF),
			(e1, s) -> {
				s.updateCall(trinesInitial, e1);
				// First set of trines falls first, before cast
				// observed positions:
				/*
				102.89, 85.00 (N)
				111.55, 110.00 (SE)
				97.11, 115.00 (S)
				85.57, 105.00 (SW)
				88.45, 90.00 (NW)
				100.0, 100.0 (Center)
				114.43, 95 (NE)
				 */

				var firstSet = s.waitEventsQuickSuccession(3, ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x10, 0x20, 0, 0));
				// We have to do this every time because it re-uses the entities
				s.waitThenRefreshCombatants(100);
				var firstTrineLocations = firstSet.stream().map(e -> state.getLatestCombatantData(e.getTarget())).map(ap::forCombatant).toList();
				s.setParam("firstTrineLocations", firstTrineLocations);
				var wings1 = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBACE, 0xBACD), false);
				// Half room cleave - BACE = cleaving relative right, so safe spot is relative left
				var wingsSafe = ArenaPos.combatantFacing(wings1.getLocationInfo().getBestHeading()).plusQuads(wings1.abilityIdMatches(0xBACE) ? -1 : 1);
				s.setParam("wingsSafe", wingsSafe);
				s.updateCall(wingsOfDestruction, wings1);
				// We want to find a place where the first and last set were next to each other
				// Second set we don't directly care about - but there's no reason to wait for the third set here, when we know that the third set will consist of anything not hit by first or second sets.
				var secondSet = s.waitEventsQuickSuccession(3, ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x10, 0x20, 0, 0));
				s.waitThenRefreshCombatants(100);
				var secondTrineLocations = secondSet.stream().map(e -> state.getLatestCombatantData(e.getTarget())).map(ap::forCombatant).toList();
				s.setParam("secondTrineLocations", secondTrineLocations);

				var thirdTrineLocations = EnumSet.copyOf(trinePositions);
				thirdTrineLocations.removeAll(firstTrineLocations);
				thirdTrineLocations.removeAll(secondTrineLocations);
				s.setParam("thirdTrineLocations", thirdTrineLocations.stream().toList());
				// Try to figure out a reasonable starting location
				ArenaSector bestStart;
				// TODO: it's probably always center
				if (thirdTrineLocations.contains(CENTER)) {
					bestStart = CENTER;
				}
				else {
					bestStart = thirdTrineLocations.stream().filter(candidate -> {
						if (candidate.isCardinal()) {
							// If cardinal, then the good spots are the ones next to it (e.g. N -> NE)
							return firstTrineLocations.stream().anyMatch(ft -> ft.isStrictlyAdjacentTo(candidate));
						}
						else {
							// If intercard, then the good spots are adjacent cardinals (e.g. NE -> N) and the same-side intercard (NE -> SE)
							return firstTrineLocations.stream().anyMatch(ft -> {
								if (ft.isCardinal()) {
									return ft.isStrictlyAdjacentTo(candidate);
								}
								else {
									return (ft.isStrictlyAdjacentTo(WEST) && candidate.isStrictlyAdjacentTo(WEST))
									       || (ft.isStrictlyAdjacentTo(EAST) && candidate.isStrictlyAdjacentTo(EAST));
								}
							});
						}
					}).findFirst().orElseGet(() -> {
						log.error("Could not find starting candidate! first: {}, second: {}, third: {}", firstTrineLocations, secondTrineLocations, thirdTrineLocations);
						return null;
					});
				}
				s.setParam("bestStart", bestStart);
				s.updateCall(trinesSafe);

			});
	@NpcCastCallout(0xBAE1)
	private final ModifiableCallout<AbilityCastStart> lightOfJudgmentEnrage = ModifiableCallout.durationBasedCall("Failed P2 Enrage", "Failed");

	@NpcCastCallout(0xC3F7)
	private final ModifiableCallout<AbilityCastStart> aeroIIIAssault = ModifiableCallout.durationBasedCall("Aero III Assault", "Knockback");

//	private final ModifiableCallout<AbilityCastStart> bowelsInitial = ModifiableCallout.durationBasedCall("Bowels of Agony", "Raidwide");
//
//	private static final int ENTROPY = 0x640;
//	private static final int DYNAMIC = 0x641;
//	private static final int HEADWIND = 0x642;
//	private static final int TAILWIND = 0x643;
//
//
//	private final ModifiableCallout<BuffApplied> bowelsHeadwind = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind Only", "Headwind").statusIcon(HEADWIND);
//	private final ModifiableCallout<BuffApplied> bowelsTailwind = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind Only", "Tailwind").statusIcon(TAILWIND);
//	private final ModifiableCallout<BuffApplied> bowelsHeadwindEntropy = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind + Entropy", "Headwind and Entropy").statusIcon(ENTROPY);
//	private final ModifiableCallout<BuffApplied> bowelsTailwindEntropy = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind + Entropy", "Tailwind and Entropy").statusIcon(ENTROPY);
//	private final ModifiableCallout<BuffApplied> bowelsHeadwindDynamic = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind + Dynamic Fluid", "Headwind and Dynamic Fluid").statusIcon(DYNAMIC);
//	private final ModifiableCallout<BuffApplied> bowelsTailwindDynamic = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind + Dynamic Fluid", "Tailwind and Dynamic Fluid").statusIcon(DYNAMIC);
//
//	@AutoFeed
//	private final SequentialTrigger<BaseEvent> bowelsSq = SqtTemplates.sq(180_000,
//			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAF2),
//			(e1, s) -> {
//				s.updateCall(bowelsInitial, e1);
//				var allBuffs = s.waitEventsQuickSuccession(12, BuffApplied.class, ba -> ba.buffIdMatches(ENTROPY, DYNAMIC, HEADWIND, TAILWIND));
//				XivPlayerCharacter player = state.getPlayer();
//				var myEntropy = buffs.findStatusOnTarget(player, ENTROPY);
//				var myDynamic = buffs.findStatusOnTarget(player, DYNAMIC);
//				var myHeadwind = buffs.findStatusOnTarget(player, HEADWIND);
//				var myTailwind = buffs.findStatusOnTarget(player, TAILWIND);
//				if (myHeadwind != null) {
//					if (myEntropy != null) {
//						s.updateCall(bowelsHeadwindEntropy, myEntropy);
//					}
//					else if (myDynamic != null) {
//						s.updateCall(bowelsHeadwindDynamic, myDynamic);
//					}
//					else {
//						s.updateCall(bowelsHeadwind, myHeadwind);
//					}
//
//				}
//				else if (myTailwind != null) {
//					if (myEntropy != null) {
//						s.updateCall(bowelsTailwindEntropy, myEntropy);
//					}
//					else if (myDynamic != null) {
//						s.updateCall(bowelsTailwindDynamic, myDynamic);
//					}
//					else {
//						s.updateCall(bowelsTailwind, myTailwind);
//					}
//				}
//				else {
//					log.error("bowels error: no head/tail");
//				}
//
//			});

	/*
	Limit cut:
	1-8 numbers
	Players go to inter-intercards?
	 */

	// P4
	/*
	Agony:
	Debuff players are one LP, non-debuff is another LP
	 */
}
