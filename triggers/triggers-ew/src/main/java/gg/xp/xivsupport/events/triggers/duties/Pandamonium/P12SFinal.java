package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.HeadmarkerOffsetTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.List;

@CalloutRepo(name = "P12S Final Boss", duty = KnownDuty.P12S)
public class P12SFinal extends AutoChildEventHandler implements FilteredEventHandler {


	private XivState state;
	private StatusEffectRepository buffs;
	private final HeadmarkerOffsetTracker hmot;

	public P12SFinal(XivState state, StatusEffectRepository buffs, HeadmarkerOffsetTracker hmot) {
		this.state = state;
		this.buffs = buffs;
		this.hmot = hmot;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P12S);
	}

	@HandleEvents
	public void p2hmReset(EventContext context, BuffRemoved br) {
		if (br.buffIdMatches(0x968)) {
			hmot.reset();
		}
	}

	@NpcCastCallout({0x8682, 0x86f6})
	private final ModifiableCallout<AbilityCastStart> ultima = ModifiableCallout.durationBasedCall("Ultima", "Raidwide");
	@NpcCastCallout(0x8326)
	private final ModifiableCallout<AbilityCastStart> gaiaochos = ModifiableCallout.durationBasedCall("Gaiaochos", "Get In, Raidwide");

	@NpcCastCallout(0x832B)
	private final ModifiableCallout<AbilityCastStart> geoHoriz = ModifiableCallout.durationBasedCall("Geocentrism: Horizontal", "Horizontal");
	@NpcCastCallout(0x8329)
	private final ModifiableCallout<AbilityCastStart> geoVert = ModifiableCallout.durationBasedCall("Geocentrism: Vertical", "Vertical");
	@NpcCastCallout(0x832A)
	private final ModifiableCallout<AbilityCastStart> geoIn = ModifiableCallout.durationBasedCall("Geocentrism: Inside", "Inside");

	@NpcCastCallout(0x831A)
	private final ModifiableCallout<AbilityCastStart> palladianGrasp = ModifiableCallout.durationBasedCall("Palladian Grasp", "Tankbuster");

	private final ModifiableCallout<BuffApplied> cc1 = new ModifiableCallout<BuffApplied>("Classical Concepts 1", "Raidwide").autoIcon();
	private final ModifiableCallout<BuffApplied> circleAlpha = new ModifiableCallout<BuffApplied>("CC1: Circle Alpha", "Circle Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> circleBeta = new ModifiableCallout<BuffApplied>("CC1: Circle Beta", "Circle Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> squareAlpha = new ModifiableCallout<BuffApplied>("CC1: Square Alpha", "Square Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> squareBeta = new ModifiableCallout<BuffApplied>("CC1: Square Beta", "Square Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> triangleAlpha = new ModifiableCallout<BuffApplied>("CC1: Triangle Alpha", "Triangle Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> triangleBeta = new ModifiableCallout<BuffApplied>("CC1: Triangle Beta", "Triangle Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> crossAlpha = new ModifiableCallout<BuffApplied>("CC1: Cross Alpha", "Cross Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> crossBeta = new ModifiableCallout<BuffApplied>("CC1: Cross Beta", "Cross Beta").autoIcon();
	private final ModifiableCallout<?> afterClassical1 = new ModifiableCallout<>("CC1: After", "Dodge Orbs, Then Bait Cleaves");

	private final ModifiableCallout<BuffApplied> cc2 = new ModifiableCallout<BuffApplied>("Classical Concepts 2", "Raidwide").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2circleAlpha = new ModifiableCallout<BuffApplied>("CC2: Circle Alpha", "Circle Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2circleBeta = new ModifiableCallout<BuffApplied>("CC2: Circle Beta", "Circle Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2squareAlpha = new ModifiableCallout<BuffApplied>("CC2: Square Alpha", "Square Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2squareBeta = new ModifiableCallout<BuffApplied>("CC2: Square Beta", "Square Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2triangleAlpha = new ModifiableCallout<BuffApplied>("CC2: Triangle Alpha", "Triangle Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2triangleBeta = new ModifiableCallout<BuffApplied>("CC2: Triangle Beta", "Triangle Beta").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2crossAlpha = new ModifiableCallout<BuffApplied>("CC2: Cross Alpha", "Cross Alpha").autoIcon();
	private final ModifiableCallout<BuffApplied> cc2crossBeta = new ModifiableCallout<BuffApplied>("CC2: Cross Beta", "Cross Beta").autoIcon();
	private final ModifiableCallout<?> afterClassical2 = new ModifiableCallout<>("CC2: After", "Dodge Orbs, Then Bait Cleaves");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> classicalConcepts = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8331),
			(e1, s) -> {
				s.updateCall(cc1);
				HeadMarkerEvent myHm = s.waitEvent(HeadMarkerEvent.class, hm -> hm.getTarget().isThePlayer());
				BuffApplied alphaBeta = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xDE8, 0xDE9) && ba.getTarget().isThePlayer());
				boolean beta = alphaBeta.buffIdMatches(0xDE9);
				s.updateCall(switch (myHm.getMarkerOffset()) {
					case -101 -> beta ? circleBeta : circleAlpha;
					case -100 -> beta ? triangleBeta : triangleAlpha;
					case -99 -> beta ? squareBeta : squareAlpha;
					case -98 -> beta ? crossBeta : crossAlpha;
					default -> null;
				}, alphaBeta);
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xE04));
				s.updateCall(afterClassical1);
			}, (e1, s) -> {
				s.updateCall(cc2);
				HeadMarkerEvent myHm = s.waitEvent(HeadMarkerEvent.class, hm -> hm.getTarget().isThePlayer());
				BuffApplied alphaBeta = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xDE8, 0xDE9) && ba.getTarget().isThePlayer());
				boolean beta = alphaBeta.buffIdMatches(0xDE9);
				s.updateCall(switch (myHm.getMarkerOffset()) {
					case -101 -> beta ? cc2circleBeta : cc2circleAlpha;
					case -100 -> beta ? cc2triangleBeta : cc2triangleAlpha;
					case -99 -> beta ? cc2squareBeta : cc2squareAlpha;
					case -98 -> beta ? cc2crossBeta : cc2crossAlpha;
					default -> null;
				}, alphaBeta);
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xE04));
				s.updateCall(afterClassical2);

			});

	@NpcCastCallout(0x8317)
	private final ModifiableCallout<AbilityCastStart> crushHelm = ModifiableCallout.durationBasedCall("Crush Helm", "Tankbuster, Multiple Hits");

	private final ModifiableCallout<AbilityCastStart> caloricRaidwide = ModifiableCallout.durationBasedCall("Caloric Theory: Raidwide", "Raidwide");
	private final ModifiableCallout<?> caloricFireMarkers = new ModifiableCallout<>("Caloric Theory: Fire Markers", "Fire on {fire[0]} and {fire[1]}");
	private final ModifiableCallout<BuffApplied> caloricFireBuff = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric Theory: Fire Debuff", "Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> caloricWindBuff = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric Theory: Wind Debuff", "Wind").autoIcon();
	private final ModifiableCallout<BuffApplied> caloricFireBuff2 = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric Theory Part 2: Fire Debuff", "Stack on {nothings}").autoIcon();
	private final ModifiableCallout<BuffApplied> caloricWindBuff2lowStack = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric Theory Part 2: Wind Debuff, <=3 Stacks", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> caloricWindBuff2highStack = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric Theory Part 2: Wind Debuff, 4 Stacks", "Plant").autoIcon();
	private final ModifiableCallout<?> caloricNothing2 = new ModifiableCallout<>("Caloric Theory Part 2: Nothing", "Stack on {fires}").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> caloricTheory = SqtTemplates.multiInvocation(60_000,
			// The headmarkers happen before the cast starts, so trigger on the Crush Helm beforehand
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8317),
			(e1, s) -> {
				List<HeadMarkerEvent> flameMarks = s.waitEvents(2, HeadMarkerEvent.class, hm -> hm.getMarkerOffset() == -165);
				List<XivCombatant> initialFirePlayers = List.of(flameMarks.get(0).getTarget(), flameMarks.get(1).getTarget());
				s.setParam("fire", initialFirePlayers);
				s.updateCall(caloricFireMarkers);
				AbilityCastStart castBar = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x833D));
				s.waitMs(3_000);
				s.call(caloricRaidwide, castBar);
				BuffApplied myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xE06, 0xE07) && ba.getTarget().isThePlayer());
				if (myBuff.buffIdMatches(0xE06)) {
					s.updateCall(caloricFireBuff, myBuff);
				}
				else {
					s.updateCall(caloricWindBuff, myBuff);
				}
				s.waitMs(1_000);
				BuffApplied followUpFire = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xE06));
				s.waitMs(500);
				List<XivCombatant> newFires = buffs.findBuffs(ba -> ba.buffIdMatches(0xE06) && ba.getInitialDuration().toMillis() <= 11_500)
						.stream()
						.map(BuffApplied::getTarget)
						.toList();
				List<XivCombatant> winds = buffs.findBuffs(ba -> ba.buffIdMatches(0xE07))
						.stream()
						.map(BuffApplied::getTarget)
						.toList();
				s.setParam("nothings", state.getPartyList().stream().filter(player -> !(buffs.isStatusOnTarget(player, 0xE06) || buffs.isStatusOnTarget(player, 0xE07))).toList());
				s.setParam("fires", newFires);
				s.setParam("winds", winds);
				if (myBuff.buffIdMatches(0xE07)) {
					int stacks = buffs.buffStacksOnTarget(state.getPlayer(), 0xE05);
					s.setParam("stacks", stacks);
					if (stacks <= 3) {
						s.updateCall(caloricWindBuff2lowStack, myBuff);
					}
					else {
						s.updateCall(caloricWindBuff2highStack, myBuff);
					}
				}
				else {
					BuffApplied newFire = buffs.findStatusOnTarget(state.getPlayer(), 0xE06);
					if (newFire == null) {
						s.updateCall(caloricNothing2);
					}
					else {
						s.updateCall(caloricFireBuff2, newFire);
					}
				}
			});

	private final ModifiableCallout<?> pangNothing = new ModifiableCallout<>("Pangenesis: Nothing", "Nothing");
	private final ModifiableCallout<BuffApplied> pangOneStack = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: One Stack", "One Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> pangShortDark = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: Short Dark", "Short Dark").autoIcon();
	private final ModifiableCallout<BuffApplied> pangLongDark = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: Long Dark", "Long Dark").autoIcon();
	private final ModifiableCallout<BuffApplied> pangShortLight = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: Short Light", "Short Light").autoIcon();
	private final ModifiableCallout<BuffApplied> pangLongLight = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: Long Light", "Long Light").autoIcon();
	private final ModifiableCallout<BuffApplied> pangNewLight = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: New Light", "Soak Dark").autoIcon();
	private final ModifiableCallout<BuffApplied> pangNewDark = ModifiableCallout.<BuffApplied>durationBasedCall("Pangenesis: New Dark", "Soak Light").autoIcon();
	private final ModifiableCallout<?> pangPickUpTethers = new ModifiableCallout<>("Pangenesis: Pick Up Tethers", "Grab Tethers");
	private final ModifiableCallout<?> pangAvoidTethers = new ModifiableCallout<>("Pangenesis: Avoid Tethers", "Avoid Tethers");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> pangenesis = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x833F),
			(e1, s) -> {
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xE22));
				s.waitMs(300);
				BuffApplied unstable = buffs.findBuff(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xE09));
				BuffApplied tilt = buffs.findBuff(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDF8, 0xDF9));
				if (unstable == null) {
					s.updateCall(pangNothing);
				}
				else if (unstable.getStacks() == 1) {
					s.updateCall(pangOneStack, unstable);
				}
				else {
					boolean light = tilt.buffIdMatches(0xDF8);
					boolean longTilt = tilt.getInitialDuration().toSeconds() > 18;
					if (light) {
						if (longTilt) {
							s.updateCall(pangLongLight, unstable);
						}
						else {
							s.updateCall(pangShortLight, unstable);
						}
					}
					else {
						if (longTilt) {
							s.updateCall(pangLongDark, unstable);
						}
						else {
							s.updateCall(pangShortDark, unstable);
						}

					}
				}
				s.waitMs(5_000);
				while (true) {
					BuffApplied newTilt = s.waitEventUntil(
							BuffApplied.class, ba -> ba.buffIdMatches(0xDF8, 0xDF9) && ba.getTarget().isThePlayer(),
							TetherEvent.class, tether -> tether.tetherIdMatches(0x54));
					if (newTilt == null) {
						break;
					}
					boolean light = newTilt.buffIdMatches(0xDF8);
					if (light) {
						s.updateCall(pangNewLight, newTilt);
					}
					else {
						s.updateCall(pangNewDark, newTilt);
					}
				}
				if (unstable == null) {
					s.updateCall(pangPickUpTethers);
				}
				else {
					s.updateCall(pangAvoidTethers);
				}
			});

}
