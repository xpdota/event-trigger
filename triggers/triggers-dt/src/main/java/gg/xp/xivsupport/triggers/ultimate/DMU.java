package gg.xp.xivsupport.triggers.ultimate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectCurrentStatus;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerConcurrencyMode;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gg.xp.xivsupport.models.ArenaSector.CENTER;
import static gg.xp.xivsupport.models.ArenaSector.EAST;
import static gg.xp.xivsupport.models.ArenaSector.NORTH;
import static gg.xp.xivsupport.models.ArenaSector.NORTHEAST;
import static gg.xp.xivsupport.models.ArenaSector.NORTHWEST;
import static gg.xp.xivsupport.models.ArenaSector.SOUTH;
import static gg.xp.xivsupport.models.ArenaSector.SOUTHEAST;
import static gg.xp.xivsupport.models.ArenaSector.SOUTHWEST;
import static gg.xp.xivsupport.models.ArenaSector.WEST;

/*
TODO: timeline issue
Gets stuck on "Blizzard III Blowout?", but not really?
Seems like it just never picks up another sync.
But oddly, in debug mode, the timeline overlay is full of that same entry, but in newer -> older order?
There's definitely a timeline processing and/or display bug because that should never happen.
There *could* also be an issue with the timeline itself but idk.
 */
@CalloutRepo(name = "DMU Triggers", duty = KnownDuty.DMU)
public class DMU extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(DMU.class);

	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	private EnumSetting<CleanseCallOption> cleanseCallSetting;

	public DMU(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs, PersistenceProvider pers) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
		String settingsBase = "triggers.dmu.";
		cleanseCallSetting = new EnumSetting<>(pers, settingsBase + "cleanse-call-setting", CleanseCallOption.class, CleanseCallOption.PRIOR_SET);
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
	// TODO: since everyone seems to conga this anyway, can look at X positions
	private final ModifiableCallout<AbilityCastStart> gravenTakeTower = ModifiableCallout.durationBasedCall("Graven Image 1: Take Tower", "Take Tower");

	private final ModifiableCallout<BuffApplied> gravenConfetti = ModifiableCallout.<BuffApplied>durationBasedCall("Graven Image 1: Confetti on You", "Confetti").autoIcon();
	private final ModifiableCallout<BuffApplied> gravenNoConfetti = ModifiableCallout.durationBasedCall("Graven Image 1: Confetti on You", "Confetti on {confettiPlayers}");

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
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					var playerHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.markerIdMatches(127, 128));
					boolean fakeFire = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(673));
					boolean fakeIce = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(675));
					boolean presentSpread = playerHm.markerIdMatches(127);
					boolean actuallySpread = presentSpread != fakeFire;
					s.setParam("fakeFire", fakeFire);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kefkaHM.get(0);
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
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					boolean fakeThunder = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(677));
					boolean fakeIce = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(675));
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kefkaHM.get(0);
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

				s.setParam("safeSpot2", westSafe2 ? WEST : EAST);

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

	// TODO: add double icons to these now that it is supported
	private final ModifiableCallout<BuffApplied> ttNN = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Double N", "Double North").statusIcons(0x130C, 0x130C);
	private final ModifiableCallout<BuffApplied> ttSS = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Double S", "Double South").statusIcons(0x130D, 0x130D);
	private final ModifiableCallout<BuffApplied> ttEE = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Double E", "Double East").statusIcons(0x130E, 0x130E);
	private final ModifiableCallout<BuffApplied> ttWW = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Double W", "Double West").statusIcons(0x130F, 0x130F);

	private final ModifiableCallout<BuffApplied> ttNW = ModifiableCallout.<BuffApplied>durationBasedCall("TT: N -> W", "North West", "North -> West").statusIcons(0x130C, 0x130F)
			.extendedDescription("""
					Note that these by default call the order in which the arrows will expire, i.e. right-to-left on the HUD.""");
	private final ModifiableCallout<BuffApplied> ttNE = ModifiableCallout.<BuffApplied>durationBasedCall("TT: N -> E", "North East", "North -> East").statusIcons(0x130C, 0x130E);
	private final ModifiableCallout<BuffApplied> ttSW = ModifiableCallout.<BuffApplied>durationBasedCall("TT: S -> W", "South West", "South -> West").statusIcons(0x130D, 0x130F);
	private final ModifiableCallout<BuffApplied> ttSE = ModifiableCallout.<BuffApplied>durationBasedCall("TT: S -> E", "South East", "South -> East").statusIcons(0x130D, 0x130E);
	private final ModifiableCallout<BuffApplied> ttEN = ModifiableCallout.<BuffApplied>durationBasedCall("TT: E -> N", "East North", "East -> North").statusIcons(0x130E, 0x130C);
	private final ModifiableCallout<BuffApplied> ttES = ModifiableCallout.<BuffApplied>durationBasedCall("TT: E -> S", "East South", "East -> South").statusIcons(0x130E, 0x130D);
	private final ModifiableCallout<BuffApplied> ttWN = ModifiableCallout.<BuffApplied>durationBasedCall("TT: W -> N", "West North", "West -> North").statusIcons(0x130F, 0x130C);
	private final ModifiableCallout<BuffApplied> ttWS = ModifiableCallout.<BuffApplied>durationBasedCall("TT: W -> S", "West South", "West -> South").statusIcons(0x130F, 0x130D);
	private final ModifiableCallout<BuffApplied> ttError = ModifiableCallout.durationBasedCall("TT: Error", "Error");

	private final ModifiableCallout<BuffApplied> ttConfettiOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Confetti on You", "Confetti on {confettiPlayers}").autoIcon();
	private final ModifiableCallout<BuffApplied> ttConfettiNotOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("TT: Confetti not on You", "Confetti on {confettiPlayers}").autoIcon();

	private final ModifiableCallout<TetherEvent> ttSleepTetherInitial = new ModifiableCallout<TetherEvent>("TT: Sleep Tether (Initial)", "Sleep Tether").statusIcon(4894);
	private final ModifiableCallout<TetherEvent> ttConfusionTetherInitial = new ModifiableCallout<TetherEvent>("TT: Confusion Tether (Initial)", "Confusion Tether").statusIcon(1283);
	;

	private final ModifiableCallout<TetherEvent> ttSleepTether = new ModifiableCallout<TetherEvent>("TT: Sleep Tether (After Confetti)", "Spread for Sleep").statusIcon(4894);
	private final ModifiableCallout<TetherEvent> ttConfuseTether = new ModifiableCallout<TetherEvent>("TT: Confusion Tether (After Confetti)", "Spread for Confusion").statusIcon(1283);

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
				// TODO: 8:37PM 6/20, confettis was empty
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
					playerStone = myTetherFrom.getPos().x() > 100;
					s.setParam("playerStone", playerStone);
				}
				// This call will not overwrite the confetti call
				var tetherCall = s.call(playerStone ? ttSleepTetherInitial : ttConfusionTetherInitial);
				s.waitBuffRemoved(buffs, confettis.get(0));
				tetherCall.forceExpire();
				s.updateCall(playerStone ? ttSleepTether : ttConfuseTether);

				var lookMechanic = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.allFieldsMatch(0x19D, 0x40, 0x80, 0, 0));
				s.waitThenRefreshCombatants(100);
				var lookFrom = state.getLatestCombatantData(lookMechanic.getTarget());
				boolean fakeGaze = lookFrom.getPos().x() < 100;
				s.setParam("fakeGaze", fakeGaze);
				// This call is also in parallel
				var gazeCall = s.call(fakeGaze ? ttEarlyFakeGaze : ttEarlyRealGaze, lookMechanic);

				{
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(19504));
					var playerHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.markerIdMatches(FIRE_SPREAD, FIRE_STACK));
					boolean fakeFire = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_FIRE));
					boolean fakeThunder = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
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

	// Duration is fake - it auto refreshes
	@PlayerStatusCallout(value = 0x13DB, cancellable = true)
	private final ModifiableCallout<BuffApplied> forsakenDebuffReminder = new ModifiableCallout<BuffApplied>("Forsaken: Debuff Tracker", "", "{event.stacks} Stacks").autoIcon()
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
	private final ModifiableCallout<?> forsakenTowerCone = new ModifiableCallout<>("Forsaken: Followup Cone + Past/Future (Tower Call)", "Cone with {buddy}, Baits")
			.extendedDescription("""
					The 'Tower Call' variants of these call remind you of which tower pattern you will need to do. These indicate that the tower set will be accompanied with Past/Future cast bar, while the other four indicate that it will be accompanied with All Things Ending resolving.
					
					In addition, the `towerSet` variable will let you see which set of towers it is (starting at 1 then incrementing each time towers go off). You can use expressions like `{towerSet % 2 == 0 ? 'even' : 'odd'}` to have conditional logic based on whether it is even or odd towers, or for doing things like swap callouts.""");
	private final ModifiableCallout<?> forsakenTowerCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Past/Future (Tower Call)", "Circle with {buddy}, Baits");
	private final ModifiableCallout<?> forsakenTowerStack = new ModifiableCallout<>("Forsaken: Followup Stack + Past/Future (Tower Call)", "Stack, Baits");
	private final ModifiableCallout<?> forsakenTowerNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + Past/Future (Tower Call)", "Nothing, Baits");

	private final ModifiableCallout<?> forsakenTowerNoPfCone = new ModifiableCallout<>("Forsaken: Followup Cone + No Past/Future (Tower Call)", "Cone");
	private final ModifiableCallout<?> forsakenTowerNoPfCircle = new ModifiableCallout<>("Forsaken: Followup Circle + No Past/Future (Tower Call)", "Circle");
	private final ModifiableCallout<?> forsakenTowerNoPfStack = new ModifiableCallout<>("Forsaken: Followup Stack + No Past/Future (Tower Call)", "Stack with {buddy}");
	private final ModifiableCallout<?> forsakenTowerNoPfNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + No Past/Future (Tower Call)", "Nothing");

	private final ModifiableCallout<?> forsakenFollowupPastCone = new ModifiableCallout<>("Forsaken: Followup Cone + Past", "Cone, Bait Between")
			.extendedDescription("""
					This set of eight calls tells you that you need to bait for All Things Ending.""");
	private final ModifiableCallout<?> forsakenFollowupPastCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Past", "Circle, Bait Between");
	private final ModifiableCallout<?> forsakenFollowupPastStack = new ModifiableCallout<>("Forsaken: Followup Stack + Past", "Stack with {buddy}, Bait Between");
	private final ModifiableCallout<?> forsakenFollowupPastNothing = new ModifiableCallout<>("Forsaken: Followup Nothing + Past", "Nothing, Bait Between");

	private final ModifiableCallout<?> forsakenFollowupFutureCone = new ModifiableCallout<>("Forsaken: Followup Cone + Future", "Cone, Bait Away");
	private final ModifiableCallout<?> forsakenFollowupFutureCircle = new ModifiableCallout<>("Forsaken: Followup Circle + Future", "Circle, Bait Away");
	private final ModifiableCallout<?> forsakenFollowupFutureStack = new ModifiableCallout<>("Forsaken: Followup Stack + Future", "Stack with {buddy}, Bait Away");
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

					// Find other person with same mechanic
					s.setParam("buddy", markers.stream().filter(hm -> !hm.getTarget().isThePlayer())
							.filter(hm -> mechForHm(hm) == myMech)
							.findFirst()
							.map(hm -> hm.getTargetMatching(XivCombatant::isPc))
							.orElse(null));

					// towerSet is which set of towers are about to go off. Initial towers were 1. First towers after that are 2.
					// So even numbers are where we see the past/future cast (and require bait pattern), odd numbers are where we need to first bait the cleaves, and then get in "stack pattern".
					s.setParam("towerSet", i);
					boolean hasPastFuture = i % 2 == 0;
					s.setParam("hasPastFuture", hasPastFuture);
					if (hasPastFuture) {
						// If this is a tower set with past/future, then what we need to do is:
						// First call the "early call bait pattern"
						// These are 0/2/2 pattern
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
						// These are the 2/1/1 sets
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

	private final ArenaPos tightAp = new ArenaPos(100.0, 100.0, 4.0, 4.0);
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
				var firstTrineLocations = firstSet.stream().map(e -> state.getLatestCombatantData(e.getTarget())).map(tightAp::forCombatant).toList();
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
				var secondTrineLocations = secondSet.stream().map(e -> state.getLatestCombatantData(e.getTarget())).map(tightAp::forCombatant).toList();
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

	@PlayerStatusCallout(0x1060)
	private final ModifiableCallout<BuffApplied> epicHero = new ModifiableCallout<BuffApplied>("Epic Hero", "Attack Chaos").autoIcon();
	@PlayerStatusCallout(0x1062)
	private final ModifiableCallout<BuffApplied> fatedHero = new ModifiableCallout<BuffApplied>("Fated Hero", "Attack Exdeath").autoIcon();

	private final ModifiableCallout<AbilityCastStart> bowelsInitial = ModifiableCallout.durationBasedCall("Bowels of Agony", "Raidwide");

	private static final int ENTROPY = 0x640;
	private static final int DYNAMIC = 0x641;
	private static final int HEADWIND = 0x642;
	private static final int TAILWIND = 0x643;


	private final ModifiableCallout<BuffApplied> bowelsHeadwind = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind Only", "Headwind").statusIcon(HEADWIND);
	private final ModifiableCallout<BuffApplied> bowelsTailwind = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind Only", "Tailwind").statusIcon(TAILWIND);
	private final ModifiableCallout<BuffApplied> bowelsHeadwindEntropy = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind + Entropy", "Headwind and Entropy").statusIcons(ENTROPY, HEADWIND);
	private final ModifiableCallout<BuffApplied> bowelsTailwindEntropy = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind + Entropy", "Tailwind and Entropy").statusIcons(ENTROPY, TAILWIND);
	private final ModifiableCallout<BuffApplied> bowelsHeadwindDynamic = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind + Dynamic Fluid", "Headwind and Dynamic Fluid").statusIcons(DYNAMIC, HEADWIND);
	private final ModifiableCallout<BuffApplied> bowelsTailwindDynamic = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind + Dynamic Fluid", "Tailwind and Dynamic Fluid").statusIcons(DYNAMIC, TAILWIND);

	private final ModifiableCallout<BuffApplied> bowelsMyEntropySoon = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: My Entropy Soon", "Entropy On You Soon").statusIcon(ENTROPY);
	private final ModifiableCallout<BuffApplied> bowelsOtherEntropySoon = ModifiableCallout.durationBasedCall("Bowels: Other Entropy Soon", "Entropies Soon");
	private final ModifiableCallout<BuffApplied> bowelsMyDynamicSoon = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: My Dynamic Soon", "Dynamic On You Soon").statusIcon(DYNAMIC);
	private final ModifiableCallout<BuffApplied> bowelsOtherDynamicSoon = ModifiableCallout.durationBasedCall("Bowels: Other Dynamic Soon", "Dynamics Soon");

	private final ModifiableCallout<BuffApplied> bowelsHeadwindAfter = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Headwind After Entropy/Dynamic", "Headwind").statusIcon(HEADWIND);
	private final ModifiableCallout<BuffApplied> bowelsTailwindAfter = ModifiableCallout.<BuffApplied>durationBasedCall("Bowels: Tailwind After Entropy/Dynamic", "Tailwind").statusIcon(TAILWIND);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> bowelsSq = SqtTemplates.sq(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAF2),
			(e1, s) -> {
				// TODO: would be cool for headwind call to expire when you lose it
				// TODO: call crystal locations?
				s.updateCall(bowelsInitial, e1);
				var allBuffs = s.waitEventsQuickSuccession(12, BuffApplied.class, ba -> ba.buffIdMatches(ENTROPY, DYNAMIC, HEADWIND, TAILWIND));
				XivPlayerCharacter player = state.getPlayer();
				var myEntropy = buffs.findStatusOnTarget(player, ENTROPY);
				var myDynamic = buffs.findStatusOnTarget(player, DYNAMIC);
				var myHeadwind = buffs.findStatusOnTarget(player, HEADWIND);
				var myTailwind = buffs.findStatusOnTarget(player, TAILWIND);
				if (myHeadwind != null) {
					if (myEntropy != null) {
						s.updateCall(bowelsHeadwindEntropy, myEntropy);
					}
					else if (myDynamic != null) {
						s.updateCall(bowelsHeadwindDynamic, myDynamic);
					}
					else {
						s.updateCall(bowelsHeadwind, myHeadwind);
					}

				}
				else if (myTailwind != null) {
					if (myEntropy != null) {
						s.updateCall(bowelsTailwindEntropy, myEntropy);
					}
					else if (myDynamic != null) {
						s.updateCall(bowelsTailwindDynamic, myDynamic);
					}
					else {
						s.updateCall(bowelsTailwind, myTailwind);
					}
				}
				else {
					log.error("bowels error: no head/tail");
				}
				BuffApplied anyEntropy = buffs.findBuffById(ENTROPY);
				// Wait until 5s left on buff
				s.waitDuration(anyEntropy.remainingDurationPlus(Duration.ofSeconds(-5)));
				if (myEntropy != null) {
					s.updateCall(bowelsMyEntropySoon, myEntropy);
					s.waitBuffRemoved(buffs, myEntropy);
					if (myHeadwind != null) {
						s.updateCall(bowelsHeadwindAfter, myHeadwind);
					}
					else if (myTailwind != null) {
						s.updateCall(bowelsTailwindAfter, myTailwind);
					}
				}
				else {
					s.call(bowelsOtherEntropySoon, anyEntropy);
				}

				BuffApplied anyDynamic = buffs.findBuffById(DYNAMIC);
				// Wait until 5s left on buff
				if (anyDynamic != null) {
					s.waitDuration(anyDynamic.remainingDurationPlus(Duration.ofSeconds(-5)));
				}
				if (myDynamic != null) {
					if (buffs.statusOrRefreshActive(myDynamic)) {
						s.updateCall(bowelsMyDynamicSoon, myDynamic);
						s.waitBuffRemoved(buffs, myDynamic);
					}
					if (myHeadwind != null) {
						s.updateCall(bowelsHeadwindAfter, myHeadwind);
					}
					else if (myTailwind != null) {
						s.updateCall(bowelsTailwindAfter, myTailwind);
					}
				}
				else {
					if (buffs.findBuffById(DYNAMIC) != null) {
						s.call(bowelsOtherDynamicSoon, anyDynamic);
					}
				}
			});

	private final ModifiableCallout<?> lcInitial = new ModifiableCallout<>("Limit Cut: Initial", "Starting {resultingStart} -> {resultingClockwise ? 'Clockwise' : 'CCW'}")
			.extendedDescription("""
					The four variables you can use in this call are `initialClone` and `initialClockwise` which are the first clone and which direction the initial waves are going.
					`resultingStart` and `resultingClockwise` are where the limit cut hits will start from and which direction.""");
	private final ModifiableCallout<HeadMarkerEvent> lc1 = new ModifiableCallout<HeadMarkerEvent>("Limit Cut: 1", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }")
			.extendedDescription("""
					For the individual number calls, you can use {myNumber} which is your limit cut number, starting at 1, in case you want to do math in the expressions.
					You can also use {myPosition} for the arena position opposite your clone.
					For example, `{ myPosition } { resultingClockwise ? 'Left' : 'Right' }` would call out something like "North Right" (left/right is looking inwards) for the typical LC strategy.
					""");
	private final ModifiableCallout<HeadMarkerEvent> lc2 = new ModifiableCallout<>("Limit Cut: 2", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc3 = new ModifiableCallout<>("Limit Cut: 3", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc4 = new ModifiableCallout<>("Limit Cut: 4", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc5 = new ModifiableCallout<>("Limit Cut: 5", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc6 = new ModifiableCallout<>("Limit Cut: 6", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc7 = new ModifiableCallout<>("Limit Cut: 7", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lc8 = new ModifiableCallout<>("Limit Cut: 8", "{myNumber} { myPosition } { resultingClockwise ? 'CW' : 'CCW' }");
	private final ModifiableCallout<HeadMarkerEvent> lcUnknown = new ModifiableCallout<>("Limit Cut: Error", "Error");

	private final ArenaPos ap = new ArenaPos(100, 100, 10, 10);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> lcSq = SqtTemplates.sq(180_000,
			// There isn't a good cut point for a new trigger here, so just use the same start condition as bowels
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAF2),
			(e1, s) -> {

				var hit1 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAE3) && aue.isFirstTarget());
				var hit2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAE3) && aue.isFirstTarget());
				s.waitThenRefreshCombatants(100);
				var from1 = ap.forCombatant(state.getLatestCombatantData(hit1.getSource()));
				s.setParam("initialClone", from1);
				var from2 = ap.forCombatant(state.getLatestCombatantData(hit2.getSource()));
				Boolean resultingClockwise;
				ArenaSector resultingStart;

				if (!from1.isStrictlyAdjacentTo(from2)) {
					log.error("Error determining LC positions: from1 {} not adjacent to from2 {} (hit1={}, hit2={})", from1, from2, hit1, hit2);
					resultingStart = null;
					resultingClockwise = null;
				}
				else {
					/*
					Example: First is E, second is SE
					initialClone = E
					initialClockwise = true
					resultingClockwise = false
					resultingStart = W

					Player numbers 1 - 8 are [W, SW, ..., SE]
					 */
					boolean initialClockwise = from1.eighthsTo(from2) == 1;
					s.setParam("initialClockwise", initialClockwise);
					resultingClockwise = !initialClockwise;
					s.setParam("resultingClockwise", resultingClockwise);
					resultingStart = from1.opposite();
					s.setParam("resultingStart", resultingStart);
				}
				s.updateCall(lcInitial);


				var myHm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().isThePlayer());
				int myNumber = switch ((int) myHm.getMarkerId()) {
					case 336 -> 1;
					case 337 -> 2;
					case 338 -> 3;
					case 339 -> 4;
					case 437 -> 5;
					case 438 -> 6;
					case 439 -> 7;
					case 440 -> 8;
					default -> {
						log.error("Bad LC headmarker: {}", myHm);
						yield -1;
					}
				};
				s.setParam("myNumber", myNumber);
				if (resultingClockwise != null) {
					s.setParam("myPosition", resultingStart.plusEighths((myNumber - 1) * (resultingClockwise ? 1 : -1)));
				}
				else {
					s.setParam("muPosition", ArenaSector.UNKNOWN);
				}
				ModifiableCallout<HeadMarkerEvent> markerCall = switch (myNumber) {
					case 1 -> lc1;
					case 2 -> lc2;
					case 3 -> lc3;
					case 4 -> lc4;
					case 5 -> lc5;
					case 6 -> lc6;
					case 7 -> lc7;
					case 8 -> lc8;
					default -> lcUnknown;
				};
				s.updateCall(markerCall, myHm);
				/*
				Mechanics:
				Kefka charges through arena from a random direction, doing mini-raidwides
				What matters is where he starts and whether he moves CW or CCW.
				Some strats use wind crystal as north?
				Then markers come out.
				For LC, Kefka will start at the same place, but in the opposite direction.
				Then, Exdeath does two lightning III TBs which are proximity? TODO maybe I need to change the other lightning call.
				 */

			});


	private final ModifiableCallout<AbilityCastStart> longitudinalCast = ModifiableCallout.durationBasedCallWithExtraCastTime("Longitudinal Implosion: Cast", "Sides Then Front/Back");
	private final ModifiableCallout<AbilityCastStart> latitudinalCast = ModifiableCallout.durationBasedCallWithExtraCastTime("Latitudinal Implosion: Cast", "Front/Back then Sides");
	private final ModifiableCallout<AbilityUsedEvent> longitudinalMove = new ModifiableCallout<>("Longitudinal Implosion: Move", "Front/Back");
	private final ModifiableCallout<AbilityUsedEvent> latitudinalMove = new ModifiableCallout<>("Latitudinal Implosion: Move", "Sides");

	@NpcCastCallout(0xBB13)
	private final ModifiableCallout<AbilityCastStart> vacuumWave = ModifiableCallout.durationBasedCall("Vacuum Wave", "Knockback from {event.source}");

	@NpcCastCallout(0xBB12)
	private final ModifiableCallout<AbilityCastStart> thunderIII = ModifiableCallout.durationBasedCall("Thunder III (Exdeath AoE)", "Away from {event.source}");
	@NpcCastCallout(0xBB09)
	private final ModifiableCallout<AbilityCastStart> thunderIIItb = ModifiableCallout.durationBasedCall("Thunder III (Exdeath Proximity)", "Proximity Buster");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> decisiveBattleSq = SqtTemplates.multiInvocation(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC2E2),
			(e1, s) -> {
				// Nothing here that isn't already covered by other triggers
			}, (e1, s) -> {
				// The one after limit cut
			});

	private final ModifiableCallout<AbilityCastStart> earthquake = ModifiableCallout.durationBasedCall("Earthquake Initial", "Earthquake - 1 HP");

	// These tell us the same information that the Primordial Crust duration tells us
	// First = 72s, second = 106s, third = 139s
	private static final int LINE_1 = 0xBBC;
	private static final int LINE_2 = 0xBBD;
	private static final int LINE_3 = 0xBBE;

	private static final int ACCRETION = 0x644;
	private static final int CRUST = 0x154E;

	private enum AccretionRole {
		FIRST_DPS(1),
		FIRST_SUPPORT(1),
		FIRST_ACCRETION(1),
		SECOND_DPS(2, FIRST_DPS),
		SECOND_SUPPORT(2, FIRST_SUPPORT),
		SECOND_ACCRETION(2, FIRST_ACCRETION),
		THIRD_DPS(3, SECOND_DPS),
		THIRD_SUPPORT(3, SECOND_SUPPORT);

		private final int set;
		private final @Nullable AccretionRole previous;

		AccretionRole(int set) {
			this(set, null);
		}

		AccretionRole(int set, AccretionRole previous) {
			this.set = set;
			this.previous = previous;
		}

		public int getSet() {
			return set;
		}

		public @Nullable AccretionRole getPrevious() {
			return previous;
		}
	}

	enum CleanseCallOption implements HasFriendlyName {
		ALL("All Cleanses"),
		MATCHED("Prior Set, Same Role"),
		PRIOR_SET("Entire Prior Set");

		private final String friendlyName;

		CleanseCallOption(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		@Override
		public String getFriendlyName() {
			return friendlyName;
		}
	}

	private final ModifiableCallout<BuffApplied> earthquake1supp = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: First in Line (Support, No Accretion)", "First").statusIcon(LINE_1)
			.extendedDescription("""
					The variable {accretions} will contain the players who have accretion. Consider adding that to your callout if you are a healer or have a single-target healing buff.""");
	private final ModifiableCallout<BuffApplied> earthquake2supp = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: Second in Line (Support, No Accretion)", "Second").statusIcon(LINE_2);
	private final ModifiableCallout<BuffApplied> earthquake3supp = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: Third in Line (Support, No Accretion)", "Third").statusIcon(LINE_3);
	private final ModifiableCallout<BuffApplied> earthquake1dps = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: First in Line (DPS, No Accretion)", "First").statusIcon(LINE_1);
	private final ModifiableCallout<BuffApplied> earthquake2dps = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: Second in Line (DPS, No Accretion)", "Second").statusIcon(LINE_2);
	private final ModifiableCallout<BuffApplied> earthquake3dps = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: Third in Line (DPS, No Accretion)", "Third").statusIcon(LINE_3);
	private final ModifiableCallout<BuffApplied> earthquake1acc = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: First in Line (Accretion)", "First + Accretion").statusIcons(LINE_1, ACCRETION);
	private final ModifiableCallout<BuffApplied> earthquake2acc = ModifiableCallout.<BuffApplied>durationBasedCall("Earthquake: Second in Line (Accretion)", "Second + Accretion").statusIcons(LINE_2, ACCRETION);
	private final ModifiableCallout<?> earthquakeInvalid = new ModifiableCallout<>("Earthquake: Invalid", "Error");

	private static final Duration slapHappyDelay = Duration.ofMillis(3_700);
	private final ModifiableCallout<AbilityCastStart> slapHappyRoles = ModifiableCallout.durationBasedCallWithOffset("Slap Happy: Roles", "Roles {safe}", slapHappyDelay);
	private final ModifiableCallout<AbilityCastStart> slapHappyStack = ModifiableCallout.durationBasedCallWithOffset("Slap Happy: Stack", "Stack {safe}", slapHappyDelay);
	private final ModifiableCallout<AbilityCastStart> damningEdict = ModifiableCallout.durationBasedCall("Damning Edict", "{safe} Behind Chaos");

	private final ModifiableCallout<AbilityCastStart> earthquakeBodySlamDamningEdict = ModifiableCallout.durationBasedCall("Earthquake: Body Slam + Damning Edict", "{safeSpots} safe");
	// Lat: front/back then sides
	// Long: sides then front/back
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyRolesLat = ModifiableCallout.<AbilityCastStart>durationBasedCall("Earthquake: Slap Happy + Lat: Front/Back First then Roles", "{firstSafe} to {secondSafe}, Roles {finalSafe}")
			.extendedDescription("""
					Please note that Chaos is not necessarily going to perfectly face a cardinal or intercard for this, so these directions are best-effort.
					You may still need to use eyes.
					`{firstSafe}` is one or more directions that is/are safe for the initial hit of lat/long and are adjacent to the final safe spot.
					`{secondSafe}` is one or more directions that is/are safe for the second hit of lat/long and are adjacent to the final safe spot.
					`{finalSafe}` is the slap happy safe direction.
					Note that it is technically possible to use the {secondSafe} spot for a party stack, but this is not a standard strategy.""");
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyRolesLong = ModifiableCallout.durationBasedCall("Earthquake: Slap Happy + Long: Sides First then Roles", "{firstSafe} to {secondSafe}, Roles {finalSafe}");
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyStackLat = ModifiableCallout.durationBasedCall("Earthquake: Slap Happy + Lat: Front/Back First then Stack", "{firstSafe} to {secondSafe}, Stack {finalSafe}");
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyStackLong = ModifiableCallout.durationBasedCall("Earthquake: Slap Happy + Long: Sides First then Stack", "{firstSafe} to {secondSafe}, Stack {finalSafe}");

	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyFinalRolesLat = ModifiableCallout.durationBasedCallWithOffset("Earthquake: Slap Happy + Lat: Sides + Stack", "Roles {finalSafe}", slapHappyDelay);
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyFinalRolesLong = ModifiableCallout.durationBasedCallWithOffset("Earthquake: Slap Happy + Long: Front/Back + Stack", "Roles {finalSafe}", slapHappyDelay);
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyFinalStackLat = ModifiableCallout.durationBasedCallWithOffset("Earthquake: Slap Happy + Lat: Sides + Stack", "Stack {finalSafe}", slapHappyDelay);
	private final ModifiableCallout<AbilityCastStart> earthquakeSlapHappyFinalStackLong = ModifiableCallout.durationBasedCallWithOffset("Earthquake: Slap Happy + Long: Front/Back + Stack", "Stack {finalSafe}", slapHappyDelay);

	private final ModifiableCallout<AbilityCastStart> earthquakeDespairOnly = ModifiableCallout.durationBasedCall("Earthquake: Despair Only", "{safe} Safe");

	private final ModifiableCallout<AccretionRolesEvent> earthquakePersistentTracker = new ModifiableCallout<AccretionRolesEvent>("Earthquake: Persistent Text", "", "{event.onesRemaining} #1, {event.twosRemaining} #2, {event.threesRemaining #3}", AccretionRolesEvent::anyRemain)
			.disabledByDefault()
			.extendedDescription("""
					This is a text-only callout that provides a persistent view of how many debuffs are still present in each role. You can use .onesRemaining, .twosRemaining, .threesRemaining, or .totalRemaining.""");

	private final ModifiableCallout<BuffRemoved> earthquakeCleansed = new ModifiableCallout<BuffRemoved>("Earthquake: Debuff Cleansed", "{event.target} Cleansed")
			.extendedDescription("""
					You can control when this callout fires on the settings tab above.
					By default, it works on a same-role, prior-set basis - i.e. #1 accretion cleansing will trigger this if you are #2 accretion.
					This does NOT call your own debuff being removed - use the self cleanse call below for that.""");
	private final ModifiableCallout<BuffRemoved> earthquakeSelfCleanse = new ModifiableCallout<>("Earthquake: Self Cleansed", "Cleansed");

	private final ModifiableCallout<?> earthquakeTetherSet1 = new ModifiableCallout<>("Earthquake: Tether Set #1 (One then Two)", "{firstTethers} then {secondTethers}")
			.extendedDescription("""
					For tether sets that are 1 + 2 or 2 + 1 staggered spawns, {firstTethers} and {secondTethers} tell you the first vs second locations.
					You can use {allTethers} for all locations.""");
	private final ModifiableCallout<?> earthquakeTetherSet2 = new ModifiableCallout<>("Earthquake: Tether Set #3 (Three Tethers)", "{allTethers}");
	private final ModifiableCallout<?> earthquakeTetherSet3 = new ModifiableCallout<>("Earthquake: Tether Set #4 (Three Tethers)", "{allTethers}");
	private final ModifiableCallout<?> earthquakeTetherSet4 = new ModifiableCallout<>("Earthquake: Tether Set #6 (Two then One)", "{firstTethers} then {secondTethers}");

	// These are used in callout scriptlets
	@SuppressWarnings("unused")
	private final class AccretionRolesEvent extends BaseEvent {
		final Map<AccretionRole, XivPlayerCharacter> roleMap;
		final Map<XivPlayerCharacter, AccretionRole> combatantMap;
		final @Nullable AccretionRole myRole;
		final Map<XivPlayerCharacter, BuffApplied> crustBuffs;
		final Map<XivPlayerCharacter, AbilityUsedEvent> lastNothingnessHit = new HashMap<>();

		private AccretionRolesEvent(Map<AccretionRole, XivPlayerCharacter> roleMap, Map<XivPlayerCharacter, AccretionRole> combatantMap, @Nullable AccretionRole myRole, Map<XivPlayerCharacter, BuffApplied> crustBuffs) {
			this.roleMap = roleMap;
			this.combatantMap = combatantMap;
			this.myRole = myRole;
			this.crustBuffs = crustBuffs;
		}

		long getTotalRemaining() {
			return crustBuffs.values().stream().filter(crust -> buffs.statusOf(crust) == StatusEffectCurrentStatus.ACTIVE).count();
		}

		boolean anyRemain() {
			return getTotalRemaining() > 0;
		}

		private long getRemaining(AccretionRole... roles) {
			return Arrays.stream(roles).map(roleMap::get).filter(Objects::nonNull)
					.map(crustBuffs::get).filter(Objects::nonNull)
					.filter(buffs::originalStatusActive).count();
		}

		long getOnesRemaining() {
			return getRemaining(AccretionRole.FIRST_ACCRETION, AccretionRole.FIRST_DPS, AccretionRole.FIRST_SUPPORT);
		}

		long getTwosRemaining() {
			return getRemaining(AccretionRole.SECOND_ACCRETION, AccretionRole.SECOND_DPS, AccretionRole.SECOND_SUPPORT);
		}

		long getThreesRemaining() {
			return getRemaining(AccretionRole.THIRD_DPS, AccretionRole.THIRD_SUPPORT);
		}

		public void recordNothingnessHit(AbilityUsedEvent hit) {
			if (hit.getTarget() instanceof XivPlayerCharacter pc) {
				lastNothingnessHit.put(pc, hit);

			}
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> earthquakeSq = SqtTemplates.sq(240_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC571, 0xC572),
			(e1, s) -> {
				s.updateCall(earthquake, e1);
				var crusts = s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(CRUST));
				// Give time for accretions
				s.waitMs(100);
				var accretions = buffs.findBuffsById(ACCRETION);
				List<XivCombatant> accretionPlayers = accretions.stream().map(BuffApplied::getTarget).toList();
				s.setParam("accretions", accretionPlayers);
				boolean iHaveAccretion = accretionPlayers.stream().anyMatch(XivCombatant::isThePlayer);
				s.setParam("iHaveAccretion", iHaveAccretion);

				Map<AccretionRole, XivPlayerCharacter> roleMap = new EnumMap<>(AccretionRole.class);
				Map<XivPlayerCharacter, AccretionRole> combatantMap = new HashMap<>();
				Map<XivPlayerCharacter, BuffApplied> crustBuffs = new HashMap<>();

				/*
				The role assignments are:
				DPS with #1
				Support with #1
				DPS or support with #1 + accretion
				DPS with #2
				Support with #2
				DPS or support with #2 + accretion
				DPS with #3
				Support with #3


				 */
				for (BuffApplied crust : crusts) {
					long seconds = crust.getInitialDuration().toSeconds();
					XivPlayerCharacter target = (XivPlayerCharacter) crust.getTarget();
					boolean accretion = accretionPlayers.contains(target);
					AccretionRole role;
					if (seconds < 90) {
						if (accretion) {
							role = AccretionRole.FIRST_ACCRETION;
						}
						else if (target.getJob().isSupport()) {
							role = AccretionRole.FIRST_SUPPORT;
						}
						else {
							role = AccretionRole.FIRST_DPS;
						}
					}
					else if (seconds < 120) {
						if (accretion) {
							role = AccretionRole.SECOND_ACCRETION;
						}
						else if (target.getJob().isSupport()) {
							role = AccretionRole.SECOND_SUPPORT;
						}
						else {
							role = AccretionRole.SECOND_DPS;
						}
					}
					else {
						if (accretion) {
							// This isn't supposed to happen
							log.error("Third in line also has accretion: crust {}, accretions {}", crust, accretions);
							role = null;
						}
						else if (target.getJob().isSupport()) {
							role = AccretionRole.THIRD_SUPPORT;
						}
						else {
							role = AccretionRole.THIRD_DPS;
						}
					}
					if (role != null) {
						roleMap.put(role, target);
						combatantMap.put(target, role);
					}
					crustBuffs.put(target, crust);
				}
				AccretionRole myRole = combatantMap.get(state.getPlayer());
				s.accept(new AccretionRolesEvent(roleMap, combatantMap, myRole, crustBuffs));
				// Everything past here on THIS SQ has no relation to roles or tethers. Just fire the event and let other SQs do that.

				// Role slap happy:
				// Boss ACEE 197 1E3A:0:0:0
				// then ACEE 197 1E44:0:0:0
				// Slap Happy BAE7 (4.7)
				// Slap Happy BAE9 (1.2s)
				// BB01 behind

				{
					log.info("Waiting for slap happy");
					var slapHappy = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAE6, 0xBAE7));
					ArenaSector bossFacing = ArenaPos.combatantFacing(slapHappy.getLocationInfo().getBestHeading());
					s.setParam("bossFacing", bossFacing);
					// People typically call these "left safe" or "right safe" but that is left/right looking at the boss
					// who is actually outside the arena. The actual left/right relative to the boss's true facing direction
					// is the opposite.
					if (slapHappy.abilityIdMatches(0xBAE7)) {
						// role stacks
						s.setParam("safe", bossFacing.plusQuads(1));
						s.updateCall(slapHappyRoles, slapHappy);
					}
					else {
						// whole party stack
						s.setParam("safe", bossFacing.plusQuads(-1));
						s.updateCall(slapHappyStack, slapHappy);
					}
				}


				/*
				Stuff that might be useful to call:
				* Persistent visual callout for how many people still have the debuff in each set
				* One-by-one call for a member of the prior set cleansing
				 */

				// There is the front/back damning edict cleave and a slap happy at approximately the same time next
				{
					log.info("Waiting for Damning Edict");
					AbilityCastStart damningEdictCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBB01));
					ArenaSector chaosFacing = ArenaPos.combatantFacing(damningEdictCast.getLocationInfo().getBestHeading());
					s.setParam("chaosFacing", chaosFacing);
					s.setParam("safe", chaosFacing.opposite());
					// This will overlap, so don't use an exclusive call
					s.call(damningEdict, damningEdictCast);
				}
				// Delay slightly so calls don't overlap too much
				s.waitMs(1_500);
				{
					log.info("Waiting for Slap Happy #2");
					var slapHappy = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAE6, 0xBAE7));
					ArenaSector bossFacing = ArenaPos.combatantFacing(slapHappy.getLocationInfo().getBestHeading());
					s.setParam("bossFacing", bossFacing);
					// People typically call these "left safe" or "right safe" but that is left/right looking at the boss
					// who is actually outside the arena. The actual left/right relative to the boss's true facing direction
					// is the opposite.
					if (slapHappy.abilityIdMatches(0xBAE7)) {
						// role stacks
						s.setParam("safe", bossFacing.plusQuads(1));
						s.updateCall(slapHappyRoles, slapHappy);
					}
					else {
						// whole party stack
						s.setParam("safe", bossFacing.plusQuads(-1));
						s.updateCall(slapHappyStack, slapHappy);
					}
					s.waitCastFinished(casts, slapHappy);
				}
				// Done:
				// Next is damning edict again, but with Kefka doing a body slam cleave through the middle from whatever direction he's facing.
				// Next up is lat/long + slap happy at the same time. Not entirely sure on the best way to call. Are they always aligned in some manner in terms of directions?

				// Not done yet:
				// Next is another body slam at the same time as the final tether appears.
				/*
				After that is a sequence of mechanics:
				All players get blizzard AoEs which should be dropped in center.
				One player will get a stack marker. Four players (commonly same role as stack marker player) will need to stack.
				The other four, commonly the other role, will need to do a pair of 2-man towers.
				Then the roles swap and you do stack/towers again.
				Then you move out of middle (or wherever you put the stacks)
				Exdeath casts blizzard III which is an ordained motion
				 */

				// slam + damning
				{
					log.info("Waiting for Damning + Despair (1/2)");
					// this one is messy because the body slam is baited on a player. So it could be a massive safe spot, or
					// two smaller safe spots, or a large safe spot and a small safe spot that sucks for melees.
					// BAEC is the boss cast (3.7s + 1.0s), BAEE is cast by a fake actor (4.7s) but I'm pretty sure the latter is the real damaging AoE.
					AbilityCastStart despairCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAEE));
					// We intentionally look for these out-of-order to prevent issues with a previous cast interfering
					AbilityCastStart damningEdictCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBB01));
					log.info("Waiting for Damning + Despair (2/2)");
					// Frontal cleave
					var edictFacing = ArenaPos.combatantFacing(damningEdictCast.getLocationInfo().getBestHeading());
					// Line aoe
					var despairFacing = ArenaPos.combatantFacing(despairCast.getLocationInfo().getBestHeading());
					s.setParam("edictFacing", edictFacing);
					s.setParam("despairFacing", despairFacing);
					Set<ArenaSector> safe;
					// case 1: body slam is in the same direction, or exact opposite direction, of the half room cleave
					// e.g. both facing north - we want to call SW and SE
					if (edictFacing == despairFacing || edictFacing.opposite() == despairFacing) {
						log.info("Edict/despair case 1: {} {}", edictFacing, despairFacing);
						safe = EnumSet.of(despairFacing.plusEighths(3), despairFacing.plusEighths(-3));
					}
					// case 2: body slam is perpendicular
					// If slam is E-W, body slam N, then safe spot is S
					else if (edictFacing.plusQuads(1) == despairFacing || edictFacing.plusQuads(-1) == despairFacing) {
						log.info("Edict/despair case 2: {} {}", edictFacing, despairFacing);
						safe = EnumSet.of(edictFacing.opposite());
					}
					// case 3: body slam is diagonal
					// There are multiple ways we could do this, but I think it's best to call out the most melee-friendly
					// safe spot.
					// e.g. if cleave is E-W, cleave is NW, then we should call S.
					else {
						log.info("Edict/despair case 3: {} {}", edictFacing, despairFacing);
						// The sides of the line aoe might be safe
						safe = EnumSet.of(despairFacing.plusQuads(1), despairFacing.plusQuads(-1));
						// Remove the areas getting hit by edict
						safe.remove(edictFacing);
						safe.remove(edictFacing.plusEighths(1));
						safe.remove(edictFacing.plusEighths(-1));
					}
					s.setParam("safeSpots", safe);
					s.updateCall(earthquakeBodySlamDamningEdict, despairCast);
					// TODO: got wrong call
					/*
					Damning edict was South-Southwest - likely interpreted as south
					Cleave was NW-SE
					should have called NE
					It called SE safe
					 */
				}

				// lat/long + slap happy
				{

					log.info("Waiting for Slap Happy + Lat/Long (1/2)");
					var slapHappy = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAE6, 0xBAE7));
					log.info("Waiting for Slap Happy + Lat/Long (2/2)");
					var latLong = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAFD, 0xBAFE));
					boolean longi = latLong.abilityIdMatches(0xBAFD);
					boolean roles = slapHappy.abilityIdMatches(0xBAE7);
					ArenaSector bossFacing = ArenaPos.combatantFacing(slapHappy.getLocationInfo().getBestHeading());
					ArenaSector latLongFacing = ArenaPos.combatantFacing(latLong.getLocationInfo().getBestHeading());
					s.setParam("bossFacing", bossFacing);
					s.setParam("latLongFacing", latLongFacing);
					ArenaSector slapHappyCleavingTowards = roles ? bossFacing.plusQuads(-1) : bossFacing.plusQuads(1);
					List<ArenaSector> slapHappyCleaving = List.of(slapHappyCleavingTowards.plusEighths(-1), slapHappyCleavingTowards, slapHappyCleavingTowards.plusEighths(1));
					Set<ArenaSector> sidesSafe = EnumSet.of(latLongFacing.plusQuads(-1), latLongFacing.plusQuads(1));
					Set<ArenaSector> frontBackSafe = EnumSet.of(latLongFacing, latLongFacing.opposite());
					frontBackSafe.removeAll(slapHappyCleaving);
					sidesSafe.removeAll(slapHappyCleaving);
					Set<ArenaSector> firstSafe;
					Set<ArenaSector> secondSafe;
					ArenaSector finalSafe = slapHappyCleavingTowards.opposite();
					ModifiableCallout<AbilityCastStart> call;
					ModifiableCallout<AbilityCastStart> nextCall;
					if (longi) {
						// sides first
						firstSafe = sidesSafe;
						secondSafe = frontBackSafe;
						call = roles ? earthquakeSlapHappyRolesLong : earthquakeSlapHappyStackLong;
						nextCall = roles ? earthquakeSlapHappyFinalRolesLong : earthquakeSlapHappyFinalStackLong;
					}
					else {
						// front/back first
						firstSafe = frontBackSafe;
						secondSafe = sidesSafe;
						call = roles ? earthquakeSlapHappyRolesLat : earthquakeSlapHappyStackLat;
						nextCall = roles ? earthquakeSlapHappyFinalRolesLat : earthquakeSlapHappyFinalStackLat;
					}
					s.setParam("firstSafe", firstSafe);
					s.setParam("secondSafe", secondSafe);
					s.setParam("finalSafe", finalSafe);
					s.updateCall(call, latLong);

					// BAFF is the actual hit
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAFF));
					s.updateCall(nextCall, slapHappy);
				}

				// Despair only
				{
					AbilityCastStart despairCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xBAEE));
					var despairFacing = ArenaPos.combatantFacing(despairCast.getLocationInfo().getBestHeading());
					s.setParam("despairFacing", despairFacing);
					var safe = List.of(despairFacing.plusQuads(1), despairFacing.plusQuads(-1));
					s.setParam("safe", safe);
					s.updateCall(earthquakeDespairOnly, despairCast);
				}
			}).setConcurrency(SequentialTriggerConcurrencyMode.BLOCK_NEW);

	// This has to be defined after earthquakeSq
	@AutoFeed
	private final SequentialTrigger<BaseEvent> longitudinalLatitudinalSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAFD, 0xBAFE),
			(e1, s) -> {
				if (earthquakeSq.isActive()) {
					return;
				}
				boolean longi = e1.abilityIdMatches(0xBAFD);
				s.updateCall(longi ? longitudinalCast : latitudinalCast, e1);
				// BAFF is the actual hit
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAFF));
				s.updateCall(longi ? longitudinalMove : latitudinalMove);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> earthquakeRolesRealCleanseTracker = SqtTemplates.sq(180_000,
			AccretionRolesEvent.class, ignored -> true,
			(e1, s) -> {
				// TODO: this runs out of time
				while (e1.anyRemain()) {
					var hit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAFC) && aue.isFirstTarget());
					e1.recordNothingnessHit(hit);
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> earthquakeRolesCleanses = SqtTemplates.sq(180_000,
			AccretionRolesEvent.class, ignored -> true,
			(e1, s) -> {
				// Initial call
				var myRole = e1.myRole;
				if (myRole == null) {
					s.updateCall(earthquakeInvalid);
					return;
				}
				var myBuff = e1.crustBuffs.get(state.getPlayer());
				s.updateCall(switch (myRole) {
					case FIRST_DPS -> earthquake1dps;
					case FIRST_SUPPORT -> earthquake1supp;
					case FIRST_ACCRETION -> earthquake1acc;
					case SECOND_DPS -> earthquake2dps;
					case SECOND_SUPPORT -> earthquake2supp;
					case SECOND_ACCRETION -> earthquake2acc;
					case THIRD_DPS -> earthquake3dps;
					case THIRD_SUPPORT -> earthquake3supp;
				}, myBuff);
				s.call(earthquakePersistentTracker, e1);

				while (e1.anyRemain()) {
					BuffRemoved br = s.waitEvent(BuffRemoved.class, e -> e.buffIdMatches(CRUST));
					XivPlayerCharacter target = (XivPlayerCharacter) br.getTarget();
					AccretionRole targetRole = e1.combatantMap.get(target);
					if (targetRole == null) {
						// TODO
						continue;
					}
					if (target.isThePlayer()) {
						s.updateCall(earthquakeSelfCleanse);
					}
					else {
						CleanseCallOption opt = cleanseCallSetting.get();
						// An intended cleanse is one where the player in question was recently the primary target of "nothingness"
						AbilityUsedEvent lastNothingnessHit = e1.lastNothingnessHit.get(target);
						boolean isIntendedCleanse = lastNothingnessHit != null && lastNothingnessHit.getEffectiveTimeSince().toMillis() < 2_000;
						switch (opt) {
							// Unconditional
							case ALL -> {
								if (isIntendedCleanse) {
									s.updateCall(earthquakeCleansed, br);
								}
							}
							case MATCHED -> {
								// This one does NOT have "intended cleanse" logic
								if (myRole.getPrevious() == targetRole) {
									s.updateCall(earthquakeCleansed, br);
								}
							}
							case PRIOR_SET -> {
								if (isIntendedCleanse && targetRole.getSet() == myRole.getSet() - 1) {
									s.updateCall(earthquakeCleansed, br);
								}
							}
						}
					}
				}
				// TODO: tether calls
				// Should really have a custom priority
			});

	// Tethers appear on four specific locations
	private static final ArenaPos blackholeTetherAp = new ArenaPos(100, 100, 16.9, 16.9);

	private static final int BLACK_HOLE = 0xBAFB;

	private record StaggeredTethersResult(List<ArenaSector> firstSet, List<ArenaSector> secondSet,
	                                      List<ArenaSector> combined) {
	}

	/**
	 * For sets of three tethers, you can just look at the black hole NPCs and ignore tether events entirely,
	 * since you can tell which ones will have the tethers.
	 *
	 * @param s The sequential trigger controller
	 * @return The list of arena sectors for the tethers
	 */
	private List<ArenaSector> getSimpleTetherSet(SequentialTriggerController<BaseEvent> s) {
		log.info("getSimpleTetherSet: start");
		List<ArenaSector> tetherCandidates;
		do {
			s.waitThenRefreshCombatants(50);
			// They should be 17 units away from center and on a cardinal
			tetherCandidates = state.npcsById(19512).stream()
					.map(blackholeTetherAp::forCombatant)
					.filter(ArenaSector::isCardinal)
					.sorted()
					.toList();
			log.info("getSimpleTetherSet: found {} candidates", tetherCandidates.size());
		} while (tetherCandidates.size() < 3);
		return tetherCandidates;
	}

	/**
	 * For staggered tether sets, you can identify where the tethers will come from, but not which ones will be the first vs second set.
	 * <p>
	 * TODO: look into if there's some entity ID shenanigans that can be used
	 *
	 * @param s                The sequential trigger controlly
	 * @param firstSetExpected How many tethers to wait for in the first set
	 * @return The result
	 */
	private StaggeredTethersResult getStaggeredTetherSet(SequentialTriggerController<BaseEvent> s, int firstSetExpected) {
		log.info("getStaggeredTetherSet: start");
		List<XivCombatant> tethers = new ArrayList<>(firstSetExpected);
		// Need to account for the possibility that a tether jumps instantly - we don't want to double count an NPC.
		Set<XivCombatant> seen = new HashSet<>();
		while (tethers.size() < firstSetExpected) {
			var tetherEvent = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(cbt -> cbt.npcIdMatches(19512)));
			var blackHole = tetherEvent.getTargetMatching(cbt -> cbt.npcIdMatches(19512));
			boolean unique = seen.add(blackHole);
			if (unique) {
				tethers.add(blackHole);
			}
			log.info("getStaggeredTetherSet: found {} tethers", tethers.size());
		}
		int remainingSize = 3 - firstSetExpected;
		log.info("getStaggeredTetherSet: finding non-tether NPCs");

		List<ArenaSector> secondSet;
		do {
			s.waitThenRefreshCombatants(50);
			// They should be 17 units away from center and on a cardinal
			secondSet = state.npcsById(19512).stream()
					// Exclude things we already got a tether from
					.filter(npc -> !seen.contains(npc))
					.map(blackholeTetherAp::forCombatant)
					.filter(ArenaSector::isCardinal)
					.sorted()
					.toList();
			log.info("getStaggeredTetherSet: found {}/{} second set candidates", secondSet.size(), remainingSize);
		} while (secondSet.size() < remainingSize);
		var firstSet = tethers.stream().map(state::getLatestCombatantData).map(blackholeTetherAp::forCombatant).sorted().toList();
		var combined = new ArrayList<>(firstSet);
		combined.addAll(secondSet);
		return new StaggeredTethersResult(firstSet, secondSet, combined);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> earthquakeTethers = SqtTemplates.sq(180_000,
			AccretionRolesEvent.class, ignored -> true,
			(e1, s) -> {
				// TODO: increase delays on these - it causes a lot of log spam
				// TODO: think about better ways to sort these. Setting for TN vs boss relative?
				// "Black Hole" action
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAFB));
				// staggered 1 > 2 set
				{
					var set1 = getStaggeredTetherSet(s, 1);
					s.setParam("firstTethers", set1.firstSet);
					s.setParam("secondTethers", set1.secondSet);
					s.setParam("allTethers", set1.secondSet);
					s.updateCall(earthquakeTetherSet1);
				}
				// Damning Edict (frontal cleave)
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBB01));

				{
					var set2 = getSimpleTetherSet(s);
					s.clearParams();
					s.setParam("allTethers", set2);
					s.updateCall(earthquakeTetherSet2);
				}

				// Damning Edict (frontal cleave) again
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBB01));

				{
					var set3 = getSimpleTetherSet(s);
					s.clearParams();
					s.setParam("allTethers", set3);
					s.updateCall(earthquakeTetherSet3);
				}

				// White Hole
				// TODO: do we need a call for white hole?
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBD66));
				// staggered 2 > 1 set
				{
					var set4 = getStaggeredTetherSet(s, 2);
					s.setParam("firstTethers", set4.firstSet);
					s.setParam("secondTethers", set4.secondSet);
					s.setParam("allTethers", set4.secondSet);
					s.updateCall(earthquakeTetherSet4);
				}

			});

	private final ModifiableCallout<AbilityCastStart> stompAMole = ModifiableCallout.durationBasedCall("Stomp-a-Mole: Initial Cast", "Bait Blizzards then Stacks");
	private final ModifiableCallout<AbilityCastStart> stompAMoleMove1 = ModifiableCallout.durationBasedCall("Stomp-a-Mole: Move 1", "Move");
	private final ModifiableCallout<HeadMarkerEvent> stompAMoleStackMarker1 = new ModifiableCallout<HeadMarkerEvent>("Stomp-a-Mole: Stack Marker 1", "Stack on {event.target.job.support ? 'Support' : 'DPS'}")
			.extendedDescription("""
					You can reference your own role to make this call directly tell you whether to stack or take towers,
					e.g. `{event.target.job.support == state.player.job.support ? 'Stack' : 'Towers'}""");
	// TODO: split this
	private final ModifiableCallout<AbilityCastStart> stompAMoleMove2 = ModifiableCallout.durationBasedCall("Stomp-a-Mole: Move 2", "Move");
	private final ModifiableCallout<?> stompAMoleSwitch = new ModifiableCallout<>("Stomp-a-Mole: Swap", "Swap");
	private final ModifiableCallout<AbilityCastStart> bigBangAndB3 = ModifiableCallout.durationBasedCall("Stomp-a-Mole: Blizzard + Big Bang", "Away from Stacks, Keep Moving");

	private final ModifiableCallout<AbilityCastStart> p3normalEnrage = ModifiableCallout.durationBasedCall("P3 Enrage (Normal)", "Enrage");
	private final ModifiableCallout<AbilityCastStart> p3bowelsEnrage = ModifiableCallout.durationBasedCall("P3 Enrage (Failed)", "Failed");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stompAMoleSq = SqtTemplates.sq(90_000,
			// BB0F is the Blizzard III cast
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB0F),
			(e1, s) -> {
				s.updateCall(stompAMole, e1);
				// Blizzard
				var b1 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB0D));
				s.updateCall(stompAMoleMove1, b1);
				var stackMarker = s.waitEvent(HeadMarkerEvent.class, hme -> hme.markerIdMatches(0xA1));
				s.setParam("stackOn", stackMarker.getTarget());
				s.call(stompAMoleStackMarker1, stackMarker);
				var b2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB0D));
				s.updateCall(stompAMoleMove2, b2);

				// Did we get hit by first stomp??
				List<AbilityUsedEvent> knockHits = s.collectAoeHits(aue -> aue.abilityIdMatches(0xBB03));
				List<AbilityUsedEvent> stompHits = s.collectAoeHits(aue -> aue.abilityIdMatches(0xBAF0));
				if (Stream.concat(knockHits.stream(), stompHits.stream()).anyMatch(e -> e.getTarget().isThePlayer())) {
					s.updateCall(stompAMoleSwitch);
				}
				else {
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBAF0));
					s.updateCall(stompAMoleSwitch);
				}

				var b3 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB11));
				s.updateCall(bigBangAndB3, b3);

				// TODO enrage
				// Confirmed:
				// C258 = meteor fail
				// C259 = bowels of agony fail

				// From fflogs:
				// C61E = meteor normal enrage

				// Unconfirmed:
				// C61F = bowels of agony normal enrage
				var enrages = s.waitEventsQuickSuccession(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC61E, 0xC61F, 0xC258, 0xC259));
				enrages.stream().filter(e -> e.abilityIdMatches(0xC258, 0xC259))
						.findAny()
						.ifPresentOrElse(badEnrage -> {
							s.updateCall(p3bowelsEnrage, badEnrage);
						}, () -> {
							var anyEnrage = enrages.get(0);
							s.updateCall(p3normalEnrage, anyEnrage);
							s.waitCastFinished(casts, anyEnrage);
							s.expireLastCall();
						});


				/*
				IDs:
				BAEF 4.7s initial cast
				BAF0 1.2s individual food (staggered)
				BB0D 2.7s blizzard
				BB02 4.7s knock down (stack?)
				BB03 knock down actual damage
				 */

			});


	// Exdeath Thunder III 6.7 BB12
	// Exdeath Thunder III 4.7 BB09
	// Longitudinal BAFD: Sides safe first
	// Latitudinal BAFE: Front/back safe first
	// Umbra smash BJ jump BB00 at same time as BB13 vacuum wave - should combine call
	// Does BB13 vacuum wave come up later?

	/*
	Limit cut:
	1-8 numbers
	Players go to inter-intercards?
	 */

	// P4
	/*
	Agony:
	Debuff players are one LP, non-debuff is another LP


	Kefka Says:
	We get the various debuffs but they can be real or fake

	Kefka does the arena stuff
	Exdeath gives the debuffs - the debuffs are real or fake based on his headmarker

	Acceleration bomb will be stillness or motion
	e.g. fake = motion

	Chaos cats tsunami but it's fake so actually inferno


	Second set of grand cross debuffs

	Next set doesn't matter if it's real or fake, they're double negative

	e.g. undying means you want to get hit by blue
	Always just get hit by the color you have

	Next part seems to be debuff vomit
	 */

	@NpcCastCallout(0xBB14)
	private final ModifiableCallout<AbilityCastStart> grandCross = ModifiableCallout.durationBasedCall("Grand Cross", "Raidwide");
	@NpcCastCallout(0xBB20)
	private final ModifiableCallout<AbilityCastStart> inferno = ModifiableCallout.durationBasedCall("Inferno", "Raidwide");
	@NpcCastCallout(0xBB21)
	private final ModifiableCallout<AbilityCastStart> tsunami = ModifiableCallout.durationBasedCall("Tsunami", "Raidwide");

	private static final long SHRIEK = 0x15A7;
	private static final long FORK = 0x15A8;
	private static final long WATER = 0x15A9;
	private static final long ACCEL = 0x15AA;
	private static final long WHITE_WOUND = 0x15A5; // Also 1317?
	private static final long WHITE_WOUND_FAKE = 0x1317; // Also 1317?
	private static final long BLACK_WOUND = 0x15A6; // Also 1318?
	private static final long BLACK_WOUND_FAKE = 0x1318; // Also 1318?
	private static final long ALLAG_FIELD = 0x1C6;
	private static final long BEYOND_DEATH = 0x566; // Also 1558?
	private static final long BEYOND_DEATH_FAKE = 0x1558; // Also 1558?

	private final ModifiableCallout<AbilityCastStart> kefkaSays = ModifiableCallout.durationBasedCall("Kefka Says", "Debuffs");

	private final ModifiableCallout<HeadMarkerEvent> ksRealIceRealThunder = new ModifiableCallout<>("Kefka Says: Real Ice, Real Thunder (All Sets)", "Avoid Both");
	private final ModifiableCallout<HeadMarkerEvent> ksRealIceFakeThunder = new ModifiableCallout<>("Kefka Says: Real Ice, Fake Thunder (All Sets)", "Out of Cones, In Lines");
	private final ModifiableCallout<HeadMarkerEvent> ksFakeIceRealThunder = new ModifiableCallout<>("Kefka Says: Fake Ice, Real Thunder (All Sets)", "In Cones, Out of Lines");
	private final ModifiableCallout<HeadMarkerEvent> ksFakeIceFakeThunder = new ModifiableCallout<>("Kefka Says: Fake Ice, Fake Thunder (All Sets)", "Stand in Both");

	// First set
	private final ModifiableCallout<BuffApplied> ksRealAccelShort = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Short (First Set Applied)", "Real Short Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ksRealAccelShortShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Short, with Shriek (First Set Applied)", "Real Short + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ksRealAccelLong = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Long (First Set Applied)", "Real Long Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ksRealAccelLongShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Long, with Shriek (First Set Applied)", "Real Long + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ksRealWater = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Water (First Set Applied)", "Real Water").statusIcon(WATER);
	private final ModifiableCallout<BuffApplied> ksRealLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Lightning (First Set Applied)", "Real Lightning").statusIcon(FORK);
	private final ModifiableCallout<BuffApplied> ksFakeAccelShort = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Short (First Set Applied)", "Fake Short Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ksFakeAccelShortShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Short, with Shriek (First Set Applied)", "Fake Short Accel").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ksFakeAccelLong = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Long (First Set Applied)", "Fake Long Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ksFakeAccelLongShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Long, with Shriek (First Set Applied)", "Fake Long + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ksFakeWater = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Water (First Set Applied)", "Fake Water").statusIcon(WATER);
	private final ModifiableCallout<BuffApplied> ksFakeLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Lightning (First Set Applied)", "Fake Lightning").statusIcon(FORK);

	private final ModifiableCallout<BuffApplied> ksRealDyn = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Dynamic Fluid (First Set Applied)", "Real Dynamic").statusIcon(DYNAMIC)
			.extendedDescription("""
					Note that the first/second set refer to the order in which they will RESOLVE, not apply.""");
	private final ModifiableCallout<BuffApplied> ksRealEnt = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Entropy (First Set Applied)", "Real Entropy").statusIcon(ENTROPY);
	private final ModifiableCallout<BuffApplied> ksFakeDyn = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Dynamic Fluid (First Set Applied)", "Fake Dynamic").statusIcon(DYNAMIC);
	private final ModifiableCallout<BuffApplied> ksFakeEnt = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Entropy (First Set Applied)", "Fake Entropy").statusIcon(ENTROPY);


	private final ModifiableCallout<BuffApplied> ks2RealAccelShort = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Short (Second Set Applied)", "Real Short Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ks2RealAccelShortShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Short, with Shriek (Second Set Applied)", "Real Short + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ks2RealAccelLong = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Long (Second Set Applied)", "Real Long Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ks2RealAccelLongShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Accel, Long, with Shriek (Second Set Applied)", "Real Long + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ks2RealWater = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Water (Second Set Applied)", "Real Water").statusIcon(WATER);
	private final ModifiableCallout<BuffApplied> ks2RealLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Lightning (Second Set Applied)", "Real Lightning").statusIcon(FORK);
	private final ModifiableCallout<BuffApplied> ks2FakeAccelShort = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Short (Second Set Applied)", "Fake Short Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ks2FakeAccelShortShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Short, with Shriek (Second Set Applied)", "Fake Short Accel").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ks2FakeAccelLong = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Long (Second Set Applied)", "Fake Long Accel").statusIcon(ACCEL);
	private final ModifiableCallout<BuffApplied> ks2FakeAccelLongShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Accel, Long, with Shriek (Second Set Applied)", "Fake Long + Shriek").statusIcons(SHRIEK, ACCEL);
	private final ModifiableCallout<BuffApplied> ks2FakeWater = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Water (Second Set Applied)", "Fake Water").statusIcon(WATER);
	private final ModifiableCallout<BuffApplied> ks2FakeLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Lightning (Second Set Applied)", "Fake Lightning").statusIcon(FORK);
	
	private final ModifiableCallout<BuffApplied> ksRealDyn2 = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Dynamic Fluid (Second Set Applied)", "Real Dynamic").statusIcon(DYNAMIC);
	private final ModifiableCallout<BuffApplied> ksRealEnt2 = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real Entropy (Second Set Applied)", "Real Entropy").statusIcon(ENTROPY);
	private final ModifiableCallout<BuffApplied> ksFakeDyn2 = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Dynamic Fluid (Second Set Applied)", "Fake Dynamic").statusIcon(DYNAMIC);
	private final ModifiableCallout<BuffApplied> ksFakeEnt2 = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake Entropy (Second Set Applied)", "Fake Entropy").statusIcon(ENTROPY);

	private final ModifiableCallout<BuffApplied> ks3RealWWBD = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real WW + BD", "Real White + Death (Applied)").statusIcons(WHITE_WOUND, BEYOND_DEATH);
	private final ModifiableCallout<BuffApplied> ks3FakeWWBD = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake WW + BD", "Fake White + Death (Applied)").statusIcons(WHITE_WOUND, BEYOND_DEATH);
	private final ModifiableCallout<BuffApplied> ks3RealBWBD = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real BW + BD", "Real Black + Death (Applied)").statusIcons(BLACK_WOUND, BEYOND_DEATH);
	private final ModifiableCallout<BuffApplied> ks3FakeBWBD = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake BW + BD", "Fake Black + Death (Applied)").statusIcons(BLACK_WOUND, BEYOND_DEATH);
	private final ModifiableCallout<BuffApplied> ks3RealWWAF = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real WW + AF", "Real White + Allag (Applied)").statusIcons(WHITE_WOUND, ALLAG_FIELD);
	private final ModifiableCallout<BuffApplied> ks3FakeWWAF = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake WW + AF", "Fake White + Allag (Applied)").statusIcons(WHITE_WOUND, ALLAG_FIELD);
	private final ModifiableCallout<BuffApplied> ks3RealBWAF = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Real BW + AF", "Real Black + Allag (Applied)").statusIcons(BLACK_WOUND, ALLAG_FIELD);
	private final ModifiableCallout<BuffApplied> ks3FakeBWAF = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Fake BW + AF", "Fake Black + Allag (Applied)").statusIcons(BLACK_WOUND, ALLAG_FIELD);
	private final ModifiableCallout<BuffApplied> ks3error = new ModifiableCallout<>("Kefka Says: Missing/Invalid Debuffs", "Error");

	private final ModifiableCallout<AbilityCastStart> ks3standInWhite = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Stand in White", "Stand in White ({whitePos})");
	private final ModifiableCallout<AbilityCastStart> ks3standInBlack = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Stand in Black", "Stand in Black ({blackPos})");

	private final ModifiableCallout<BuffApplied> ksFirstBombSetStack = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Stack", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> ksFirstBombSetSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Spread", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> ksFirstBombSetNothing = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Nothing", "Stack with {stacks}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksFirstBombSetAccelStack = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Accel + Stack", "{stillness ? 'Stillness' : 'Motion'} and Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> ksFirstBombSetAccelSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Accel + Spread", "{stillness ? 'Stillness' : 'Motion'} and Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> ksFirstBombSetAccelNothing = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Debuff Set Resolving: Accel + Nothing", "{stillness ? 'Stillness' : 'Motion'} and Stack with {stacks}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksThunderShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Thunder and First Shrieks Resolving", "{fakeThunder ? 'Fake' : 'Real'} Thunder, {fakeShriek ? 'Fake' : 'Real'} Gaze").statusIcon(SHRIEK);
	private final ModifiableCallout<BuffApplied> ksFirstEntropyDynamic = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: First Entropy/Dynamic Resolving", "{isDonut ? 'Donut' : 'Stack'}").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksFirstEntropyDynamicMove = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: First Entropy/Dynamic: Move (Circle Aoe)", "Move").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksFirstEntropyDynamicStay = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: First Entropy/Dynamic: Stay (Donut AoE)", "Stay").autoIcon();

	private final ModifiableCallout<BuffApplied> ksSecondBombSetStack = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Stack", "Stack{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondBombSetSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Spread", "Spread{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondBombSetNothing = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Nothing", "Stack with {stacks}{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondBombSetAccelStack = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Accel + Stack", "{stillness ? 'Stillness' : 'Motion'} and Stack{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondBombSetAccelSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Accel + Spread", "{stillness ? 'Stillness' : 'Motion'} and Spread{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondBombSetAccelNothing = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Debuff Set Resolving: Accel + Nothing", "{stillness ? 'Stillness' : 'Motion'} and Stack with {stacks}{fakeIce ? ' In Ice' : ', Avoid Ice'}").autoIcon();

	private final ModifiableCallout<BuffApplied> ksSecondShriek = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Shrieks Resolving", "{fakeShriek ? 'Fake' : 'Real'} Gaze").statusIcon(SHRIEK);

	private final ModifiableCallout<BuffApplied> ksSecondEntropyDynamicBothReal = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Entropy/Dynamic Resolving, Both Thunder/Ice Real", "{isDonut ? 'Donut' : 'Stack'}, Avoid Both").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicMoveBothReal = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Move, Both Thunder/Ice Real", "Move, Avoid Both").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicStayBothReal = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Stay, Both Thunder/Ice Real", "Stay out of Both").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondEntropyDynamicBothFake = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Entropy/Dynamic Resolving, Both Thunder/Ice Fake", "{isDonut ? 'Donut' : 'Stack'}, Stand in Both").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicMoveBothFake = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Move, Both Thunder/Ice Fake", "Move, Into Both").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicStayBothFake = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Stay, Both Thunder/Ice Fake", "Stay in Both").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondEntropyDynamicFakeIce = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Entropy/Dynamic Resolving, Real Thunder, Fake Ice", "{isDonut ? 'Donut' : 'Stack'}, Fake Ice, Real Thunder").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicMoveFakeIce = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Move, Real Thunder, Fake Ice", "Move Into Ice").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicStayFakeIce = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Stay, Real Thunder, Fake Ice", "Stay In Ice").autoIcon();
	private final ModifiableCallout<BuffApplied> ksSecondEntropyDynamicFakeThunder = ModifiableCallout.<BuffApplied>durationBasedCall("Kefka Says: Second Entropy/Dynamic Resolving, Fake Thunder, Real Ice", "{isDonut ? 'Donut' : 'Stack'}, Real Ice, Fake Thunder").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicMoveFakeThunder = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Move, Fake Thunder, Real Ice", "Move Into Thunder").autoIcon();
	private final ModifiableCallout<AbilityCastStart> ksSecondEntropyDynamicStayFakeThunder = ModifiableCallout.<AbilityCastStart>durationBasedCall("Kefka Says: Second Entropy/Dynamic: Stay, Fake Thunder, Real Ice", "Stay in Thunder").autoIcon();

	private static final int NPC_CHAOS = 19507;
	private static final int NPC_NEOXD = 19510;

	private static final int REAL_NE = 1122;
	private static final int FAKE_NE = 1121;
	private static final int REAL_CH = 1120;
	private static final int FAKE_CH = 1119;

	private @Nullable BuffApplied findDurationBelow(int seconds, BuffApplied... buffs) {
		return Arrays.stream(buffs).filter(Objects::nonNull).filter(ba -> ba.getEstimatedRemainingDuration().toSeconds() < seconds && !ba.wouldBeExpired()).findFirst().orElse(null);
	}

	private static @Nullable BuffApplied findById(List<BuffApplied> buffs, long... ids) {
		return buffs.stream().filter(ba -> ba.buffIdMatches(ids)).findAny().orElse(null);
	}

	private ModifiableCallout<BuffApplied> getDynEntCall(boolean isDynamic, boolean isLong, boolean isReal) {
		if (isDynamic) {
			if (isLong) {
				return isReal ? ksRealDyn2 : ksFakeDyn2;
			}
			else {
				return isReal ? ksRealDyn : ksFakeDyn;
			}
		}
		else {
			if (isLong) {
				return isReal ? ksRealEnt2 : ksFakeEnt2;
			}
			else {
				return isReal ? ksRealEnt : ksFakeEnt;
			}
		}

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> kefkaSaysSq = SqtTemplates.multiInvocation(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC2DC),
			(e1, s) -> {
				s.updateCall(kefkaSays, e1);

				// Kefka's ID changes throughout the fight
				final int NPC_KEFKA = 18475;
				{
					log.info("MM1 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					boolean fakeThunder = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
					boolean fakeIce = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_ICE));
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kefkaHM.get(0);
					if (fakeThunder) {
						s.updateCall(fakeIce ? ksFakeIceFakeThunder : ksRealIceFakeThunder, hm1);
					}
					else {
						s.updateCall(fakeIce ? ksFakeIceRealThunder : ksRealIceRealThunder, hm1);
					}
					log.info("MM1 end");
				}

				// Mystery magic 2
				{
					log.info("MM2 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					boolean fakeThunder = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
					boolean fakeIce = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_ICE));
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kefkaHM.get(0);
					if (fakeThunder) {
						s.updateCall(fakeIce ? ksFakeIceFakeThunder : ksRealIceFakeThunder, hm1);
					}
					else {
						s.updateCall(fakeIce ? ksFakeIceRealThunder : ksRealIceRealThunder, hm1);
					}
					log.info("MM2 end");
				}

				// Mystery magic 3
				{
					log.info("MM3 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					boolean fakeThunder = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
					boolean fakeIce = kefkaHM.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_ICE));
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeIce", fakeIce);
					var hm1 = kefkaHM.get(0);
					if (fakeThunder) {
						s.updateCall(fakeIce ? ksFakeIceFakeThunder : ksRealIceFakeThunder, hm1);
					}
					else {
						s.updateCall(fakeIce ? ksFakeIceRealThunder : ksRealIceRealThunder, hm1);
					}
					log.info("MM3 end");
				}

				// Mana Charge
				{
					var kefkaHm1 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.eitherTargetMatches(cbt -> cbt.npcIdMatches(NPC_KEFKA)));
					boolean fakeThunderPre = kefkaHm1.markerIdMatches(FAKE_THUNDER);
					var kefkaHm2 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.eitherTargetMatches(cbt -> cbt.npcIdMatches(NPC_KEFKA)));
					boolean fakeIcePre = kefkaHm2.markerIdMatches(FAKE_ICE);

					var manaChargeFinalHms = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					boolean fakeThunderPost = manaChargeFinalHms.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_THUNDER));
					boolean fakeIcePost = manaChargeFinalHms.stream().anyMatch(hme -> hme.markerIdMatches(FAKE_ICE));
					boolean fakeThunder = fakeThunderPre ^ fakeThunderPost;
					boolean fakeIce = fakeIcePre ^ fakeIcePost;
					s.accept(new KefkaManaChargeDetailEvent(fakeThunder, fakeIce));
				}

				// Very slightly later, Kefka starts casting Mystery Magic BA94
				// Neo Exdeath got StatusLoopVfx 1122 0x462 (real?)
				// Thunder/blizzard goes off
				// Chaos got SLV 1120 but it visually looked the same as 1122?
				// Debuffs went out
				// NExd does his thing
				// Chaos does his thing
				// Kefka gets two more HMs
				// Chaos gets SLV 1199 - question marks
				// Would probably guess NE's fake is 1121

			});

	// Just going to track these separately because they have very annoying timing inconsistency.
	private @Nullable StatusLoopVfxApplied lastNeVfx;
	private @Nullable StatusLoopVfxApplied lastChVfx;

	@HandleEvents
	public void handleVfx(StatusLoopVfxApplied vfx) {
		XivCombatant target = vfx.getTarget();
		if (target.npcIdMatches(NPC_CHAOS)) {
			lastChVfx = vfx;
		}
		else if (target.npcIdMatches(NPC_NEOXD)) {
			lastNeVfx = vfx;
		}
	}

	private boolean realNe() {
		return lastNeVfx.vfxIdMatches(REAL_NE);
	}

	private boolean realCh() {
		return lastChVfx.vfxIdMatches(REAL_CH);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> kefkaSaysSqExdeath = SqtTemplates.multiInvocation(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC2DC),
			(e1, s) -> {

				// Tracks which debuffs are fake
				Set<BuffApplied> fakeDebuffs = new HashSet<>();

				// Kefka's ID changes throughout the fight
				final int NPC_KEFKA = 18475;
				{
					log.info("NE MM1 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					log.info("NE MM1 end");
				}
				log.info("Waiting for neVfx1");
				// TODO: make separate call for these
				var neVfx1 = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(NPC_NEOXD));
				boolean neReal1 = neVfx1.vfxIdMatches(REAL_NE);
				// Wait for grand cross hit
//				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBB14));
				// Doing it like this so the trigger doesn't get messed up if player is dead and doesn't get a debuff
				log.info("Waiting for debuff spam");
				var myDebuffs1 = s.waitEventsQuickSuccession(10, BuffApplied.class, ba -> ba.buffIdMatches(FORK, WATER, SHRIEK, ACCEL))
						.stream()
						.peek(ba -> {
							if (!neReal1) {
								fakeDebuffs.add(ba);
							}
						})
						.filter(ba -> ba.getTarget().isThePlayer())
						.toList();
				BuffApplied myAccel1 = findById(myDebuffs1, ACCEL);
				BuffApplied myShriek1 = findById(myDebuffs1, SHRIEK);
				BuffApplied myWater1 = findById(myDebuffs1, WATER);
				BuffApplied myFork1 = findById(myDebuffs1, FORK);

				s.waitMs(100);

//				var fork = buffs.findBuffsById(FORK);
//				var water = buffs.findBuffsById(WATER);
//				var shriek = buffs.findBuffsById(SHRIEK);
//				var accel = buffs.findBuffsById(ACCEL);
//
				if (myAccel1 != null) {
					if (myAccel1.getInitialDuration().toSeconds() < 60) {
						if (myShriek1 != null) {
							s.updateCall(neReal1 ? ksRealAccelShortShriek : ksFakeAccelShortShriek, myAccel1);
						}
						else {
							s.updateCall(neReal1 ? ksRealAccelShort : ksFakeAccelShort, myAccel1);
						}
					}
					else {
						if (myShriek1 != null) {
							s.updateCall(neReal1 ? ksRealAccelLongShriek : ksFakeAccelLongShriek, myAccel1);
						}
						else {
							s.updateCall(neReal1 ? ksRealAccelLong : ksFakeAccelLong, myAccel1);
						}
					}
				}
				else if (myWater1 != null) {
					s.updateCall(neReal1 ? ksRealWater : ksFakeWater, myWater1);
				}
				else if (myFork1 != null) {
					s.updateCall(neReal1 ? ksRealLightning : ksFakeLightning, myFork1);
				}

				// Mystery magic 2
				{
					log.info("NE MM2 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					s.expireLastCall();
					log.info("NE MM2 end");
				}

				log.info("Waiting for neVfx2");
				var neVfx2 = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(NPC_NEOXD));
				boolean neReal2 = neVfx2.vfxIdMatches(REAL_NE);

				log.info("Waiting for debuff spam 2");
				var myDebuffs2 = s.waitEventsQuickSuccession(10, BuffApplied.class, ba -> ba.buffIdMatches(FORK, WATER, SHRIEK, ACCEL))
						.stream()
						.peek(ba -> {
							if (!neReal2) {
								fakeDebuffs.add(ba);
							}
						})
						.filter(ba -> ba.getTarget().isThePlayer())
						.toList();
				BuffApplied myAccel2 = findById(myDebuffs2, ACCEL);
				BuffApplied myShriek2 = findById(myDebuffs2, SHRIEK);
				BuffApplied myWater2 = findById(myDebuffs2, WATER);
				BuffApplied myFork2 = findById(myDebuffs2, FORK);
				if (myAccel2 != null) {
					// accels are 36 vs 61 at this point
					if (myAccel2.getInitialDuration().toSeconds() < 45) {
						if (myShriek2 != null) {
							s.updateCall(neReal2 ? ks2RealAccelShortShriek : ks2FakeAccelShortShriek, myAccel2);
						}
						else {
							s.updateCall(neReal2 ? ks2RealAccelShort : ks2FakeAccelShort, myAccel2);
						}
					}
					else {
						if (myShriek2 != null) {
							s.updateCall(neReal2 ? ks2RealAccelLongShriek : ks2FakeAccelLongShriek, myAccel2);
						}
						else {
							s.updateCall(neReal2 ? ks2RealAccelLong : ks2FakeAccelLong, myAccel2);
						}
					}
				}
				else if (myWater2 != null) {
					s.updateCall(neReal2 ? ks2RealWater : ks2FakeWater, myWater2);
				}
				else if (myFork2 != null) {
					s.updateCall(neReal2 ? ks2RealLightning : ks2FakeLightning, myFork2);
				}


				// Mystery magic 3
				{
					log.info("NE MM3 start");
					List<HeadMarkerEvent> kefkaHM = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(NPC_KEFKA));
					s.expireLastCall();
					log.info("NE MM3 end");
				}


				log.info("Waiting for white/black debuffs");
				var myDebuffs3 = s.waitEventsQuickSuccession(16, BuffApplied.class, ba -> ba.buffIdMatches(WHITE_WOUND, BLACK_WOUND, ALLAG_FIELD, BEYOND_DEATH, BEYOND_DEATH_FAKE, WHITE_WOUND_FAKE, BLACK_WOUND_FAKE))
						.stream()
						.filter(ba -> ba.getTarget().isThePlayer())
						.toList();
				// These have inconsistent ordering, so use the external tracking
				boolean neReal3 = realNe();
				log.info("myDebuffs3: {}", myDebuffs3);
				// Get hit
				BuffApplied myBD = findById(myDebuffs3, BEYOND_DEATH, BEYOND_DEATH_FAKE);
				// Don't get hit
				BuffApplied myAF = findById(myDebuffs3, ALLAG_FIELD);
				// You will die if standing in black
				BuffApplied myWW = findById(myDebuffs3, WHITE_WOUND, WHITE_WOUND_FAKE);
				// You will die if standing in white
				BuffApplied myBW = findById(myDebuffs3, BLACK_WOUND, BLACK_WOUND_FAKE);
				log.info("Player debuffs: {} {} {} {}", myBD != null, myAF != null, myWW != null, myBW != null);
				s.setParam("myBD", myBD);
				s.setParam("myAF", myAF);
				s.setParam("myWW", myWW);
				s.setParam("myBW", myBW);

				// BD3 = need to get hit, but fakeness will invert that
				boolean playerShouldGetHit = myBD != null ^ !neReal3;
				boolean playerWillDieToWhite = myWW != null ^ !neReal3;
				boolean playerShouldStandInRealWhite = playerShouldGetHit == playerWillDieToWhite;
				RawModifiedCallout<?> call = null;
				if (myBD != null) {
					if (myWW != null) {
						call = s.updateCall(neReal3 ? ks3RealWWBD : ks3FakeWWBD, myBD);
					}
					else if (myBW != null) {
						call = s.updateCall(neReal3 ? ks3RealBWBD : ks3FakeBWBD, myBD);
					}
				}
				else if (myAF != null) {
					if (myWW != null) {
						call = s.updateCall(neReal3 ? ks3RealWWAF : ks3FakeWWAF, myAF);
					}
					else if (myBW != null) {
						call = s.updateCall(neReal3 ? ks3RealBWAF : ks3FakeBWAF, myAF);
					}
				}
				if (call == null) {
					call = s.updateCall(ks3error);
				}
				// TODO: got a wrong call at 6:57 PM local time
				// Got real white + allag call
				// log: Logic: get hit: false; white is lethal: true; stand in real white: false; black/white is real: true; stand in white: false
				// Actual values:
				/*
				NE had 1122 (real) (correct)
				Had allag + white (correct)
				Need to get hit by black

				NE had 1122 again (real) (correct)
				So it correctly called "Stand in Black"

				However, it looks like we have black/white mixed up? Or maybe it's something to do with the fakery.
				Going to try swapping the cast ID.
				 */

				log.info("Waiting for neVfx4");
				var neVfx4 = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(NPC_NEOXD));
				boolean neReal4 = neVfx4.vfxIdMatches(REAL_NE);

				// Final place for player to stand
				boolean playerShouldStandInWhite = playerShouldStandInRealWhite == neReal4;
				log.info("Logic: get hit: {}; white is lethal: {}; stand in real white: {}; black/white is real: {}; stand in white: {}", playerShouldGetHit, playerWillDieToWhite, playerShouldStandInRealWhite, neReal4, playerShouldStandInWhite);

				// The white/black cast itself is also fake/real

				log.info("Waiting for white/black cast");
				AbilityCastStart blackCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(neReal4 ? 0xC395 : 0xC394));
				Position blackPos = blackCast.getLocationInfo().getPos();
				if (blackPos == null) {
					log.error("no black position!");
				}
				else {
					Position blackCleaving = blackPos.translateRelative(0, 20);
					ArenaPos tightAp = new ArenaPos(100, 100, 5, 5);
					ArenaSector blackCleavePos = tightAp.forPosition(blackCleaving);
					ArenaSector whiteCleavePos = blackCleavePos.opposite();
					s.setParam("whitePos", whiteCleavePos);
					s.setParam("blackPos", blackCleavePos);
				}
				s.updateCall(playerShouldStandInWhite ? ks3standInWhite : ks3standInBlack, blackCast);

				s.waitCastFinished(casts, blackCast);

				log.info("Fake debuffs: {}", fakeDebuffs);

				// First debuff set resolving at this point
				// Five real options here:
				// Real stack
				// Real spread
				// Fake stack (spread)
				// Fake spread (stack)
				// Nothing (go stack, call who to stack on)
				// Stillness/motion will just be a parameter.
				{
					log.info("First debuff callouts");
					BuffApplied myFork = findDurationBelow(20, myFork1, myFork2);
					BuffApplied myAccel = findDurationBelow(20, myAccel1, myAccel2);
					BuffApplied myWater = findDurationBelow(20, myWater1, myWater2);
					log.info("myFork: {}", myFork);
					log.info("myAccel: {}", myAccel);
					log.info("myWater: {}", myWater);
					// TODO: put icons on these
					if (myFork != null) {
						// Fork is naturally spread
						boolean fake = fakeDebuffs.contains(myFork);
						if (myAccel != null) {
							// Accel is naturally stillness
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(fake ? ksFirstBombSetAccelStack : ksFirstBombSetAccelSpread, myFork);
						}
						else {
							s.updateCall(fake ? ksFirstBombSetStack : ksFirstBombSetSpread, myFork);
						}
					}
					else if (myWater != null) {
						// Fork is naturally stack
						boolean fake = fakeDebuffs.contains(myWater);
						if (myAccel != null) {
							// Accel is naturally stillness
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(fake ? ksFirstBombSetAccelSpread : ksFirstBombSetAccelStack, myWater);
						}
						else {
							s.updateCall(fake ? ksFirstBombSetSpread : ksFirstBombSetStack, myWater);
						}
					}
					else {
						// Go stack on someone
						// Find the players to stack on
						List<BuffApplied> stacks = buffs.getBuffs().stream()
								.filter(ba -> {
									// Look for fake lightning or real water
									if (fakeDebuffs.contains(ba)) {
										return ba.buffIdMatches(FORK);
									}
									else {
										return ba.buffIdMatches(WATER);
									}
								}).filter(ba -> ba.getEstimatedRemainingDuration().toSeconds() < 20)
								.toList();
						BuffApplied stackBuff = stacks.stream().findAny().orElse(null);
						s.setParam("stacks", stacks.stream().map(BuffApplied::getTarget).toList());
						if (myAccel != null) {
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(ksFirstBombSetAccelNothing, stackBuff);
						}
						else {
							s.updateCall(ksFirstBombSetNothing, stackBuff);
						}
					}
				}
				var kefkaHm1 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.eitherTargetMatches(cbt -> cbt.npcIdMatches(NPC_KEFKA)));
				boolean fakeThunder = kefkaHm1.markerIdMatches(FAKE_THUNDER);
				{
					BuffApplied shortShriek = buffs.findBuff(ba -> ba.buffIdMatches(SHRIEK) && ba.getEstimatedRemainingDuration().toSeconds() < 15);
					boolean fakeShriek = fakeDebuffs.contains(shortShriek);
					s.setParam("fakeThunder", fakeThunder);
					s.setParam("fakeShriek", fakeShriek);
					s.updateCall(ksThunderShriek, shortShriek);
				}
				var kefkaHm2 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.eitherTargetMatches(cbt -> cbt.npcIdMatches(NPC_KEFKA)));
				boolean fakeIce = kefkaHm2.markerIdMatches(FAKE_ICE);

				{

					s.setParam("fakeIce", fakeIce);
					log.info("Second debuff callouts");
					BuffApplied myFork = findDurationBelow(20, myFork1, myFork2);
					BuffApplied myAccel = findDurationBelow(20, myAccel1, myAccel2);
					BuffApplied myWater = findDurationBelow(20, myWater1, myWater2);
					log.info("myFork (2): {}", myFork);
					log.info("myAccel (2): {}", myAccel);
					log.info("myWater (2): {}", myWater);
					// TODO: put icons on these
					if (myFork != null) {
						// Fork is naturally spread
						boolean fake = fakeDebuffs.contains(myFork);
						if (myAccel != null) {
							// Accel is naturally stillness
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(fake ? ksSecondBombSetAccelStack : ksSecondBombSetAccelSpread, myFork);
						}
						else {
							s.updateCall(fake ? ksSecondBombSetStack : ksSecondBombSetSpread, myFork);
						}
						s.waitBuffRemoved(buffs, myWater);
					}
					else if (myWater != null) {
						// Fork is naturally stack
						boolean fake = fakeDebuffs.contains(myWater);
						if (myAccel != null) {
							// Accel is naturally stillness
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(fake ? ksSecondBombSetAccelSpread : ksSecondBombSetAccelStack, myWater);
						}
						else {
							s.updateCall(fake ? ksSecondBombSetSpread : ksSecondBombSetStack, myWater);
						}
						s.waitBuffRemoved(buffs, myWater);
					}
					else {
						// Go stack on someone
						// Find the players to stack on
						List<BuffApplied> stacks = buffs.getBuffs().stream()
								.filter(ba -> {
									// Look for fake lightning or real water
									if (fakeDebuffs.contains(ba)) {
										return ba.buffIdMatches(FORK);
									}
									else {
										return ba.buffIdMatches(WATER);
									}
								}).filter(ba -> ba.getEstimatedRemainingDuration().toSeconds() < 20)
								.toList();
						BuffApplied stackBuff = stacks.stream().findAny().orElse(null);
						s.setParam("stacks", stacks.stream().map(BuffApplied::getTarget).toList());
						if (myAccel != null) {
							s.setParam("stillness", !fakeDebuffs.contains(myAccel));
							s.updateCall(ksSecondBombSetAccelNothing, stackBuff);
						}
						else {
							s.updateCall(ksSecondBombSetNothing, stackBuff);
						}
						s.waitBuffRemoved(buffs, stackBuff);
					}

					BuffApplied longShriek = buffs.findBuff(ba -> ba.buffIdMatches(SHRIEK) && ba.getEstimatedRemainingDuration().toSeconds() < 15);
					boolean fakeShriek = fakeDebuffs.contains(longShriek);
					s.setParam("fakeShriek", fakeShriek);
					s.updateCall(ksSecondShriek, longShriek);
				}
			});

	private static class KefkaManaChargeDetailEvent extends BaseEvent {
		final boolean thunderFake;
		final boolean iceFake;

		private KefkaManaChargeDetailEvent(boolean thunderFake, boolean iceFake) {
			this.thunderFake = thunderFake;
			this.iceFake = iceFake;
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> kefkaSaysChaos = SqtTemplates.multiInvocation(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xC2DC),
			(e1, s) -> {

				// These also have different IDs for some reason
				final int ENTROPY = 0x15AB;
				final int DYNAMIC = 0x15AC;

				// Tracks which debuffs are fake
				Set<BuffApplied> fakeDebuffs = new HashSet<>();

				log.info("Waiting for chVfx1");
				var chVfx1 = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(NPC_CHAOS));
				boolean chReal1 = chVfx1.vfxIdMatches(REAL_CH);
				// Wait for grand cross hit
//				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xBB14));
				// Doing it like this so the trigger doesn't get messed up if player is dead and doesn't get a debuff

				log.info("Waiting for first entropy/dynamic");
				// All players get these, so don't need to filter by player.
				// It can apply the first set first, or the second set first. Look at duration.
				// e.g. normal order is 60 -> 69
				// reverse order is 84 -> 45
				// Really annoying - order of application is not consistent. Sometimes it comes right before the above VFX, sometimes after?
				// For some reason one of them takes longer to apply.
				s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(DYNAMIC, ENTROPY));
				// Delay to not talk over MM call, plus we need to potentially delay to let all the buffs come anyway
				s.waitMs(1_500);
				// Capture everyone's debuffs as fake so that if we do a normal buff lookup later, it doesn't matter if we got the wrong one
				var dynEntro1 = buffs.findBuffs(ba -> ba.buffIdMatches(DYNAMIC, ENTROPY))
						.stream()
						.peek(ba -> {
							if (!chReal1) {
								fakeDebuffs.add(ba);
							}
						})
						.findFirst()
						.orElseThrow();

				s.updateCall(getDynEntCall(
						dynEntro1.buffIdMatches(DYNAMIC),
						dynEntro1.getInitialDuration().toSeconds() > 75,
						chReal1), dynEntro1);

				log.info("Waiting for chVfx2");
				var chVfx2 = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(NPC_CHAOS));
				s.expireLastCall();
				s.waitMs(5_000);

				boolean chReal2 = chVfx2.vfxIdMatches(REAL_CH);
				// It can apply the first set first, or the second set first. Look at duration.
				// e.g. normal order is 60 -> 69
				// reverse order is 84 -> 45
				log.info("Waiting for entropy/dynamic 2");
				s.findOrWaitForBuff(buffs, ba -> ba.buffIdMatches(DYNAMIC, ENTROPY) && ba.getEffectiveTimeSince().toMillis() < 5_000);
				s.waitMs(200);
				var dynEntro2 = buffs.findBuffs(ba -> ba.buffIdMatches(DYNAMIC, ENTROPY) && ba.getEffectiveTimeSince().toMillis() < 5_000)
						.stream()
						.peek(ba -> {
							if (!chReal2) {
								fakeDebuffs.add(ba);
							}
						})
						.findFirst()
						.orElseThrow();
				s.updateCall(getDynEntCall(
						dynEntro2.buffIdMatches(DYNAMIC),
						dynEntro2.getInitialDuration().toSeconds() > 60,
						chReal2), dynEntro2);

				// Wait for thunder charged
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x5CD));
				s.waitMs(6_300);
				{
					var debuff = findDurationBelow(12, dynEntro1, dynEntro2);
					if (debuff == null) {
						log.error("no early debuff!");
					}
					else {
						boolean fake = fakeDebuffs.contains(debuff);
						boolean isDonut = debuff.buffIdMatches(DYNAMIC) ^ fake;
						s.setParam("fake", fake);
						s.setParam("isDonut", isDonut);
						s.updateCall(ksFirstEntropyDynamic, debuff);

						var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB22, 0xBB25));
						s.updateCall(isDonut ? ksFirstEntropyDynamicStay : ksFirstEntropyDynamicMove, cast);
					}
				}
				// Get data about fake/real mana release
				var detail = s.waitEvent(KefkaManaChargeDetailEvent.class);
				// Wait for Mana Release
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBAA5));
				{
					var debuff = findDurationBelow(12, dynEntro1, dynEntro2);
					if (debuff == null) {
						log.error("no late debuff!");
					}
					else {
						boolean fake = fakeDebuffs.contains(debuff);
						boolean isDonut = debuff.buffIdMatches(DYNAMIC) ^ fake;
						s.setParam("fake", fake);
						s.setParam("isDonut", isDonut);
						ModifiableCallout<BuffApplied> firstCall;
						ModifiableCallout<AbilityCastStart> secondCall;
						if (detail.iceFake) {
							if (detail.thunderFake) {
								firstCall = ksSecondEntropyDynamicBothFake;
								secondCall = isDonut ? ksSecondEntropyDynamicStayBothFake : ksSecondEntropyDynamicMoveBothFake;
							}
							else {
								firstCall = ksSecondEntropyDynamicFakeIce;
								secondCall = isDonut ? ksSecondEntropyDynamicStayFakeIce : ksSecondEntropyDynamicMoveFakeIce;
							}
						}
						else {
							if (detail.thunderFake) {
								firstCall = ksSecondEntropyDynamicFakeThunder;
								secondCall = isDonut ? ksSecondEntropyDynamicStayFakeThunder : ksSecondEntropyDynamicMoveFakeThunder;
							}
							else {
								firstCall = ksSecondEntropyDynamicBothReal;
								secondCall = isDonut ? ksSecondEntropyDynamicStayBothReal : ksSecondEntropyDynamicMoveBothReal;
							}
						}
						s.updateCall(firstCall, debuff);
						var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBB22, 0xBB23, 0xBB24, 0xBB25));
						s.updateCall(secondCall, cast);
					}
				}
			});

	@NpcCastCallout(0xC24A)
	private final ModifiableCallout<AbilityCastStart> ultimaUpsurge = ModifiableCallout.durationBasedCall("Ultima Upsurge", "Raidwide");

	@NpcCastCallout(0xBABB)
	private final ModifiableCallout<AbilityCastStart> p4enrageFail = ModifiableCallout.durationBasedCall("P4 Enrage (Failed)", "Failed");

	EnumSetting<CleanseCallOption> getCleanseCallSetting() {
		return cleanseCallSetting;
	}
}
