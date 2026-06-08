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
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

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
				// Then, half room cleave + tankbuster. TODO: how to identify half room cleave safe spot?
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

				s.setParam("safeSpot2", westSafe2 ? ArenaSector.WEST : ArenaSector.EAST);

				confettis.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(ba -> s.updateCall(gravenConfetti2, ba), () -> {
							s.updateCall(gravenNoConfetti2, confettis.get(0));
						});
				s.waitMs(9_000);
				s.updateCall(gravenFinalSoaks);
				// TOOD: this was completely broken - first safe call was called wrong, second delayed everything until after the mechanic
			});


	@NpcCastCallout(0xC622)
	private final ModifiableCallout<AbilityCastStart> lightOfJudgment = ModifiableCallout.durationBasedCall("Light of Judgment", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> teleTrouncing = ModifiableCallout.durationBasedCall("Tele-trouncing", "Arrows");

	private enum ArrowDirection {
		UP,
		DOWN,
		RIGHT,
		LEFT,
	}

	private final ModifiableCallout<BuffApplied> teleTrouncingNN = ModifiableCallout.durationBasedCall("TT: Double N", "Double North");
	private final ModifiableCallout<BuffApplied> teleTrouncingSS = ModifiableCallout.durationBasedCall("TT: Double S", "Double South");
	private final ModifiableCallout<BuffApplied> teleTrouncingEE = ModifiableCallout.durationBasedCall("TT: Double E", "Double East");
	private final ModifiableCallout<BuffApplied> teleTrouncingWW = ModifiableCallout.durationBasedCall("TT: Double W", "Double West");

	// TODO: these call "North Dash West" in TTS
	private final ModifiableCallout<BuffApplied> teleTrouncingNW = ModifiableCallout.durationBasedCall("TT: N -> W", "North -> West");
	private final ModifiableCallout<BuffApplied> teleTrouncingNE = ModifiableCallout.durationBasedCall("TT: N -> E", "North -> East");
	private final ModifiableCallout<BuffApplied> teleTrouncingSW = ModifiableCallout.durationBasedCall("TT: S -> W", "South -> West");
	private final ModifiableCallout<BuffApplied> teleTrouncingSE = ModifiableCallout.durationBasedCall("TT: S -> E", "South -> East");
	private final ModifiableCallout<BuffApplied> teleTrouncingEN = ModifiableCallout.durationBasedCall("TT: E -> N", "East -> North");
	private final ModifiableCallout<BuffApplied> teleTrouncingES = ModifiableCallout.durationBasedCall("TT: E -> S", "East -> South");
	private final ModifiableCallout<BuffApplied> teleTrouncingWN = ModifiableCallout.durationBasedCall("TT: W -> N", "West -> North");
	private final ModifiableCallout<BuffApplied> teleTrouncingWS = ModifiableCallout.durationBasedCall("TT: W -> S", "West -> South");
	private final ModifiableCallout<BuffApplied> teleTrouncingError = ModifiableCallout.durationBasedCall("TT: Error", "Error");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> teleTrouncingSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAB9),
			(e1, s) -> {
				s.updateCall(teleTrouncing, e1);
				// I think they have two of each to be able to force ordering?
				// These are the ones that appear left on the party list
				var buff1 = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer()
				                                             && ba.buffIdMatches(0x130C, 0x130D, 0x130E, 0x130F));
				// These are the ones right on the party list
				var buff2 = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer()
				                                             && ba.buffIdMatches(0x13D7, 0x13D8, 0x13D9, 0x13DA));

				var dir1 = switch ((int) buff1.getBuff().getId()) {
					case 0x130C -> ArrowDirection.UP;
					case 0x130D -> ArrowDirection.DOWN;
					case 0x130E -> ArrowDirection.RIGHT;
					case 0x130F -> ArrowDirection.LEFT;
					default -> throw new RuntimeException("Bad dir1 ID: " + buff1);
				};

				var dir2 = switch ((int) buff2.getBuff().getId()) {
					case 0x13D7 -> ArrowDirection.UP;
					case 0x13D8 -> ArrowDirection.DOWN;
					case 0x13D9 -> ArrowDirection.RIGHT;
					case 0x13DA -> ArrowDirection.LEFT;
					default -> throw new RuntimeException("Bad dir2 ID: " + buff2);
				};

				ModifiableCallout<BuffApplied> call = switch (dir1) {
					case UP -> switch (dir2) {
						case UP -> teleTrouncingNN;
						case RIGHT -> teleTrouncingNE;
						case LEFT -> teleTrouncingNW;
						default -> null;
					};
					case DOWN -> switch (dir2) {
						case DOWN -> teleTrouncingSS;
						case RIGHT -> teleTrouncingSE;
						case LEFT -> teleTrouncingSW;
						default -> null;
					};
					case RIGHT -> switch (dir2) {
						case RIGHT -> teleTrouncingEE;
						case UP -> teleTrouncingEN;
						case DOWN -> teleTrouncingES;
						default -> null;
					};
					case LEFT -> switch (dir2) {
						case LEFT -> teleTrouncingWW;
						case UP -> teleTrouncingWN;
						case DOWN -> teleTrouncingWS;
						default -> null;
					};
				};
				if (call == null) {
					log.error("TT error: buff1 {} buff2 {} dir1 {} dir2 {}", buff1, buff2, dir1, dir2);
					s.updateCall(teleTrouncingError, buff1);
					return;
				}
				s.updateCall(call, buff1);

				// TODO: confetti call

				// Dark/Stone Tethers again
				// TODO: look away/at
				// TODO: real/fake lightning?

			});

	@NpcCastCallout(0xC24C)
	private final ModifiableCallout<AbilityCastStart> ultimateEmbrace = ModifiableCallout.durationBasedCall("Ultimate Embrace", "Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> forsaken = ModifiableCallout.durationBasedCall("Forsaken");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> forsakenSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBABC),
			(e1, s) -> {
				s.updateCall(forsaken, e1);
				// wtf happens here
				int stack = 715;
				int circle = 716;
				int pizza = 717;
				// 715 stack?
				// 716 circle?
				// 717 pizza?
				// set of 8 (2/3/3)
				// set of 4 (0/2/2)
				// set of 4 (2/1/1)
				// set of 4 (0/2/2)
				// set of 4 (2/1/1)
				// set of 4 (0/2/2)
				// set of 4 (2/1/1)
				for (int i = 0; i < 8; i++) {

				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinesSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBADF), // TODO
			(e1, s) -> {
			});

	@NpcCastCallout(0xC3F7)
	private final ModifiableCallout<AbilityCastStart> aeroIIIAssault = ModifiableCallout.durationBasedCall("Aero III Assault", "Knockback");

	// Buster hits highest then second highest aggro, headmarker on first one

	// P4
	/*
	Agony:
	Debuff players are one LP, non-debuff is another LP
	 */
}
