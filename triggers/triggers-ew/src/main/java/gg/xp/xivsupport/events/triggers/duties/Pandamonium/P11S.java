package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.apache.commons.lang3.CharSequenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CalloutRepo(name = "P11S", duty = KnownDuty.P11S)
public class P11S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P11S.class);

	private XivState state;
	private StatusEffectRepository buffs;
	private ActiveCastRepository acr;
	private ArenaPos ap = new ArenaPos(100, 100, 2, 2);

	public P11S(XivState state, StatusEffectRepository buffs, ActiveCastRepository acr) {
		this.state = state;
		this.buffs = buffs;
		this.acr = acr;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P11S);
	}


	@NpcCastCallout(0x822B)
	private final ModifiableCallout<AbilityCastStart> eunomia = ModifiableCallout.<AbilityCastStart>durationBasedCall("Eunomia", "Raidwide with Bleed").statusIcon(0x828);
	@NpcCastCallout(0x822D)
	private final ModifiableCallout<AbilityCastStart> dike = ModifiableCallout.durationBasedCall("Dike", "Tankbuster");
	@NpcCastCallout(0x8217)
	private final ModifiableCallout<AbilityCastStart> styx = ModifiableCallout.durationBasedCall("Styx", "Party Stack, Multiple Hits");

	@NpcCastCallout({0x820D, 0x820F})
	private final ModifiableCallout<AbilityCastStart> arcaneRevDarkSafe = ModifiableCallout.durationBasedCall("Arcane Revelation: Dark Safe", "Dark Safe");
	@NpcCastCallout({0x820E, 0x8210})
	private final ModifiableCallout<AbilityCastStart> arcaneRevLightSafe = ModifiableCallout.durationBasedCall("Arcane Revelation: Light Safe", "Light Safe");

	@NpcCastCallout(0x81E6)
	private final ModifiableCallout<AbilityCastStart> joTwistLp = ModifiableCallout.durationBasedCall("Jury Overruling: Twisters then Light Parties In", "Twisters then Light Parties");
	@NpcCastCallout(0x81E7)
	private final ModifiableCallout<AbilityCastStart> joProtBuddies = ModifiableCallout.durationBasedCall("Jury Overruling: Proteans then Buddies", "Proteans then Buddies Out");
	@NpcCastCallout(0x87D3)
	private final ModifiableCallout<AbilityCastStart> uoStackOutLp = ModifiableCallout.durationBasedCall("Upheld Overruling: Stack then Light Parties", "Stack then Light Parties Out");
	@NpcCastCallout(0x87D4)
	private final ModifiableCallout<AbilityCastStart> uoTanksInBuddiesIn = ModifiableCallout.durationBasedCall("Upheld Overruling: Tanks In then Buddies In", "Tanks In then Buddies In");
	@NpcCastCallout(0x81EC)
	private final ModifiableCallout<AbilityCastStart> doSidesLp = ModifiableCallout.durationBasedCall("Divisive Overruling: Sides then LP Out", "Sides then Light Parties Out");
	@NpcCastCallout(0x81ED)
	private final ModifiableCallout<AbilityCastStart> doSidesBuddyIn = ModifiableCallout.durationBasedCall("Divisive Overruling: Sides then Buddies In", "Sides then Buddies In");
	/*
		Jury Overruling:
		81E7 = proteans + out, buddies + out
		Upheld Overruling:
		87D3 = stack middle, out + LP
		87D4 = Tanks in, then buddies and in
		Divisive Overruling:
		81EC = Sides, far out, light parties
	 */

	/*
		Boxes:
		820D, 8785, 8789, 8787, 4x 8213 - purple good, buddies
	 */

	/*
		Shadowed Messengers:
		8219, 821B, 821E, 821F, 821C, 821A,821C,821D,87B4,87B8,87B5,87B7
		Clockwise, rotate-rotate-in, buddies
	 */

	// Dismissal overruling:
	/*
		KB + in + buddies = 8787 and 8785
		KB + out + lp = 8786 + 8784
	 */
	@NpcCastCallout(0x8784)
	private final ModifiableCallout<AbilityCastStart> dismissalOutLp = ModifiableCallout.durationBasedCall("Dismissal Overruling: KB/Out/Light Parties", "Knockback, Out, Light Parties");
	@NpcCastCallout(0x8785)
	private final ModifiableCallout<AbilityCastStart> dismissalInBuddy = ModifiableCallout.durationBasedCall("Dismissal Overruling: KB/In/Buddies", "Knockback, In, Buddies");

	private final ModifiableCallout<?> shadowedMessengerCW = new ModifiableCallout<>("Shadowed Messenger: Clockwise", "Clockwise");
	private final ModifiableCallout<?> shadowedMessengerCCW = new ModifiableCallout<>("Shadowed Messenger: Counter-Clockwise", "Counter-Clockwise");
	private final ModifiableCallout<?> shadowedMessengerLightTether = new ModifiableCallout<>("Shadowed Messenger: Light Tether", "Light Tether");
	private final ModifiableCallout<?> shadowedMessengerDarkTether = new ModifiableCallout<>("Shadowed Messenger: Dark Tether", "Dark Tether");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> shadowedMessengersSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8219),
			(e1, s) -> {
				// 821B purple
				// 821A yellow
				// go from yellow to purple direction
				List<AbilityCastStart> casts = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x821A, 0x821B))
						.stream()
						.sorted(Comparator.comparing(i -> i.getAbility().getId()))
						.toList();
				ArenaSector yellow = ap.forCombatant(casts.get(0).getSource());
				ArenaSector purple = ap.forCombatant(casts.get(1).getSource());
				int diff = yellow.eighthsTo(purple);
				if (diff > 0) {
					s.updateCall(shadowedMessengerCW);
				}
				else if (diff < 0) {
					s.updateCall(shadowedMessengerCCW);
				}
				else {
					log.error("Couldn't determine direction! {} {}", yellow, purple);
				}
				List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, te -> true);
				s.waitMs(250);
				tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findAny()
								.ifPresent(te -> {
									CastTracker ct = acr.getCastFor(te.getTargetMatching(XivCombatant::isThePlayer));
									if (ct.getCast().abilityIdMatches(0x87D1)) {
										s.updateCall(shadowedMessengerDarkTether);
									}
									else {
										s.updateCall(shadowedMessengerLightTether);
									}
								});
			});

	@NpcCastCallout(0x87B4)
	private final ModifiableCallout<AbilityCastStart> divisiveOverrulingSidesInBuddies = ModifiableCallout.durationBasedCall("Divisive Ruling: Sides, In, Buddies (During Other Mechs)", "Sides then In and Buddies");
	@NpcCastCallout(0x87B3)
	private final ModifiableCallout<AbilityCastStart> divisiveOverrulingOutLightParties = ModifiableCallout.durationBasedCall("Divisive Ruling: Sides, Out, Light Parties (During Other Mechs)", "Sides and Light Parties");

	private final ModifiableCallout<?> spinnySafeSpot = new ModifiableCallout<>("Spinners: Safe Spot", "{safe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> orbSolver = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8203),
			(e1, s) -> {
				List<HeadMarkerEvent> markers = s.waitEvents(3, HeadMarkerEvent.class, hm -> true);
				// true == clockwise
				Map<XivCombatant, Boolean> mapping = markers.stream().collect(Collectors.toMap(event -> event.getTarget(), event -> event.getMarkerOffset() == -319));
				List<ArenaSector> safe = new ArrayList<>(List.of(ArenaSector.WEST, ArenaSector.NORTHWEST, ArenaSector.NORTHEAST, ArenaSector.EAST, ArenaSector.SOUTHEAST, ArenaSector.SOUTHWEST));
				mapping.forEach((cbt, clockwise) -> {
					cbt = state.getLatestCombatantData(cbt);
					ArenaSector as = ap.forCombatant(cbt);
					log.info("Spinnies: {} rotating {}", as, clockwise ? "Clockwise" : "CCW");
					switch (as) {
						case NORTH -> {
							if (clockwise) {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.NORTHWEST);
								safe.remove(ArenaSector.SOUTHWEST);
							}
							else {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.NORTHEAST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
						}
						case NORTHEAST -> {
							if (clockwise) {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.NORTHWEST);
								safe.remove(ArenaSector.NORTHEAST);
							}
							else {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.SOUTHWEST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
						}
						case SOUTHEAST -> {
							if (clockwise) {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.NORTHEAST);
								safe.remove(ArenaSector.NORTHWEST);
							}
							else {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.SOUTHWEST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
						}
						case SOUTH -> {
							if (clockwise) {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.NORTHEAST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
							else {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.NORTHWEST);
								safe.remove(ArenaSector.SOUTHWEST);
							}
						}
						case SOUTHWEST -> {
							if (clockwise) {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.SOUTHWEST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
							else {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.NORTHWEST);
								safe.remove(ArenaSector.NORTHEAST);
							}
						}
						case NORTHWEST -> {
							if (clockwise) {
								safe.remove(ArenaSector.WEST);
								safe.remove(ArenaSector.SOUTHWEST);
								safe.remove(ArenaSector.SOUTHEAST);
							}
							else {
								safe.remove(ArenaSector.EAST);
								safe.remove(ArenaSector.NORTHEAST);
								safe.remove(ArenaSector.NORTHWEST);
							}
						}
						default -> {
							log.error("Bad position for spinny! {}", cbt);
						}
					}
					log.info("Remaining safe spots: {}", safe);
				});
				if (safe.size() == 1) {
					s.setParam("safe", safe.get(0));
					s.updateCall(spinnySafeSpot);
				}
				else {
					log.error("Bad safe spots: {}", safe);
				}
			});

	private final ModifiableCallout<BuffApplied> darkWithDark = new ModifiableCallout<BuffApplied>("Dark and Light: Dark Tethered to Dark", "Dark with Dark ({buddy})").autoIcon();
	private final ModifiableCallout<BuffApplied> darkWithLight = new ModifiableCallout<BuffApplied>("Dark and Light: Dark Tethered to Light", "Dark with Light ({buddy})").autoIcon();
	private final ModifiableCallout<BuffApplied> lightWithDark = new ModifiableCallout<BuffApplied>("Dark and Light: Light Tethered to Dark", "Light with Dark ({buddy})").autoIcon();
	private final ModifiableCallout<BuffApplied> lightWithLight = new ModifiableCallout<BuffApplied>("Dark and Light: Light Tethered to Light", "Light with Light ({buddy})").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> darkLightTethers = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x81FE),
			(e1, s) -> {
				TetherEvent myTether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant buddy = myTether.getTargetMatching(t -> !t.isThePlayer());
				s.setParam("buddy", buddy);
				boolean imLight;
				boolean buddyLight;
				BuffApplied myBuff = buffs.findBuff(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDE1, 0xDE2, 0xDE3, 0xDE4));
				BuffApplied buddyBuff = buffs.findBuff(ba -> ba.getTarget().equals(buddy) && ba.buffIdMatches(0xDE1, 0xDE2, 0xDE3, 0xDE4));
				imLight = myBuff.buffIdMatches(0xDE1, 0xDE3);
				buddyLight = buddyBuff.buffIdMatches(0xDE1, 0xDE3);
				s.setParam("imLight", imLight);
				s.setParam("buddyLight", buddyLight);
				if (imLight) {
					if (buddyLight) {
						s.updateCall(lightWithLight, myBuff);
					}
					else {
						s.updateCall(lightWithDark, myBuff);
					}
				}
				else {
					if (buddyLight) {
						s.updateCall(darkWithLight, myBuff);
					}
					else {
						s.updateCall(darkWithDark, myBuff);
					}
				}
			});

	@NpcCastCallout(0x8211)
	private final ModifiableCallout<AbilityCastStart> twofoldRevelationDarkSafe = new ModifiableCallout<>("Twofold Revelation: Dark Safe", "Dark Safe");
	@NpcCastCallout(0x8212)
	private final ModifiableCallout<AbilityCastStart> twofoldRevelationLightSafe = new ModifiableCallout<>("Twofold Revelation: Light Safe", "Light Safe");

	// TODO: rotating exaflares, light tether stack then go to tank tether mechs (on both clones and towers)
	// TODO: KB + CW/CCW rotation for towers mechanic
}
