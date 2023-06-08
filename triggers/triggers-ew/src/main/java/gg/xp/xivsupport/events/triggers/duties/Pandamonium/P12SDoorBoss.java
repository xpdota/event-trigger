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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.HeadmarkerOffsetTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.events.TrinityFullEvent;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.events.TrinityInitialEvent;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@CalloutRepo(name = "P12S Doorboss", duty = KnownDuty.P12S)
public class P12SDoorBoss extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P12SDoorBoss.class);

	private XivState state;
	private StatusEffectRepository buffs;
	private HeadmarkerOffsetTracker hmot;
	private final ArenaPos ap = new ArenaPos(100, 100, 5, 5);
	private ArenaPos sc2bAp = new ArenaPos(100, 100, 3, 3);

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

	private final ModifiableCallout<?> trinityInitial = new ModifiableCallout<>("Trinity Safe Spots", "Start {safespots[0]}")
			.extendedDescription("""
					There are multiple ways you can configure this callout.
					{safespots[n]} will tell you the actual compass direction
					after considering the boss's facing angle.
					e.g. if the boss is facing Southwest, "Left" is Southeast,
					and "Right" is Northwest.
										
					Alternatively, {right[n]} is a boolean which tells you if you
					should be on the right-hand side relative to the boss's
					original facing angle, e.g. {right[n] ? "Right" : "Left"}.
										
					Also note that this call is NOT used during Superchain IIA.
					Instead, this information is part of the calls specifically
					for that mechanic.""");
	private final ModifiableCallout<?> trinitySafeSpots = new ModifiableCallout<>("Trinity Safe Spots", "{safespots[0]}, {safespots[1]}, {safespots[2]}");

	private volatile boolean trinitySuppress;

	@HandleEvents
	public void reset(EventContext context, DutyCommenceEvent event) {
		trinitySuppress = false;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinityOfSoulsCaller = SqtTemplates.sq(60_000,
			TrinityInitialEvent.class, event -> true,
			(e1, s) -> {
				if (trinitySuppress) {
					log.info("Trinity: Suppressed");
					trinitySuppress = false;
					return;
				}
				s.setParam("right", List.of(e1.isRightSafe()));
				s.setParam("safespots", List.of(e1.getSafeSpot()));
				s.updateCall(trinityInitial);
				TrinityFullEvent full = s.waitEvent(TrinityFullEvent.class);
				s.setParam("right", full.getRightSafe());
				s.setParam("safespots", full.getSafeSpots());
				s.updateCall(trinitySafeSpots);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinityOfSoulsFeeder = SqtTemplates.sq(60_000,
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
				List<Boolean> right = new ArrayList<>(3);
				s.waitThenRefreshCombatants(100);
				ArenaSector bossFacingInitial = ArenaPos.combatantFacing(state.getLatestCombatantData(e1.getSource()));
				boolean firstRight = e1.abilityIdMatches(0x82E2, 0x82E8);
				ArenaSector firstSafe = firstRight ? bossFacingInitial.plusQuads(1) : bossFacingInitial.plusQuads(-1);
				right.add(firstRight);
				safeSpots.add(firstSafe);
				s.accept(new TrinityInitialEvent(firstRight, firstSafe));
				List<HeadMarkerEvent> hms = s.waitEvents(3, HeadMarkerEvent.class, hm -> true);
				boolean flipping = e1.abilityIdMatches(0x82E7, 0x82E8);
				HeadMarkerEvent secondHm = hms.get(1);
				Boolean secondRight = switch (secondHm.getMarkerOffset()) {
					// Left, unless flipping
					case -1 -> flipping;
					// Right, unless flipping
					case -2 -> !flipping;
					// Unknown
					default -> null;
				};
				right.add(secondRight);
				if (secondRight == null) {
					safeSpots.add(ArenaSector.UNKNOWN);
				}
				else if (secondRight) {
					safeSpots.add(bossFacingInitial.plusQuads(1));
				}
				else {
					safeSpots.add(bossFacingInitial.plusQuads(-1));
				}
				HeadMarkerEvent thirdHm = hms.get(2);
				Boolean thirdRight = switch (thirdHm.getMarkerOffset()) {
					case +7, +9 -> false;
					case +6, +8 -> true;
					default -> null;
				};
				right.add(thirdRight);
				if (thirdRight == null) {
					safeSpots.add(ArenaSector.UNKNOWN);
				}
				else if (thirdRight) {
					safeSpots.add(bossFacingInitial.plusQuads(1));
				}
				else {
					safeSpots.add(bossFacingInitial.plusQuads(-1));
				}
				s.accept(new TrinityFullEvent(right, safeSpots));
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
	private List<XivCombatant> getOrbChainActors(XivCombatant fixedOrb, int limit) {
		long id = fixedOrb.getId();
		List<XivCombatant> out = new ArrayList<>();
		while (limit < 0 || limit-- > 0) {
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
	private final ModifiableCallout<BuffApplied> superchain1lightStack = new ModifiableCallout<BuffApplied>("Superchain 1: Light Stack Group", "Next: {secondSafe}, Light Stack", 10_000).autoIcon();
	private final ModifiableCallout<BuffApplied> superchain1darkStack = new ModifiableCallout<BuffApplied>("Superchain 1: Dark Stack Group", "Next: {secondSafe}, Dark Stack", 10_000).autoIcon();
	private final ModifiableCallout<BuffApplied> superchain1lightLaser = new ModifiableCallout<BuffApplied>("Superchain 1: Light Laser", "Next: {secondSafe}, Light Laser on You", 10_000).autoIcon();
	private final ModifiableCallout<BuffApplied> superchain1darkLaser = new ModifiableCallout<BuffApplied>("Superchain 1: Dark Laser", "Next: {secondSafe}, Dark Laser on You", 10_000).autoIcon();
	private final ModifiableCallout<?> superchain1final = new ModifiableCallout<>("Superchain 1: Final Orb", "Last: {finalOrb}, {finalMechs[0]} then {finalMechs[1]}", 10_000);

	private final ModifiableCallout<BuffApplied> superchain1lightTowerEnd = new ModifiableCallout<BuffApplied>("Superchain 1 End: Light Tower", "Place Light Tower", 10_000).statusIcon(0xDFB);
	private final ModifiableCallout<BuffApplied> superchain1darkTowerEnd = new ModifiableCallout<BuffApplied>("Superchain 1: Dark Tower", "Place Dark Tower", 10_000).statusIcon(0xDFC);
	private final ModifiableCallout<BuffApplied> superchain1lightLaserEnd = new ModifiableCallout<BuffApplied>("Superchain 1 End: Was Light Laser", "Soak Dark Tower", 10_000);
	private final ModifiableCallout<BuffApplied> superchain1darkLaserEnd = new ModifiableCallout<BuffApplied>("Superchain 1 End: Was Dark Laser", "Soak Light Tower", 10_000);
	private final ModifiableCallout<BuffApplied> superchain1HolyEnd = new ModifiableCallout<BuffApplied>("Superchain 1: Holy", "Holy").statusIcon(0xDFA);

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
				List<OrbMechanic> firstMechs = getOrbChainActors(firstOrb, -1).stream().map(OrbMechanic::forNpc).toList();
				s.setParam("firstMechs", firstMechs);
				s.updateCall(superchain1start);
				List<XivCombatant> orbs;
				BuffApplied myBuff;
				do {
					s.waitThenRefreshCombatants(200);
					orbs = state.npcsById(16176);
					myBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0xdf8, 0xdf9, 0xdfb, 0xdfc, 0xdfd, 0xdfe));
				} while (orbs.size() < 3 || myBuff == null);
				XivCombatant inOrb = orbs.subList(1, orbs.size())
						.stream()
						.filter(cbt -> getOrbChainActors(cbt, -1).stream().anyMatch(mechOrb -> OrbMechanic.forNpc(mechOrb) == OrbMechanic.IN))
						.findFirst()
						.orElseThrow(() -> new RuntimeException("Couldn't find 'in' orb!"));
				log.info("Second/third orb found");
				s.setParam("secondSafe", ap.forCombatant(inOrb));
				log.info("Second buff: {}", myBuff);
				ModifiableCallout<BuffApplied> secondMechBuffCall = switch ((int) myBuff.getBuff().getId()) {
					case 0xdf8, 0xdfb -> superchain1lightStack;
					case 0xdf9, 0xdfc -> superchain1darkStack;
					case 0xdfd -> superchain1lightLaser;
					case 0xdfe -> superchain1darkLaser;
					default -> null;
				};
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
				List<XivCombatant> finalChainActors = getOrbChainActors(finalOrb, -1)
						.stream()
						.sorted(Comparator.comparing(a -> a.getPos().distanceFrom2D(finalOrb.getPos())))
						.toList();
				OrbMechanic finalMech1 = OrbMechanic.forNpc(finalChainActors.get(0));
				OrbMechanic finalMech2 = OrbMechanic.forNpc(finalChainActors.get(1));
				s.setParam("finalMechs", List.of(finalMech1, finalMech2));
				s.setParam("finalOrb", ap.forCombatant(finalOrb));
				s.waitMs(6_500);
				s.call(superchain1final);
				s.waitMs(7_500);
				ModifiableCallout<BuffApplied> finalMechBuffCall = switch ((int) myBuff.getBuff().getId()) {
					case 0xdfb -> superchain1lightTowerEnd;
					case 0xdfc -> superchain1darkTowerEnd;
					case 0xdfd -> superchain1lightLaserEnd;
					case 0xdfe -> superchain1darkLaserEnd;
					default -> superchain1HolyEnd;
				};
				s.call(finalMechBuffCall);
			});

	@NpcCastCallout(0x82FE)
	private final ModifiableCallout<AbilityCastStart> apodialogos = ModifiableCallout.durationBasedCall("Apodialogos", "Party In, Tanks Out");
	@NpcCastCallout(0x82FF)
	private final ModifiableCallout<AbilityCastStart> peridialogos = ModifiableCallout.durationBasedCall("Peridialogos", "Party Stack Out, Tanks In");


	private final ModifiableCallout<TetherEvent> paradeigma3lightTether = new ModifiableCallout<TetherEvent>("Paradeigma 3: Light Tether", "Light Tether")
			.extendedDescription("Intended for https://raidplan.io/plan/56FZFwTFz2e1xIq9");
	private final ModifiableCallout<TetherEvent> paradeigma3darkTether = new ModifiableCallout<>("Paradeigma 3: Dark Tether", "Dark Tether");

	private final ModifiableCallout<BuffApplied> paradeigma3light = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Light Buff", "Light Buff").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3dark = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Dark Buff", "Dark Buff").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3plus = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Plus", "Plus").autoIcon();
	private final ModifiableCallout<BuffApplied> paradeigma3cross = ModifiableCallout.<BuffApplied>durationBasedCall("Paradeigma 3: Cross", "Cross").autoIcon();

	private final ModifiableCallout<?> paradeigma3plusPart2 = new ModifiableCallout<>("Paradeigma 3: Drop Plus", "Drop Plus");
	private final ModifiableCallout<?> paradeigma3crossPart2 = new ModifiableCallout<>("Paradeigma 3: Drop Cross", "Drop Cross");

	private final ModifiableCallout<?> paradeigma3baitLaser = new ModifiableCallout<>("Paradeigma 3: Bait Laser", "Bait Laser");
	private final ModifiableCallout<?> paradeigma3placeTowerWherever = new ModifiableCallout<>("Paradeigma 3: Place Tower (Normal)", "Tower");
	private final ModifiableCallout<?> paradeigma3placeTowerMiddle = new ModifiableCallout<>("Paradeigma 3: Place Tower (Middle)", "Tower Middle");

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
					Pd3SupportMech mech = switch ((int) buff.getBuff().getId()) {
						case 0xDFB -> Pd3SupportMech.LIGHT_TOWER;
						case 0xDFC -> Pd3SupportMech.DARK_TOWER;
						case 0xDFF -> Pd3SupportMech.PLUS;
						case 0xE00 -> Pd3SupportMech.CROSS;
						default -> throw new RuntimeException("Unknown Buff");
					};
					s.updateCall(switch (mech) {
						case LIGHT_TOWER -> paradeigma3light;
						case DARK_TOWER -> paradeigma3dark;
						case PLUS -> paradeigma3plus;
						case CROSS -> paradeigma3cross;
					}, buff);
					// Shock - tower drop
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8312));
					if (mech == Pd3SupportMech.PLUS || mech == Pd3SupportMech.CROSS) {
						ModifiableCallout<?> call = switch (mech) {
							case PLUS -> paradeigma3plusPart2;
							case CROSS -> paradeigma3crossPart2;
							default -> null;
						};
						s.updateCall(call);
						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xDFF, 0xE00));
						s.updateCall(paradeigma3baitLaser);
					}
					else {
//						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xDFF, 0xE00));
						// wait a little bit so that an early call doesn't bait someone into eating a tether
						s.waitMs(1_000);
						boolean playerEast = state.getPlayer().getPos().x() > 100;
						boolean playerLight = mech == Pd3SupportMech.LIGHT_TOWER;
						boolean lightTetherEast = tethers.stream().filter(te -> te.tetherIdMatches(233, 250))
								.map(te -> te.getTargetMatching(XivCombatant::isPc))
								.map(state::getLatestCombatantData)
								.anyMatch(player -> player.getPos().x() > 100);
						boolean placeTowerMiddle = (playerEast == playerLight) == lightTetherEast;
						log.info("Player East: {}; Player Light: {}; Light Tether East: {}, Place Tower Middle: {}", playerEast, playerLight, lightTetherEast, placeTowerMiddle);
						if (placeTowerMiddle) {
							s.updateCall(paradeigma3placeTowerMiddle);
						}
						else {
							s.updateCall(paradeigma3placeTowerWherever);
						}
					}

				}
			}
	);

	private enum Pd3SupportMech {
		LIGHT_TOWER,
		DARK_TOWER,
		PLUS,
		CROSS
	}

	@NpcAbilityUsedCallout(0x82F3)
	private final ModifiableCallout<AbilityUsedEvent> ultimaBlade = new ModifiableCallout<>("Ultima Blade", "Big Raidwide");

	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave1 = new ModifiableCallout<>("Limit Cut: You Have #1", "One", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave2 = new ModifiableCallout<>("Limit Cut: You Have #2", "Two", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave3 = new ModifiableCallout<>("Limit Cut: You Have #3", "Three", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave4 = new ModifiableCallout<>("Limit Cut: You Have #4", "Four", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave5 = new ModifiableCallout<>("Limit Cut: You Have #5", "Five", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave6 = new ModifiableCallout<>("Limit Cut: You Have #6", "Six", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave7 = new ModifiableCallout<>("Limit Cut: You Have #7", "Seven", 30_000);
	private final ModifiableCallout<HeadMarkerEvent> limitCutYouHave8 = new ModifiableCallout<>("Limit Cut: You Have #8", "Eight", 30_000);

	private final ModifiableCallout<?> limitCut1 = new ModifiableCallout<>("Limit Cut: #1", "One");
	private final ModifiableCallout<?> limitCut2 = new ModifiableCallout<>("Limit Cut: #2", "Two");
	private final ModifiableCallout<?> limitCut3 = new ModifiableCallout<>("Limit Cut: #3", "Three");
	private final ModifiableCallout<?> limitCut4 = new ModifiableCallout<>("Limit Cut: #4", "Four");
	private final ModifiableCallout<?> limitCut5 = new ModifiableCallout<>("Limit Cut: #5", "Five");
	private final ModifiableCallout<?> limitCut6 = new ModifiableCallout<>("Limit Cut: #6", "Six");
	private final ModifiableCallout<?> limitCut7 = new ModifiableCallout<>("Limit Cut: #7", "Seven");
	private final ModifiableCallout<?> limitCut8 = new ModifiableCallout<>("Limit Cut: #8", "Eight");
	private final ModifiableCallout<?> limitCutPost = new ModifiableCallout<>("Limit Cut: After", "Avoid Center Platforms");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitCut = SqtTemplates.sq(60_000,
			AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x82F3),
			(e1, s) -> {
				List<HeadMarkerEvent> headmarkers = s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hme -> true);
				List<ModifiableCallout<?>> groupCalls = List.of(limitCut1, limitCut2, limitCut3, limitCut4, limitCut5, limitCut6, limitCut7, limitCut8);

				List<ModifiableCallout<HeadMarkerEvent>> personalCalls = List.of(limitCutYouHave1, limitCutYouHave2, limitCutYouHave3, limitCutYouHave4, limitCutYouHave5, limitCutYouHave6, limitCutYouHave7, limitCutYouHave8);
				for (int i = 0; i < headmarkers.size(); i++) {
					HeadMarkerEvent thisHm = headmarkers.get(i);
					if (thisHm.getTarget().isThePlayer()) {
						s.call(personalCalls.get(i), thisHm);
						break;
					}
				}
				s.waitMs(2_000);
				// Palladion = puddle
				for (int i = 1; i <= 8; i++) {
					log.info("Limit Cut: {}", i);
					s.waitMs(300);
					s.updateCall(groupCalls.get(i - 1));
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x82F6) && aue.isFirstTarget());
				}
				s.waitMs(6_000);
				s.updateCall(limitCutPost);
			});

	@NpcCastCallout(0x82FA)
	private final ModifiableCallout<AbilityCastStart> theosUltima = ModifiableCallout.durationBasedCall("Theos's Ultima", "Big Raidwide");

	private final ModifiableCallout<?> sc2a_northProtean = new ModifiableCallout<>("Superchain 2A: Start North Protean", "North Protean, {trinitySafe[0]}")
			.extendedDescription("""
					This callout happens at the same time as the Trinity initial
					call would happen, so this call is used instead. You can use
					{trinitySafe[0]} or {trinityRight[0]} in this call and they
					will behave as they would in the standalone Trinity initial
					call.""");
	private final ModifiableCallout<?> sc2a_northBuddies = new ModifiableCallout<>("Superchain 2A: Start North Buddies", "North Buddies, {trinitySafe[0]}");
	private final ModifiableCallout<?> sc2a_southProtean = new ModifiableCallout<>("Superchain 2A: Start South Protean", "South Protean, {trinitySafe[0]}");
	private final ModifiableCallout<?> sc2a_southBuddies = new ModifiableCallout<>("Superchain 2A: Start South Buddies", "South Buddies, {trinitySafe[0]}");
	private final ModifiableCallout<?> sc2a_midDonut = new ModifiableCallout<>("Superchain 2A: Mid Donut", "Next: {(trinitySafe[1] == trinitySafe[0]) ? 'Same Side' : 'Cross'} Middle, {(firstMechAt == secondMechAt) ? 'Back' : 'Through'}")
			.extendedDescription("""
					As with the initial call, you can use Trinity variables here:
					{trinitySafe[0]} {trinitySafe[1]} {trinitySafe[2]}
					{trinityRight[0]} {trinityRight[1]} {trinityRight[2]}""");
	private final ModifiableCallout<?> sc2a_northProteanFinal = new ModifiableCallout<>("Superchain 2A: Final North Protean", "{(trinitySafe[2] == trinitySafe[1]) ? 'Same Side' : 'Cross'} then North Protean");
	private final ModifiableCallout<?> sc2a_northBuddiesFinal = new ModifiableCallout<>("Superchain 2A: Final North Buddies", "{(trinitySafe[2] == trinitySafe[1]) ? 'Same Side' : 'Cross'} then North Buddies");
	private final ModifiableCallout<?> sc2a_southProteanFinal = new ModifiableCallout<>("Superchain 2A: Final South Protean", "{(trinitySafe[2] == trinitySafe[1]) ? 'Same Side' : 'Cross'} then South Protean");
	private final ModifiableCallout<?> sc2a_southBuddiesFinal = new ModifiableCallout<>("Superchain 2A: Final South Buddies", "{(trinitySafe[2] == trinitySafe[1]) ? 'Same Side' : 'Cross'} then South Buddies");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> superchain2a = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86FA),
			(e1, s) -> {
				trinitySuppress = true;
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x86FA));
				// This one seems different than SC1.
				// North buddies, then mid donut, then south proteans
				//
				// North 3177 16176, with 3176 16180 (buddies)
				// Mid 3175 16176, with 3174 16177 (out)
				// South 3173 16176, with 3172 16177 (out)
				// Next lowest ID is 3171 which is the mid "in" (16178)
				// After that, we have 3170 and 316e, which are 16177 (out)
				// But also 316f which is 16179 (protean) which is the final mechanic
				// Also 0ce? is that related?

				s.waitMs(300);
				List<XivCombatant> fixedOrbs;
				do {
					s.waitThenRefreshCombatants(100);
					fixedOrbs = state.npcsById(16176);
				} while (fixedOrbs.size() != 3);

				fixedOrbs = fixedOrbs.stream().sorted(Comparator.<XivCombatant, Double>comparing(orb -> orb.getPos().y())).toList();
				XivCombatant northOrb = fixedOrbs.get(0);
				XivCombatant southOrb = fixedOrbs.get(2);
				OrbMechanic northMech = getOrbChainActors(northOrb, 1).stream().map(OrbMechanic::forNpc).filter(Objects::nonNull).findFirst().orElse(null);
				OrbMechanic southMech = getOrbChainActors(southOrb, 1).stream().map(OrbMechanic::forNpc).filter(Objects::nonNull).findFirst().orElse(null);

				TrinityInitialEvent trinStart = s.waitEvent(TrinityInitialEvent.class);
				s.setParam("trinitySafe", List.of(trinStart.getSafeSpot()));
				s.setParam("trinityRight", List.of(trinStart.isRightSafe()));
				if (northMech == OrbMechanic.PROTEAN) {
					s.updateCall(sc2a_northProtean);
					s.setParam("firstMechAt", ArenaSector.NORTH);
				}
				else if (northMech == OrbMechanic.BUDDIES) {
					s.updateCall(sc2a_northBuddies);
					s.setParam("firstMechAt", ArenaSector.NORTH);
				}
				else if (southMech == OrbMechanic.PROTEAN) {
					s.updateCall(sc2a_southProtean);
					s.setParam("firstMechAt", ArenaSector.SOUTH);
				}
				else if (southMech == OrbMechanic.BUDDIES) {
					s.updateCall(sc2a_southBuddies);
					s.setParam("firstMechAt", ArenaSector.SOUTH);
				}
				else {
					log.error("Unexpected mech(s)! {} {}", northMech, southMech);
				}
				TrinityFullEvent trinFull = s.waitEvent(TrinityFullEvent.class);
				s.setParam("trinitySafe", trinFull.getSafeSpots());
				s.setParam("trinityRight", trinFull.getRightSafe());
				Optional<XivCombatant> finalMechMaybe = state.getCombatantsListCopy().stream()
						.filter(cbt -> cbt.npcIdMatches(16179, 16180))
						// Range sanity check - might have stale combatants
						.filter(cbt -> cbt.getId() > (northOrb.getId() - 16))
						.min(Comparator.comparing(cbt -> cbt.getId()));
				ModifiableCallout<?> finalCall = null;
				// TODO: this is checking too quickly - orb isn't positioned yet
				// Possible solution: ignore if it hasn't turned yet (i.e. heading == 0 => retry)
				if (finalMechMaybe.isPresent()) {
					XivCombatant finalOrb = finalMechMaybe.get();
					while (Math.abs(finalOrb.getPos().heading()) < 0.01) {
						finalOrb = state.getLatestCombatantData(finalOrb);
						s.waitMs(100);
					}
					OrbMechanic mech = OrbMechanic.forNpc(finalOrb);
					Position translated = finalOrb.getPos().translateRelative(0, 100);
					boolean north = translated.y() < 100;
					s.setParam("secondMechAt", north ? ArenaSector.NORTH : ArenaSector.SOUTH);
					if (mech == OrbMechanic.BUDDIES) {
						finalCall = (north ? sc2a_northBuddiesFinal : sc2a_southBuddiesFinal);
					}
					else if (mech == OrbMechanic.PROTEAN) {
						finalCall = (north ? sc2a_northProteanFinal : sc2a_southProteanFinal);
					}
					else {
						log.error("Unexpected mech! {}", mech);
					}
				}
				s.updateCall(sc2a_midDonut);
				s.waitEvent(AbilityCastStart.class, event -> event.abilityIdMatches(0x82DC));
				s.updateCall(finalCall);
			});

	private final ModifiableCallout<?> sc2b0 = new ModifiableCallout<>("Superchain 2B: Starting Spot", "Start {firstSafe}");
	private final ModifiableCallout<?> sc2b1 = new ModifiableCallout<>("Superchain 2B: Initial", "{firstSafe}, then {firstSafe.eighthsTo(secondSafe) < 0 ? 'Right' : 'Left'} Out, {secondMech}");
	private final ModifiableCallout<?> sc2b2 = new ModifiableCallout<>("Superchain 2B: Final", "{goIn ? 'In' : 'Out'} then {secondSafe.eighthsTo(finalSafe) < 0 ? 'Right' : 'Left'} {finalMech}");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> superchain2b = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86FB),
			(e1, s) -> {
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x86FB));
				// Parthenos (0x803 cleaving N/front) is initial N/S cleave
				Map<ArenaSector, XivCombatant> fixedOrbs = new EnumMap<>(ArenaSector.class);
				Map<ArenaSector, OrbMechanic> initialMechs = new EnumMap<>(ArenaSector.class);
				List<XivCombatant> orbs;
				do {
					s.waitThenRefreshCombatants(100);
					orbs = state.npcsById(OrbMechanic.FIXED_ORB.npcId);
					log.info("Orbs: {}", orbs.size());
					for (XivCombatant orb : orbs) {
						ArenaSector sector = sc2bAp.forCombatant(orb);
						log.info("Orb: {} at {} {}", orb.getbNpcId(), sector, orb.getPos());
						// Don't overwrite the initial N/S with the further out ones
						List<XivCombatant> actors = getOrbChainActors(orb, 1);
						if (!actors.isEmpty()) {
							fixedOrbs.putIfAbsent(sector, orb);
							initialMechs.put(sector, OrbMechanic.forNpc(actors.get(0)));
						}
					}
				} while (!(fixedOrbs.containsKey(ArenaSector.NORTH) && fixedOrbs.containsKey(ArenaSector.SOUTH)));
				// Check for IN side
				ArenaSector firstSafe = initialMechs.get(ArenaSector.NORTH) == OrbMechanic.IN ? ArenaSector.NORTH : ArenaSector.SOUTH;
				s.setParam("firstSafe", firstSafe);
				s.updateCall(sc2b0);
				do {
					s.waitThenRefreshCombatants(100);
					orbs = state.npcsById(OrbMechanic.FIXED_ORB.npcId);
					log.info("Orbs: {}", orbs.size());
					for (XivCombatant orb : orbs) {
						ArenaSector sector = sc2bAp.forCombatant(orb);
						log.info("Orb: {} at {} {}", orb.getbNpcId(), sector, orb.getPos());
						// Don't overwrite the initial N/S with the further out ones
						List<XivCombatant> actors = getOrbChainActors(orb, 1);
						if (!actors.isEmpty()) {
							fixedOrbs.putIfAbsent(sector, orb);
							initialMechs.put(sector, OrbMechanic.forNpc(actors.get(0)));
						}
					}
				} while (!(fixedOrbs.containsKey(ArenaSector.NORTH) && fixedOrbs.containsKey(ArenaSector.SOUTH) && fixedOrbs.containsKey(ArenaSector.WEST) && fixedOrbs.containsKey(ArenaSector.EAST)));
				// Check for OUT side, go opposite
				ArenaSector secondSafe = initialMechs.get(ArenaSector.WEST) == OrbMechanic.OUT ? ArenaSector.EAST : ArenaSector.WEST;
				s.setParam("secondSafe", secondSafe);
				OrbMechanic secondMech = initialMechs.get(secondSafe);
				s.setParam("secondMech", secondMech);
				s.updateCall(sc2b1);
				boolean leftCleaveShift = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82EE))
						.stream()
						.anyMatch(cast -> cast.getSource().getPos().x() < 96);
				s.setParam("leftCleaveShift", leftCleaveShift);
				boolean goIn = (secondSafe == ArenaSector.WEST) == leftCleaveShift;
				s.setParam("goIn", goIn);
				// Consider only the two highest IDs
				Map.Entry<ArenaSector, OrbMechanic> finalStuff = state.npcsById(16176)
						.stream()
						.sorted(Comparator.comparing(npc -> -npc.getId()))
						.limit(2)
						.flatMap(orb -> {
							OrbMechanic mech = getOrbChainActors(orb, 2).stream()
									.map(OrbMechanic::forNpc)
									.filter(mch -> mch == OrbMechanic.PROTEAN || mch == OrbMechanic.BUDDIES)
									.findFirst()
									.orElse(null);
							if (mech == null) {
								return Stream.empty();
							}
							else {
								ArenaSector where = ap.forCombatant(orb);
								return Stream.of(Map.entry(where, mech));
							}
						})
						.findFirst()
						.orElseThrow(() -> new RuntimeException("No final orb!"));

				ArenaSector finalSafe = finalStuff.getKey();
				OrbMechanic finalMech = finalStuff.getValue();
				s.setParam("finalSafe", finalSafe);
				s.setParam("finalMech", finalMech);
				s.updateCall(sc2b2);


			});

}
