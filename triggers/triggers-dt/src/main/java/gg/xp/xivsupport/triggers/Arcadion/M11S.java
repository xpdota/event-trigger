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
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@CalloutRepo(name = "M11S", duty = KnownDuty.M11S)
public class M11S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M11S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M11S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M11S);
	}

	@NpcCastCallout(0xB406)
	private final ModifiableCallout<AbilityCastStart> crownOfArcadia = ModifiableCallout.durationBasedCall("Crown of Arcadia", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> axeTank = ModifiableCallout.durationBasedCallWithExtraCastTime("Axe (As Tank)", "Tank Stack");
	private final ModifiableCallout<AbilityCastStart> axeNonTank = ModifiableCallout.durationBasedCallWithExtraCastTime("Axe (Non Tank)", "Party Spread");

	private final ModifiableCallout<AbilityCastStart> scytheTank = ModifiableCallout.durationBasedCallWithExtraCastTime("Axe (As Tank)", "Tank Spread");
	private final ModifiableCallout<AbilityCastStart> scytheNonTank = ModifiableCallout.durationBasedCallWithExtraCastTime("Axe (Non Tank)", "Party Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> axeScytheSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB422, 0xB423),
			(e1, s) -> {
				boolean isTank = state.playerJobMatches(Job::isTank);
				if (e1.abilityIdMatches(0xB422)) {
					s.updateCall(isTank ? axeTank : axeNonTank, e1);
				}
				else {
					s.updateCall(isTank ? scytheTank : scytheNonTank, e1);
				}
			}
	);

	@NpcCastCallout(0xB42A)
	private final ModifiableCallout<AbilityCastStart> oneAndOnly = ModifiableCallout.durationBasedCall("One and Only", "Raidwide");

	@NpcCastCallout(value = 0xB414, onYou = true)
	private final ModifiableCallout<AbilityCastStart> comet = ModifiableCallout.durationBasedCall("Comet", "Spread");

	private static final ArenaPos ap = new ArenaPos(100, 100, 6, 6);

	private enum Weapon {
		Axe(19184),
		Scythe(19185),
		Sword(19186);

		private final int npcId;

		Weapon(int npcId) {
			this.npcId = npcId;
		}

		public static Weapon forNpc(XivCombatant target) {
			return Arrays.stream(values())
					.filter(wep -> target.getbNpcId() == wep.npcId)
					.findAny()
					.orElse(null);
		}
	}

	private Map<Weapon, XivCombatant> getWeaponsOuter() {
		var out = new EnumMap<Weapon, XivCombatant>(Weapon.class);
		for (Weapon wep : Weapon.values()) {
			state.npcsById(wep.npcId).stream().filter(w -> w.getPos().distanceFrom2D(Position.of2d(100, 100)) > 1)
					.findAny().ifPresent(npc -> out.put(wep, npc));
		}
		return out;
	}

	private Map<Weapon, ArenaSector> getWeaponsOuterSectors() {
		var out = new EnumMap<Weapon, ArenaSector>(Weapon.class);
		getWeaponsOuter().forEach((w, npc) -> out.put(w, ap.forCombatant(npc)));
		return out;
	}

	private Map<ArenaSector, Weapon> getSectorWeaponsOuter() {
		var out = new EnumMap<ArenaSector, Weapon>(ArenaSector.class);
		getWeaponsOuter().forEach((w, npc) -> out.put(ap.forCombatant(npc), w));
		return out;
	}

	private final ModifiableCallout<AbilityCastStart> assaultEvolvedFirst = ModifiableCallout.<AbilityCastStart>durationBasedCallWithOffset(
			"Assault Evolved: First",
					"Start { bossFacing } {{ mechanics.take(2).collect{['Stack Out', 'Proteans In', 'Light Parties Cross'][it]} }}",
					Duration.ofMillis(1_500))
			.extendedDescription("""
			To change the number of mechanics read out at once, change `take(2)` to 1 or 3.""");
	private final ModifiableCallout<?> assaultEvolvedSecond = new ModifiableCallout<>("Assault Evolved: Second", "{{ mechanics.take(2).collect{['Stack Out', 'Proteans In', 'Light Parties Cross'][it]} }}");
	private final ModifiableCallout<?> assaultEvolvedThird = new ModifiableCallout<>("Assault Evolved: Third", "{{ mechanics.collect{['Stack Out', 'Proteans In', 'Light Parties Cross'][it]} }}");
	private final ModifiableCallout<?> assaultEvolvedPuddleAfter = new ModifiableCallout<>("Assault Evolved: Puddle", "Drop Puddle");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> assaultEvolved = SqtTemplates.selfManagedMultiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB417),
			(e1, s, i) -> {
				var e1l = s.findOrWaitForCastWithLocation(casts, acs -> acs == e1, false);
				ArenaSector bossFacing = ArenaPos.combatantFacing(e1l.getLocationInfo().getBestHeading());
				s.setParam("bossFacing", bossFacing);
				Map<ArenaSector, Weapon> sectorWeaponsOuter = getSectorWeaponsOuter();
				Set<Weapon> weapons = EnumSet.allOf(Weapon.class);
				Weapon firstWeapon = sectorWeaponsOuter.get(bossFacing);
				if (firstWeapon == null) {
					log.error("no weapon in {}. mapping: {}", bossFacing, sectorWeaponsOuter);
					return;
				}
				Weapon secondWeapon = sectorWeaponsOuter.getOrDefault(bossFacing.plusEighths(2), sectorWeaponsOuter.get(bossFacing.plusEighths(3)));
				weapons.remove(firstWeapon);
				weapons.remove(secondWeapon);
				Weapon thirdWeapon = weapons.iterator().next();
				// e.g. scythe sword axe would be 1,2,0
				List<Integer> mechanics = new ArrayList<>(List.of(firstWeapon.ordinal(), secondWeapon.ordinal(), thirdWeapon.ordinal()));
				s.setParam("mechanics", mechanics);
				s.updateCall(assaultEvolvedFirst, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB41B, 0xB41C, 0xB41D) && aue.isFirstTarget());
				mechanics.remove(0);
				s.updateCall(assaultEvolvedSecond);
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB41B, 0xB41C, 0xB41D) && aue.isFirstTarget());
				mechanics.remove(0);
				s.updateCall(assaultEvolvedThird);
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB41B, 0xB41C, 0xB41D) && aue.isFirstTarget());
				if (i == 1) {
					s.updateCall(assaultEvolvedPuddleAfter);
				}
			}
	);

	@NpcCastCallout(0xB415)
	private final ModifiableCallout<AbilityCastStart> crushingComet = ModifiableCallout.durationBasedCall("Crushing Comet", "Stack");


	@NpcCastCallout(0xB412)
	private final ModifiableCallout<AbilityCastStart> voidStardust = ModifiableCallout.durationBasedCall("Void Stardust", "Puddles");

	private final ModifiableCallout<AbilityCastStart> danceOfDominationTrophy = ModifiableCallout.durationBasedCallWithExtraCastTime("Dance of Domination Trophy", "Multiple Raidwides");
	private final ModifiableCallout<AbilityCastStart> danceOfDominationTrophySafe = ModifiableCallout.durationBasedCallWithExtraCastTime("Dance of Domination Trophy: Safe Spot", "Partners {safe}")
			.extendedDescription("""
					This calls the 'middle' of the safe area. e.g. if safe is W/N/E, it will call N.
					To have it call one of the sides, use {safe.plusEighths(1)}, to shift the call to NE, or any other number.""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> danceOfDomSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB7BB),
			(e1, s) -> {
				s.updateCall(danceOfDominationTrophy, e1);
				var explosions = s.waitEventsQuickSuccession(6, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB420));
				// 12 lines, but some of them are redundant
				Set<ArenaSector> safe = EnumSet.of(ArenaSector.NORTH, ArenaSector.SOUTH, ArenaSector.WEST, ArenaSector.EAST);
				for (AbilityCastStart explosion : explosions) {
					// The position is the edge of the explosion - it is 60y long, so we need to translate it "forwards" 30y to get the middle
					Position pos = s.waitForCastLocation(explosion).getPos().translateRelative(0, 30);
					log.info("Explosion pos: {}", pos);
					ArenaSector centeredOn = ap.forPosition(pos);
					if (centeredOn.isCardinal()) {
						safe.remove(centeredOn);
					}
					// We don't care about any others - only the ones that block an entire side are relevant
				}
				if (safe.size() != 1) {
					log.error("Bad safeSpots spots! {}", safe);
					log.error("Explosions: {}", explosions);
					s.setParam("safe", ArenaSector.UNKNOWN);
					return;
				}
				s.setParam("safe", safe.iterator().next());
				s.updateCall(danceOfDominationTrophySafe, explosions.get(0));
			}
	);

	private final List<XivCombatant> pendingUltimateWeapons = new ArrayList<>();
	private final List<Integer> pendingUltimateMechs = new ArrayList<>();

	@NpcCastCallout(0xB425)
	private final ModifiableCallout<AbilityCastStart> charybdistopia = ModifiableCallout.durationBasedCall("Charybdistopia", "1 HP");


	private final ModifiableCallout<?> ultimateTrophyEarly = new ModifiableCallout<>("Ultimate Trophy First", "{ firstMechAt } {{ ['Stack Out', 'Proteans In', 'Light Parties Cross'][firstMech] }}").extendedDescription("""
			Please note that this callout cannot call the direction as the direction is not known until the second mechanic.""");
	private final ModifiableCallout<?> ultimateTrophyInitial = new ModifiableCallout<>("Ultimate Trophy Initial", "Then { clockwise ? 'Clockwise' : 'CCW' } {{ ['Stack Out', 'Proteans In', 'Light Parties Cross'][mechanics[1]] }}");
	private final ModifiableCallout<?> ultimateTrophyFollowup = new ModifiableCallout<>("Ultimate Trophy Followup", "{{ mechanics.take(2).collect{['Stack Out', 'Proteans In', 'Light Parties Cross'][it]} }}")
			.extendedDescription("""
					To change the number of mechanics read out at once, change `take(2)` to a different number. You can do the same for the followup call as well.
					You can also use the parameter 'startAt' to indicate starting location.""");
	private final ModifiableCallout<?> ultimateTrophyTornado = new ModifiableCallout<>("Ultimate Trophy Bait Tornado", "Bait Tornado");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ultimateTrophyCollectorSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB7ED),
			(e1, s) -> {
				pendingUltimateWeapons.clear();
				pendingUltimateMechs.clear();
//				Set<Long> seenIds = new HashSet<>();
				int count = 0;
				// Collector logic
				while (count < 6) {
					var acee = s.waitEvent(ActorControlExtraEvent.class,
							ace -> Weapon.forNpc(ace.getTarget()) != null && ace.getData0() >= 0x11D1 && ace.getData0() <= 0x11D3);
					XivCombatant tgt = acee.getTarget();
					s.waitThenRefreshCombatants(50);
					XivCombatant cbt = state.getLatestCombatantData(tgt);
					pendingUltimateWeapons.add(cbt);
					Weapon weapon = Weapon.forNpc(tgt);
					log.info("Weapon: {} : {} at {}", Long.toString(tgt.getId(), 16), weapon, cbt.getPos());
					int mech = weapon.ordinal();
					pendingUltimateMechs.add(mech);
					if (count == 0) {
						s.setParam("firstMech", mech);
						var firstSector = ap.forCombatant(tgt);
						s.setParam("firstMechAt", firstSector);
						s.updateCall(ultimateTrophyEarly);
					}
					count++;
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ultimateTrophyCallSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB7ED),
			(e1, s) -> {
				s.waitEvent(ActorControlExtraEvent.class, ace -> Weapon.forNpc(ace.getTarget()) != null);
				s.waitEvent(ActorControlExtraEvent.class, ace -> Weapon.forNpc(ace.getTarget()) != null);
				// Wait until we know we would have two
				while (pendingUltimateWeapons.size() < 2) {
					s.waitMs(50);
				}
				var first = pendingUltimateWeapons.get(0);
				var second = pendingUltimateWeapons.get(1);
				var firstSector = ap.forCombatant(first);
				var secondSector = ap.forCombatant(second);
				boolean clockwise = firstSector.eighthsTo(secondSector) > 0;
				s.setParam("startAt", firstSector);
				s.setParam("clockwise", clockwise);
				s.setParam("mechanics", new ArrayList<>(pendingUltimateMechs));
				s.setParam("weapons", new ArrayList<>(pendingUltimateWeapons));
				s.updateCall(ultimateTrophyInitial);

				for (int i = 1; i <= 6; i++) {
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB41B, 0xB41C, 0xB41D) && aue.isFirstTarget());
					pendingUltimateWeapons.remove(0);
					pendingUltimateMechs.remove(0);
					s.setParam("mechanics", new ArrayList<>(pendingUltimateMechs));
					s.setParam("weapons", new ArrayList<>(pendingUltimateWeapons));
					// Don't call empty
					if (i < 6) {
						s.updateCall(ultimateTrophyFollowup);
					}
					s.waitMs(100);
				}
				s.updateCall(ultimateTrophyTornado);
			});

	@NpcCastCallout(0xB42B)
	private final ModifiableCallout<AbilityCastStart> greatWallOfFire = ModifiableCallout.durationBasedCall("Great Wall of Fire", "Double Tank Buster");

	@NpcCastCallout(0xB430)
	private final ModifiableCallout<AbilityCastStart> fireAndFury = ModifiableCallout.durationBasedCall("Fire and Fury", "Sides Safe");

	private final ModifiableCallout<AbilityCastStart> orbitalOmenInitial = ModifiableCallout.durationBasedCall("Orbital Omen: Initial", "Watch Lasers");

	private final ModifiableCallout<HeadMarkerEvent> orbitalOmenMarkerOnYou = new ModifiableCallout<>("Orbital Omen: Marker on You", "Marker with {buddy}");
	private final ModifiableCallout<AbilityCastStart> orbitalOmenNoMarkerFirst = ModifiableCallout.durationBasedCall("Orbital Omen: No Marker (First Set)", "First Set");
	private final ModifiableCallout<AbilityCastStart> orbitalOmenNoMarkerSecond = ModifiableCallout.durationBasedCall("Orbital Omen: No Marker (First Set)", "Second Set");
	private final ModifiableCallout<AbilityCastStart> orbitalOmenNoMarkerThird = ModifiableCallout.durationBasedCall("Orbital Omen: No Marker (First Set)", "Third Set");
	private final ModifiableCallout<AbilityCastStart> orbitalOmenNoMarkerFourth = ModifiableCallout.durationBasedCall("Orbital Omen: Final Set", "Fourth Set");

	private void doOrbitalMarkers(SequentialTriggerController<BaseEvent> s, ModifiableCallout<AbilityCastStart> noMarkerCall) {
		var markers = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.markerIdMatches(244));
		markers.stream().filter(fm -> fm.getTarget().isThePlayer())
				.findAny()
				.ifPresentOrElse(myMarker -> {
					markers.stream()
							.filter(marker -> !marker.getTarget().isThePlayer()).findAny()
							.ifPresent(otherMarker -> s.setParam("buddy", otherMarker.getTarget()));
					s.updateCall(orbitalOmenMarkerOnYou, myMarker);
				}, () -> {
					var cast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB439), false);
					s.updateCall(noMarkerCall, cast);
				});
		s.waitMs(100);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> orbitalOmenSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB432),
			(e1, s) -> {
				s.updateCall(orbitalOmenInitial, e1);
				// Side lasers are staggered casts of B433
				// Boss cleave B430
				// Already handled by other calls

				// weird - the stack marker seems to be on an NPC?
				doOrbitalMarkers(s, orbitalOmenNoMarkerFirst);
				doOrbitalMarkers(s, orbitalOmenNoMarkerSecond);
				doOrbitalMarkers(s, orbitalOmenNoMarkerThird);
				// No fourth set - just the stack and tethers
				s.waitMs(7_000);
				// Call out stack here
				var cast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB439), false);
				s.updateCall(orbitalOmenNoMarkerFourth, cast);
				// "Hide Behind" call already handled
			});

	@NpcCastCallout(0xB43C)
	private final ModifiableCallout<AbilityCastStart> tripleAnni = ModifiableCallout.durationBasedCall("Triple Annihilation", "Hide Behind Meteors");

	private final ModifiableCallout<AbilityCastStart> flatliner = ModifiableCallout.durationBasedCall("Flatliner", "Raidwide, Knockback");
	private final ModifiableCallout<?> flatlinerNoTether = new ModifiableCallout<>("Flatliner: No Tether", "No Tether");
	private final ModifiableCallout<TetherEvent> flatlinerTetherCross = new ModifiableCallout<>("Flatliner: Tether, Cross", "{tetherFrom} {outer ? 'Outer' : 'Inner'} Tether, Cross");
	private final ModifiableCallout<TetherEvent> flatlinerTetherNoCross = new ModifiableCallout<>("Flatliner: Tether, Stay", "{tetherFrom} {outer ? 'Outer' : 'Inner'} Tether, Stay");
	private final ModifiableCallout<?> flatlinerBaitPuddlesTether = new ModifiableCallout<>("Flatliner: Bait Puddles w/ Tether", "Bait Puddles Far");
	private final ModifiableCallout<?> flatlinerBaitPuddlesNoTether = new ModifiableCallout<>("Flatliner: Bait Puddles No Tether", "Bait Puddles Close");


	private final ArenaPos flatlinerAp = new ArenaPos(100, 100, 0, 0);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> flatlinerSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xBA90),
			(e1, s) -> {
				s.updateCall(flatliner, e1);
				for (int i = 0; i < 2; i++) {

					var tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isPc));
					Optional<TetherEvent> playerTether = tethers.stream().filter(t -> t.eitherTargetMatches(XivCombatant::isThePlayer)).findAny();
					playerTether.ifPresentOrElse(
							myTether -> {
								// have tether
								s.waitThenRefreshCombatants(50);
								XivCombatant npcTarget = state.getLatestCombatantData(myTether.getTargetMatching(cbt -> !cbt.isPc()));
								// Where the tether comes from
								ArenaSector tetherFrom = flatlinerAp.forCombatant(npcTarget);
								// Is it an inner or outer tether? Inners are 95/105, outer is 85/115
								boolean outer = Math.abs(npcTarget.getPos().y() - 100) > 10;
								// Where the player is
								ArenaSector playerAt = flatlinerAp.forCombatant(state.getPlayer());
								// Where the player needs to get knocked to
								ArenaSector knockTo = tetherFrom.opposite();
								// Is the player East?
								boolean playerEast = playerAt.isAdjacentOrEqualTo(ArenaSector.EAST);
								// Is the tether from east?
								boolean tetherFromEast = tetherFrom.isAdjacentOrEqualTo(ArenaSector.EAST);
								// Does the player need to get knocked to the other side?
								boolean cross = playerEast == tetherFromEast;
								s.setParam("tetherFrom", tetherFrom);
								s.setParam("knockTo", knockTo);
								s.setParam("outer", outer);
								s.updateCall(cross ? flatlinerTetherCross : flatlinerTetherNoCross, myTether);
							}, () -> {
								// no tether
								s.updateCall(flatlinerNoTether);
							}
					);
					// Tower goes off
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB444, 0xB445));
					s.waitMs(2_800);
					// Wait for landing
					playerTether.ifPresentOrElse(t -> s.updateCall(flatlinerBaitPuddlesTether), () -> s.updateCall(flatlinerBaitPuddlesNoTether));
					// TODO: close/far laser
					// I suspect it is MapEffect
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB443));
					s.waitMs(2_000);
				}
			});


	@NpcCastCallout(0xB448)
	private final ModifiableCallout<AbilityCastStart> massiveMeteor = ModifiableCallout.durationBasedCall("Massive Meteor", "Stack, Multiple Hits");

	private final ModifiableCallout<AbilityCastStart> arcadionAvalancheCross = ModifiableCallout.durationBasedCall("Arcadion Avalanche: Cross", "Cross to {safe}, then {cleaveSafe}");
	private final ModifiableCallout<AbilityCastStart> arcadionAvalancheStay = ModifiableCallout.durationBasedCall("Arcadion Avalanche: Stay", "Stay on {safe}, then {cleaveSafe}");
	private final ModifiableCallout<AbilityCastStart> arcadionAvalancheCleaveSafe = ModifiableCallout.durationBasedCallWithExtraCastTime("Arcadion Avalanche: Cleave Safespot", "{cleaveSafe} Safe");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> arcadionAvalanche = SqtTemplates.sq(120_000,
			// B44C is SE safe
			// B44E is SW safe
			// B450 is NW safe
			// B44A is NE safe (unconfirmed)
			// Could simplify logic by using IDs directly
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB44A, 0xB44C, 0xB44E, 0xB450),
			(e1, s) -> {
				// This is the boss facing
				DescribesCastLocation<AbilityCastStart> loc = s.waitForCastLocation(e1);
				ArenaSector bossFacing = ArenaPos.combatantFacing(loc.getBestHeading());
				ArenaSector safe = bossFacing.opposite();
				s.setParam("safe", safe);
				ArenaSector playerSector = state.getPlayer().getPos().x() > 100 ? ArenaSector.EAST : ArenaSector.WEST;
				boolean cross = bossFacing == playerSector;
				// This is the angled cleave afterwards. We know the angle immediately.
				var cleave = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xB44B, 0xB44D, 0xB44F, 0xB451), false);
				ArenaSector cleaving = flatlinerAp.forPosition(cleave.getLocationInfo().getPos().translateRelative(0, 20));
				if (cleaving.isStrictlyAdjacentTo(ArenaSector.SOUTH)) {
					s.setParam("cleaveSafe", ArenaSector.NORTH);
				}
				else {
					s.setParam("cleaveSafe", ArenaSector.SOUTH);
				}
				s.updateCall(cross ? arcadionAvalancheCross : arcadionAvalancheStay, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB444, 0xB445));
				s.waitMs(2_000);
				s.updateCall(arcadionAvalancheCleaveSafe, e1);
			});


	private final ModifiableCallout<AbilityCastStart> eclipticStampedeInitial = ModifiableCallout.durationBasedCall("Ecliptic Stampede", "Ecliptic Stampede");

	private final ModifiableCallout<HeadMarkerEvent> eclipticMarker = new ModifiableCallout<HeadMarkerEvent>("Ecliptic Stampede: Marker", "Marker with {buddy}");
	private final ModifiableCallout<?> eclipticNoMarker = new ModifiableCallout<>("Ecliptic Stampede: No Marker", "No Marker");

	private final ModifiableCallout<?> eclipticDropPuddlesOutside = new ModifiableCallout<>("Ecliptic Stampede: Drop Puddles (Had Marker)", "Drop Puddles");
	private final ModifiableCallout<?> eclipticDropPuddlesThenTower = new ModifiableCallout<>("Ecliptic Stampede: Drop Puddles (No Marker)", "Drop Puddles then Tower");
	private final ModifiableCallout<AbilityCastStart> eclipticPuddlesTower = ModifiableCallout.durationBasedCall("Ecliptic Stampede: Tower", "Tower");

	private final ModifiableCallout<TetherEvent> eclipticTether = new ModifiableCallout<TetherEvent>("Ecliptic Stampede: Tether", "{tetherFrom} Tether").extendedDescription("""
			`tetherFrom` indicates the arena sector where the tether is from.
			If you want to automatically have this converted to a safe spot (e.g. E -> NW), you can do `tetherFrom.plusEighths(-3)`.
			""");
	private final ModifiableCallout<TetherEvent> eclipticNoTether = new ModifiableCallout<TetherEvent>("Ecliptic Stampede: No Tether", "No Tether");

	private final ModifiableCallout<AbilityCastStart> eclipticTwoWayWithTether = ModifiableCallout.durationBasedCall("Ecliptic Stampede: Two-Way with Fireball Tether", "Light Parties, Behind");
	private final ModifiableCallout<AbilityCastStart> eclipticTwoWayNoTether = ModifiableCallout.durationBasedCall("Ecliptic Stampede: Two-Way with No Tether", "Light Parties, Bait");

	private final ModifiableCallout<AbilityCastStart> eclipticFourWayWithTether = ModifiableCallout.durationBasedCall("Ecliptic Stampede: Two-Way with Fireball Tether", "Buddies, Behind");
	private final ModifiableCallout<AbilityCastStart> eclipticFourWayNoTether = ModifiableCallout.durationBasedCall("Ecliptic Stampede: Two-Way with No Tether", "Buddies, Bait");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> eclipticStampedeSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB452),
			(e1, s) -> {
		s.updateCall(eclipticStampedeInitial, e1);
		/*
		Mammoth Meteor 5.7s B453
		Weighty Impact 2x 4.7s B457 and Cosmic Kiss (4.7s) B456
		 */
				var headmarkers = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.markerIdMatches(30));
				Optional<HeadMarkerEvent> maybeMarker = headmarkers.stream().filter(hme -> hme.getTarget().isThePlayer()).findAny();
				maybeMarker.ifPresentOrElse(
						myMarker -> {
							headmarkers.stream().filter(hme -> !hme.getTarget().isThePlayer())
									.findAny()
									.ifPresent(otherMarker -> s.setParam("buddy", otherMarker.getTarget()));
							s.updateCall(eclipticMarker);
						}, () -> {
							s.updateCall(eclipticNoMarker);
						}
				);

				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xB453));
				if (maybeMarker.isPresent()) {
					s.updateCall(eclipticDropPuddlesOutside);
				}
				else {
					s.updateCall(eclipticDropPuddlesThenTower);
					var tower = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB456));
					// TODO: call locations of tank vs non-tank towers?
					s.updateCall(eclipticPuddlesTower, tower);
				}

				var tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(57));
				Optional<TetherEvent> maybeMyTether = tethers.stream().filter(t -> t.eitherTargetMatches(XivCombatant::isThePlayer)).findAny();
				maybeMyTether.ifPresentOrElse(
						myTether -> {
							XivCombatant tetheredTo = myTether.getTargetMatching(cbt -> !cbt.isPc());
							ArenaSector tetherFrom = ap.forCombatant(tetheredTo);
							s.setParam("tetherFrom", tetherFrom);
							s.updateCall(eclipticTether, myTether);
						}, () -> {
							s.updateCall(eclipticNoTether);
						}
				);
				boolean hadTether = maybeMyTether.isPresent();
				// B7BD two-way fireball, B45A four-wall fireball
				var fireball = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB7BD, 0xB45A));
				if (fireball.abilityIdMatches(0xB45A)) {
					s.updateCall(hadTether ? eclipticFourWayWithTether : eclipticFourWayNoTether, fireball);
				}
				else {
					s.updateCall(hadTether ? eclipticTwoWayWithTether : eclipticTwoWayNoTether, fireball);
				}

			});


	@NpcCastCallout({0xB3FF, 0xB45D})
	private final ModifiableCallout<AbilityCastStart> heartbreakKick = ModifiableCallout.durationBasedCall("Heartbreak Kick", "Tower, Multiple Hits");
	@NpcCastCallout(value = 0xB462, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> heartbreaker = ModifiableCallout.durationBasedCall("Heartbreaker", "Enrage");
}
