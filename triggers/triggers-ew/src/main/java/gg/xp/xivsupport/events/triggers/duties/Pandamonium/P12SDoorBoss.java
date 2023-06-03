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
import gg.xp.xivsupport.events.state.combatstate.HeadmarkerOffsetTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "P12S Doorboss", duty = KnownDuty.P12S)
public class P12SDoorBoss extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P12SDoorBoss.class);

	private XivState state;
	private StatusEffectRepository buffs;
	private HeadmarkerOffsetTracker hmot;
	private final ArenaPos ap = new ArenaPos(100, 100, 5, 5);

	public P12SDoorBoss(XivState state, StatusEffectRepository buffs, HeadmarkerOffsetTracker hmot) {
		this.state = state;
		this.buffs = buffs;
		this.hmot = hmot;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P12S);
	}

	@NpcCastCallout(0x8304)
	private final ModifiableCallout<?> onTheSoul = ModifiableCallout.durationBasedCall("On the Soul", "Raidwide");

	// Trigger to fix headmarker offsets with some fakery
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hmoffFixer = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82E7, 0x82E8),
			(e1, s) -> {
				ArenaSector firstSafe = e1.abilityIdMatches(0x82E8) ? ArenaSector.EAST : ArenaSector.WEST;
				HeadMarkerEvent firstHm = s.waitEvent(HeadMarkerEvent.class, hm -> true);
				if (firstSafe == ArenaSector.WEST && firstHm.getMarkerOffset() == 0) {
					log.info("Overriding marker offset");
					hmot.setFakeFirstId(firstHm.getMarkerId() - 1);
				}
			});

	private final ModifiableCallout<?> trinityInitial = new ModifiableCallout<>("Trinity Safe Spots", "Start {safespots[0]}");
	private final ModifiableCallout<?> trinitySafeSpots = new ModifiableCallout<>("Trinity Safe Spots", "{safespots[0]}, {safespots[1]}, {safespots[2]}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinityOfSouls = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82E1, 0x82E2, 0x82E7, 0x82E8),
			(e1, s) -> {
					/*
						Notes: Seems to be cast -> headmarkers during cast
						Notes indicate SAFE SPOTS
						82E8, 490 488 497 = right left left, flipping (8AB)
						82E8, 490 488 497 = right left left, flipping (8AB)
						82E7, 491 488 497 = left left left, flipping (7AB)
						82E7, 491 488 497 = left left left, flipping (7AB)
						82E7, 491 488 496 = left left right, flipping (7AC)
						82E8, 490 489 496 = right right? right?, flipping (89C)
						82E7, 491 489 496 = left right right, flipping (79C)
						82E2, 486 488 499 = right right left, non flip (245)
						82E1, 487 488 499 = left right left, non flip (145)
						82E1, 487 488 498 = left right right, non flip (146)

						82E2 = right safe first, not flipping
						82E7 = left safe first, flipping
						82E8 = right safe first, flipping
						-1 = left safe (must consider flip)
						-2 = right safe (must consider flip)
						+7 = left safe
						+6 = right safe
					 */
				List<ArenaSector> safeSpots = new ArrayList<>(3);
				ArenaSector firstSafe = e1.abilityIdMatches(0x82E2, 0x82E8) ? ArenaSector.EAST : ArenaSector.WEST;
				safeSpots.add(firstSafe);
				s.setParam("safespots", safeSpots);
				s.updateCall(trinityInitial);
				List<HeadMarkerEvent> hms = s.waitEvents(3, HeadMarkerEvent.class, hm -> true);
				boolean flipping = e1.abilityIdMatches(0x82E7, 0x82E8);
				HeadMarkerEvent secondHm = hms.get(1);
				ArenaSector secondSafe = switch (secondHm.getMarkerOffset()) {
					case -1 -> ArenaSector.WEST;
					case -2 -> ArenaSector.EAST;
					default -> ArenaSector.UNKNOWN;
				};
				if (flipping) {
					secondSafe = secondSafe.opposite();
				}
				safeSpots.add(secondSafe);
				HeadMarkerEvent thirdHm = hms.get(2);
				ArenaSector thirdSafe = switch (thirdHm.getMarkerOffset()) {
					case +7, +9 -> ArenaSector.WEST;
					case +6, +8 -> ArenaSector.EAST;
					default -> ArenaSector.UNKNOWN;
				};
				safeSpots.add(thirdSafe);
				// TODO: test
				s.setParam("safespots", safeSpots);
				s.updateCall(trinitySafeSpots);
			});

	private final ModifiableCallout<TetherEvent> engravement1tetherLight = new ModifiableCallout<>("Engravement 1: Light Tether", "Light Tether");
	private final ModifiableCallout<TetherEvent> engravement1tetherDark = new ModifiableCallout<>("Engravement 1: Dark Tether", "Dark Tether");
	private final ModifiableCallout<BuffApplied> engravement1soakLight = new ModifiableCallout<>("Engravement 1: Soak Light", "Soak Light");
	private final ModifiableCallout<BuffApplied> engravement1soakDark = new ModifiableCallout<>("Engravement 1: Soak Dark", "Soak Dark");
	private final ModifiableCallout<BuffApplied> engravement1noTetherLight = new ModifiableCallout<>("Engravement 1: No Tether, Light", "No Tether, Light");
	private final ModifiableCallout<BuffApplied> engravement1noTetherDark = new ModifiableCallout<>("Engravement 1: No Tether, Dark", "No Tether, Dark");
	private final ModifiableCallout<?> engravement1dodgeLeft = new ModifiableCallout<>("Engravement 1: Dodge Inner Left (or Outer Right)", "Dodge Left");
	private final ModifiableCallout<?> engravement1dodgeRight = new ModifiableCallout<>("Engravement 1: Dodge Inner Right (or Outer Left)", "Dodge Right");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> engravementOfSouls = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8305),
			(e1, s) -> {
				log.info("Engravement 1: Start");
				List<TetherEvent> tethers = s.waitEvents(4, TetherEvent.class, te -> true);
				Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
				myTether.ifPresentOrElse(mt -> {
							switch ((int) mt.getId()) {
								// IDs are unstretched and stretched
								case 233, 250 -> s.updateCall(engravement1tetherLight, mt);
								case 234, 251 -> s.updateCall(engravement1tetherDark, mt);
								default -> log.error("Unknown tether: {}", mt.getId());
							}
							BuffApplied buff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDF8, 0xDF9));
							switch ((int) buff.getBuff().getId()) {
								case 0xDF8 -> s.updateCall(engravement1soakDark, buff);
								case 0xDF9 -> s.updateCall(engravement1soakLight, buff);
								default -> log.error("Unknown buff: {}", mt.getId());
							}
						},
						() -> {
							s.waitMs(200);
							BuffApplied myBuff = buffs.findBuff(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDFB, 0xDFC));
							if (myBuff == null) {
								log.error("Engravement 1: No buff!");
							}
							switch ((int) myBuff.getBuff().getId()) {
								case 0xDFB -> s.updateCall(engravement1noTetherLight, myBuff);
								case 0xDFC -> s.updateCall(engravement1noTetherDark, myBuff);
							}
						});
				List<AbilityCastStart> rays = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82EE));
				s.waitThenRefreshCombatants(100);
				if (state.getLatestCombatantData(rays.get(0).getSource()).getPos().y() > 80) {
					log.warn("Unsure of what to do with rays!");
				}
				double maxX = rays.stream()
						.map(AbilityCastStart::getSource)
						.map(state::getLatestCombatantData)
						.map(XivCombatant::getPos)
						.mapToDouble(Position::x)
						.max()
						.getAsDouble();
				if (maxX < 106) {
					s.updateCall(engravement1dodgeLeft);
				}
				else {
					s.updateCall(engravement1dodgeRight);
				}
			}, (e1, s) -> { /* Superchain 1 */ });

	@NpcCastCallout(0x82FC)
	private final ModifiableCallout<AbilityCastStart> glaukopis = ModifiableCallout.durationBasedCall("Glaukopis", "Tank Buster");

	// "Mechanics" orbs immediately precede their fixed orb in entity ID
	private List<XivCombatant> getOrbChainActors(XivCombatant fixedOrb) {
		long id = fixedOrb.getId();
		List<XivCombatant> out = new ArrayList<>();
		while (true) {
			id--;
			XivCombatant maybeOrb = state.getCombatant(id);
			OrbMechanic mech = OrbMechanic.forNpc(maybeOrb);
			if (mech == null || mech == OrbMechanic.FIXED_ORB) {
				break;
			}
			out.add(maybeOrb);
		}
		return out;
	}

	private enum OrbMechanic implements HasFriendlyName {
		FIXED_ORB(16176, "Fixed"),
		OUT(16177, "Out"),
		IN(16178, "In"),
		PROTEAN(16179, "Protean"),
		BUDDIES(16180, "Buddies");

		private final int npcId;
		private final String friendlyName;

		OrbMechanic(int npcId, String friendlyName) {
			this.npcId = npcId;
			this.friendlyName = friendlyName;
		}

		@Override
		public String getFriendlyName() {
			return friendlyName;
		}

		public static @Nullable OrbMechanic forNpc(XivCombatant cbt) {
			if (cbt == null) {
				return null;
			}
			long id = cbt.getbNpcId();
			for (OrbMechanic value : values()) {
				if (value.npcId == id) {
					return value;
				}
			}
			return null;
		}
	}

	private final ModifiableCallout<?> superchain1start = new ModifiableCallout<>("Superchain 1: Start", "Start {startOrb}, {firstMechs[0]} and {firstMechs[1]}", 10_000);
	private final ModifiableCallout<?> superchain1second = new ModifiableCallout<>("Superchain 1: Second Orb", "Next: stack in {secondSafe}", 10_000);
	private final ModifiableCallout<BuffApplied> superchain1lightStack = new ModifiableCallout<>("Superchain 1: Light Stack Group", "Next: {secondSafe}, Light Stack");
	private final ModifiableCallout<BuffApplied> superchain1darkStack = new ModifiableCallout<>("Superchain 1: Dark Stack Group", "Next: {secondSafe}, Dark Stack");
	private final ModifiableCallout<BuffApplied> superchain1lightLaser = new ModifiableCallout<>("Superchain 1: Light Laser", "Next: {secondSafe}, Light Laser on You");
	private final ModifiableCallout<BuffApplied> superchain1darkLaser = new ModifiableCallout<>("Superchain 1: Dark Laser", "Next: {secondSafe}, Dark Laser on You");
	private final ModifiableCallout<?> superchain1final = new ModifiableCallout<>("Superchain 1: Final Orb", "Last: {finalOrb}, {finalMechs[0]} then {finalMechs[1]}", 10_000);
	@AutoFeed
	private final SequentialTrigger<BaseEvent> superchain1 = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82DA),
			(e1, s) -> {
				log.info("SC1 start");
				/*
					Example 1:
					Start SE (out + pairs)
					Then SW (in) + NE (out)
					Then NW (in)
					Then NW (out)
					Probably not StatusLoopVFX, they're all #583

					Fixed part is 16176, "out" is 16177, "buddies" is 16180, "in" is 16178, "protean" is (unverified) 16179?

					SE is c239 fixed (npc 16176), c237 (npc 16180), c238 (npc 16177)
					NE is c23f fixed (npc 16176), c23e (npc 16177)
					SW is c241 fixed (npc 16176), c240 (npc 16178)
					NW is c24a fixed (npc 16176), c248 (npc 16177), c249 (npc 16178)

				 */
				s.waitEvent(AbilityCastStart.class, event -> event.abilityIdMatches(0x8305));
				s.waitThenRefreshCombatants(300);
				XivCombatant firstOrb = state.npcById(16176);
				if (firstOrb == null) {
					log.error("No first orb!");
					return;
				}
				log.info("First orb");
				ArenaSector start = ap.forCombatant(firstOrb);
				s.setParam("startOrb", start);
				List<OrbMechanic> firstMechs = getOrbChainActors(firstOrb).stream().map(OrbMechanic::forNpc).toList();
				s.setParam("firstMechs", firstMechs);
				s.updateCall(superchain1start);
				List<XivCombatant> orbs;
				do {
					s.waitThenRefreshCombatants(100);
					orbs = state.npcsById(16176);
				} while (orbs.size() < 3);
				XivCombatant inOrb = orbs.subList(1, orbs.size())
						.stream()
						.filter(cbt -> getOrbChainActors(cbt).stream().anyMatch(mechOrb -> OrbMechanic.forNpc(mechOrb) == OrbMechanic.IN))
						.findFirst()
						.orElseThrow(() -> new RuntimeException("Couldn't find 'in' orb!"));
				log.info("Second/third orb found");
				s.setParam("secondSafe", ap.forCombatant(inOrb));
				BuffApplied myBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0xdf8, 0xdf9, 0xdfb, 0xdfc, 0xdfd, 0xdfe));
				ModifiableCallout<BuffApplied> secondMechBuffCall = null;
				if (myBuff != null) {
					secondMechBuffCall = switch ((int) myBuff.getBuff().getId()) {
						case 0xdf8, 0xdfb -> superchain1lightStack;
						case 0xdf9, 0xdfc -> superchain1darkStack;
						case 0xdfd -> superchain1lightLaser;
						case 0xdfe -> superchain1darkLaser;
						default -> null;
					};
				}
				s.waitMs(3_000);
				if (secondMechBuffCall != null) {
					s.call(secondMechBuffCall, myBuff);
				}
				else {
					log.error("Unknown second mech buff");
					s.call(superchain1second);
				}
				// TODO: remove when buff call is done, integrate location into buff call
				do {
					s.waitThenRefreshCombatants(100);
					orbs = state.npcsById(16176);
				} while (orbs.size() < 4);
				XivCombatant finalOrb = orbs.get(3);
				log.info("Final orb found");
				List<XivCombatant> finalChainActors = getOrbChainActors(finalOrb)
						.stream()
						.sorted(Comparator.comparing(a -> a.getPos().distanceFrom2D(finalOrb.getPos())))
						.toList();
				OrbMechanic finalMech1 = OrbMechanic.forNpc(finalChainActors.get(0));
				OrbMechanic finalMech2 = OrbMechanic.forNpc(finalChainActors.get(1));
				s.setParam("finalMechs", List.of(finalMech1, finalMech2));
				s.setParam("finalOrb", ap.forCombatant(finalOrb));
				s.waitMs(5_000);
				s.call(superchain1final);
			});

	private final ModifiableCallout<TetherEvent> paradeigma3lightTether = new ModifiableCallout<>("Paradeigma 3: Light Tether", "Light Tether");
	private final ModifiableCallout<TetherEvent> paradeigma3darkTether = new ModifiableCallout<>("Paradeigma 3: Dark Tether", "Dark Tether");

	private final ModifiableCallout<BuffApplied> paradeigma3light = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Light Buff", "Light Buff").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3dark = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Dark Buff", "Dark Buff").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3plus = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Plus", "Plus").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3cross = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Cross", "Cross").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> paradeigma = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82ED),
			(e1, s) -> {
				// not needed
			},
			(e1, s) -> {
				// not needed
			},
			(e1, s) -> {
				// arena break mech
				List<TetherEvent> tethers = s.waitEvents(4, TetherEvent.class, te -> true);
				Optional<TetherEvent> myTetherMaybe = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
				if (myTetherMaybe.isPresent()) {
					TetherEvent myTether = myTetherMaybe.get();
					if (myTether.tetherIdMatches(233, 250)) {
						s.updateCall(paradeigma3lightTether, myTether);
					}
					else {
						s.updateCall(paradeigma3darkTether, myTether);
					}
				}
				else {
					// DFB - light
					// DFC - dark
					// DFF - plus
					// E00 - X
					BuffApplied buff = buffs.findBuff(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xDFB, 0xDFC, 0xDFF, 0xE00));
					if (buff == null) {
						log.error("PD3: No tether nor buff!");
						return;
					}
					s.updateCall(switch ((int) buff.getBuff().getId()) {
						case 0xDFB -> paradeigma3light;
						case 0xDFC -> paradeigma3dark;
						case 0xDFF -> paradeigma3plus;
						case 0xE00 -> paradeigma3cross;
						default -> throw new RuntimeException("Unknown Buff");
					}, buff);
				}
			}

	);
}
