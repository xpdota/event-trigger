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
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@CalloutRepo(name = "M10S", duty = KnownDuty.M10S)
public class M10S extends AutoChildEventHandler implements FilteredEventHandler {
	/*
	Current reported issues:

	"insane air 2 does not work if it's protean or tankbuster"
	"extreme snaking on the blue side it will keep saying LP even tho it's spread on the waves"

	TODO: Double Alley-oop orbs
	Also this https://discord.com/channels/551474815727304704/594899820976668673/1458743122271146201
	 */

	private static final Logger log = LoggerFactory.getLogger(M10S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M10S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M10S);
	}

	@NpcCastCallout({0xB5B6, 0xB580}) // TODO what is the difference
	private final ModifiableCallout<AbilityCastStart> hotImpact = ModifiableCallout.durationBasedCall("Hot Impact", "Buster on {event.target}");

	private final ModifiableCallout<BuffApplied> flameFloater1 = new ModifiableCallout<BuffApplied>("Flame Floater", "First", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater2 = new ModifiableCallout<BuffApplied>("Flame Floater", "Second", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater3 = new ModifiableCallout<BuffApplied>("Flame Floater", "Third", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater4 = new ModifiableCallout<BuffApplied>("Flame Floater", "Fourth", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloaterNothing = new ModifiableCallout<>("Flame Floater", "Nothing", 30_000);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> flameFloaterSq = SqtTemplates.sq(
			60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5BA),
			(e1, s) -> {
				// Since the first buff happens prior to the cast, we can look for a buff on player, or the fourth buff in general
				// since we need an exit condition
				var aBuff = s.findOrWaitForBuff(buffs,
						ba -> (ba.getTarget().isThePlayer() && ba.buffIdMatches(0xBBC, 0xBBD, 0xBBE)) || ba.buffIdMatches(0xD7B));
				RawModifiedCallout<?> call;
				if (aBuff.getTarget().isThePlayer()) {
					call = s.updateCall(switch ((int) aBuff.getBuff().getId()) {
						case 0xBBC -> flameFloater1;
						case 0xBBD -> flameFloater2;
						case 0xBBE -> flameFloater3;
						case 0xD7B -> flameFloater4;
						default -> throw new IllegalStateException("Unexpected value: " + aBuff.getBuff().getId());
					}, aBuff);
				}
				else {
					call = s.updateCall(flameFloaterNothing, aBuff);
				}
				s.waitBuffRemoved(buffs, aBuff);
				call.forceExpire();
			});

	private final ModifiableCallout<AbilityCastStart> alleyOopInfo = ModifiableCallout.durationBasedCall("Alley-oop Inferno", "Spread");
	private final ModifiableCallout<?> alleyOopInfMove = new ModifiableCallout<>("Alley-oop Inferno, Move", "Move");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> alleyOopInfernoSq = SqtTemplates.beginningAndEndingOfCast(
			acs -> acs.abilityIdMatches(0xB5C1) && acs.getTarget().isThePlayer(),
			alleyOopInfo,
			alleyOopInfMove
	);

	@NpcCastCallout(0xB5C9)
	private final ModifiableCallout<AbilityCastStart> cutbackBlaze = ModifiableCallout.durationBasedCall("Cutback Blaze", "Raidwide then Find Safe Spot");

	private final ModifiableCallout<AbilityCastStart> pyrotation1 = ModifiableCallout.durationBasedCall("Pyrotation", "Stacks and Puddles");
	//	private final ModifiableCallout<AbilityCastStart> pyrotation1move = ModifiableCallout.<AbilityCastStart>durationBasedCall("Pyrotation: Move", "Move").disabledByDefault();
	private final ModifiableCallout<AbilityCastStart> pyrotation2 = ModifiableCallout.durationBasedCall("Pyrotation 2", "Stacks, Avoid {tankTarget}, Puddles");
	private final ModifiableCallout<AbilityCastStart> pyrotation2tb = ModifiableCallout.durationBasedCall("Pyrotation 2: Tank", "Buster on You");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> pyrotationSq = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5C2),
			(e1, s) -> {
				s.updateCall(pyrotation1, e1);
			}, (e1, s) -> {
				AbilityCastStart tb = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB5B7), false);
				XivCombatant tankTarget = tb.getTarget();
				s.setParam("tankTarget", tankTarget);
				if (tankTarget.isThePlayer()) {
					s.updateCall(pyrotation2tb, tb);
				}
				else {
					s.updateCall(pyrotation2, tb);
				}
			});
	// TODO: move call?

	/*
	-8.2 Sick Swell B5CB
	+0 Sickest Take-off 3.7s B5CD - indicates later mechanic? spread? spread also has vfxloop 0x3ee
	+6 Sickest Take-off 6.7s B5CE
	+6 Sick Swell B5CC - raidwide?
	+15 Awesome Splash (spread)?
	+15 Awesome Slab (light parties) 0x3ED vfx

	The mob is visible from outside the arena
	 */

	// TODO: these can talk over "cleave from X" call
	private final ModifiableCallout<?> sickestTakeOffSpreadLater = new ModifiableCallout<>("Sickest Take-off Spread Later", "Spread Later");
	private final ModifiableCallout<?> sickestTakeOffLightPartyLater = new ModifiableCallout<>("Sickest Take-off LP Later", "Light Party Later");
	private final ModifiableCallout<AbilityCastStart> sickestTakeOffSpread = ModifiableCallout.durationBasedCall("Sickest Take-off Spread Now", "Raidwide then Spread");
	private final ModifiableCallout<AbilityCastStart> sickestTakeOffLightParty = ModifiableCallout.durationBasedCall("Sickest Take-off LP Now", "Raidwide then Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sickestTakeOff = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5CB),
			(e1, s) -> {
				/*
				Search for `event instanceof StatusLoopVfxApplied || (event instanceof CalloutEvent && event.ttsText.contains("Later")) || event instanceof MapEffectEvent || (event instanceof AbilityCastStart && event.ability.id == 0xB5CB) || (event instanceof AbilityUsedEvent && (event.abilityIdMatches(0xB5CF) || event.ability.name.contains("Awesome")))`

				8000400 = B5D7 spread
				800040 = B5D8 light party
				3ED = B5D0 light party
				3EE = B5CF spread
				3F0 = B5CF spread but not the same one

				 */
				var event = s.waitEvent(BaseEvent.class, e ->
						(e instanceof MapEffectEvent mee
						 && mee.indexMatches(0x8000400, 0x800040))
						|| (e instanceof StatusLoopVfxApplied vfx
						    && vfx.getTarget().npcIdMatches(19288)
						    && vfx.vfxIdMatches(0x3ED, 0x3EE)));
				if (event instanceof MapEffectEvent mee) {
					boolean spread = mee.indexMatches(0x8000400);
					s.updateCall(spread ? sickestTakeOffSpreadLater : sickestTakeOffLightPartyLater);
					var raidwide = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB5CC), false);
					s.updateCall(spread ? sickestTakeOffSpread : sickestTakeOffLightParty, raidwide);
				}
				else if (event instanceof StatusLoopVfxApplied vfx) {
					boolean spread = vfx.vfxIdMatches(0x3EE);
					s.updateCall(spread ? sickestTakeOffSpreadLater : sickestTakeOffLightPartyLater);
					var raidwide = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB5CC), false);
					s.updateCall(spread ? sickestTakeOffSpread : sickestTakeOffLightParty, raidwide);
				}
			});

	private static final ArenaPos ap = new ArenaPos(100, 100, 10, 10);

	private final ModifiableCallout<AbilityCastStart> sickestTakeoffHitting = ModifiableCallout.durationBasedCallWithOffset("Sickest Take-off: Hitting Location", "From {from}, Hitting {unsafe}", Duration.ofMillis(0));

	// Sickest take-off - B5CE has the location info
	@AutoFeed
	private final SequentialTrigger<BaseEvent> sickestLocationSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5CE),
			(e1, s) -> {
				var e1loc = s.findOrWaitForCastWithLocation(casts, acs -> acs == e1, false);
				// If facing north, then it is going from the south
				var from = ArenaPos.combatantFacing(e1loc.getLocationInfo().getBestHeading()).opposite();
				// The location is at the edge of the aoe, so translate it 25 units forward to get to the center
				var hitting = ap.forPosition(e1loc.getLocationInfo().getPos().translateRelative(0, 25));
				s.setParam("from", from);
				s.setParam("unsafe", hitting);
				// Arbitrary wait to not talk over other calls
				s.waitMs(1_900);
				s.updateCall(sickestTakeoffHitting, e1);
			});

	@NpcCastCallout(value = {0xB5B8, 0xB5B9}, suppressMs = 500)
	private final ModifiableCallout<AbilityCastStart> diverDare = ModifiableCallout.durationBasedCall("Diver's Dare", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> alleyOopDoubleDip = ModifiableCallout.durationBasedCall("Alley-oop Double Dip", "Proteans, Move");
	private final ModifiableCallout<AbilityCastStart> reverseAlleyOop = ModifiableCallout.durationBasedCall("Reverse Alley-oop", "Proteans, Stay");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> alleyOopSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5DD, 0xB5E0),
			(e1, s) -> {
				// Don't call these in this phase if the player has fire debuff - they will be doing the other mechanic
				if (buffs.isStatusOnTarget(state.getPlayer(), 0x136E)) {
					return;
				}
				if (e1.abilityIdMatches(0xB5DD)) {
					s.updateCall(alleyOopDoubleDip, e1);
				}
				else {
					s.updateCall(reverseAlleyOop, e1);
				}
			});

	private final ModifiableCallout<AbilityCastStart> deepImpactTank = ModifiableCallout.durationBasedCall("Deep Impact (Tank)", "Bait Buster Far");
	private final ModifiableCallout<AbilityCastStart> deepImpactNonTank = ModifiableCallout.durationBasedCall("Deep Impact (Non-tank)", "In, Avoid Tank");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepImpact = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5B7),
			(e1, s) -> {
				// This trigger handles the calls entirely
				if (pyrotationSq.isActive()) {
					return;
				}
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(deepImpactTank, e1);
				}
				else {
					s.updateCall(deepImpactNonTank, e1);
				}
			});

	private final ModifiableCallout<AbilityCastStart> xtremeSpectacular = ModifiableCallout.durationBasedCall("Xtreme Spectacular", "Out, Multiple Raidwides");

	// these happen in pairs - annoying for a trigger!
	// insane air protean, light party, or buster

	private static final long
			BLUE_PROTEAN = 0x2_0001,
			BLUE_LIGHTPARTY = 0x20_0010,
			BLUE_BUSTER = 0x80_0040,
			RED_PROTEAN = 0x200_0100,
			RED_LIGHTPARTY = 0x800_0400,
			RED_BUSTER = 0x2000_1000;

	private static boolean isSurfboardEffect(MapEffectEvent mee) {
		// Other locations use these same values for other purposes, so filter based on location as well.
		return mee.getLocation() >= 0xE && mee.getLocation() <= 0x16
		       && mee.indexMatches(
				BLUE_PROTEAN, BLUE_LIGHTPARTY, BLUE_BUSTER, RED_PROTEAN, RED_LIGHTPARTY, RED_BUSTER
		);
	}

	private final ModifiableCallout<MapEffectEvent> insaneAirMechanics = new ModifiableCallout<MapEffectEvent>("Insane Air: Mechanics", "Red {['Protean', 'Stack', 'Buster', 'Error'][redMech]}, Blue {['Protean', 'Stack', 'Buster', 'Error'][blueMech]}")
			.extendedDescription("""
					The mechanic values are 0 for protean, 1 for light party, 2 for buster.
					Use the default output as an example of how to customize the calls.
					If you have a fixed group, you can delete the other group's callout to avoid clutter.""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> xtremeSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5A4),
			(e1, s) -> {
				s.updateCall(xtremeSpectacular, e1);
				// After this is the stuff for Insane Air
				// https://discord.com/channels/551474815727304704/594899820976668673/1458390191134736414
				/*
				 */
				for (int i = 0; i < 4; i++) {
					var effects = s.waitEventsQuickSuccession(2, MapEffectEvent.class, M10S::isSurfboardEffect);
					log.info("Surfboard Effects: {}", effects);
					s.setParam("effects", effects);
					s.setParam("redMech", -1);
					s.setParam("blueMech", -1);
					for (MapEffectEvent effect : effects) {
						long value = effect.getFlags();
						if (value == BLUE_PROTEAN) {
							s.setParam("blueMech", 0);
						}
						else if (value == BLUE_LIGHTPARTY) {
							s.setParam("blueMech", 1);
						}
						else if (value == BLUE_BUSTER) {
							s.setParam("blueMech", 2);
						}
						else if (value == RED_PROTEAN) {
							s.setParam("redMech", 0);
						}
						else if (value == RED_LIGHTPARTY) {
							s.setParam("redMech", 1);
						}
						else if (value == RED_BUSTER) {
							s.setParam("redMech", 2);
						}
					}
					s.updateCall(insaneAirMechanics, effects.get(0));
					s.waitMs(200);
				}
			});

	// B897 + b898, but the map effect is before that?
	// Also say B899 + B89A

	private final ModifiableCallout<AbilityCastStart> snaking = ModifiableCallout.durationBasedCall("Snaking", "Raidwide");
	private final ModifiableCallout<?> snakingFire = new ModifiableCallout<>("Snaking: Fire", "Fire").statusIcon(0x136E);
	private final ModifiableCallout<?> snakingWater = new ModifiableCallout<>("Snaking: Water", "Water").statusIcon(0x136F);
	@AutoFeed
	private final SequentialTrigger<BaseEvent> snakingSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB381),
			(e1, s) -> {
				s.updateCall(snaking, e1);
				// Player will get hit by one or the other which is actually
				var playerHit = s.waitEvent(
						AbilityUsedEvent.class,
						aue -> aue.getTarget().isThePlayer() && aue.abilityIdMatches(0xB381, 0xB382));
				boolean fire = playerHit.abilityIdMatches(0xB381);
				s.updateCall(fire ? snakingFire : snakingWater);
				// For some of the skills, the fake cast has better timing info, so use the name ID rather than the NPC ID
				// TODO: do we actually need any calls here? Everything is unit targeted anyway.
//				long expectedNameId = fire ? 14370 : 14369;
//				for (int i = 0; i < 4; i++) {
//					s.findOrWaitForCast(casts, acs -> {
//						// We don't need spreads because they cast directly on the target thus are already handled by another trigger
//						return acs.getSource().getbNpcNameId() == expectedNameId && acs.abilityIdMatches(
//								// Proteans
//								0xB5DD,
//								// Spread (b5c0 is the actual boss cast)
//								0xB5C1,
//								// Steam Burst (out) TODO this doesn't come from boss
//								0xB5FB,
//								// Sick Swell (?)
//								0xB5CB,
//								// Deep Varial (5s)
//								0xB891,
//								// Deep Varial (6.5s)
//								0xB5D3
//
//						);
//					}, false);
//				}
			});
	//
	private final ModifiableCallout<AbilityCastStart> deepVarial = ModifiableCallout.durationBasedCall("Deep Varial", "Cleave from {where}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepVarialSq = SqtTemplates.sq(
			30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5D3),
			(e1, s) -> {
				var cast = s.findOrWaitForCastWithLocation(casts, acs -> acs == e1, false);
				var where = ap.forPosition(cast.getLocationInfo().getPos());
				s.setParam("where", where);
				s.updateCall(deepVarial, e1);
			});

	private final ModifiableCallout<AbilityCastStart> hotAerialFire = ModifiableCallout.<AbilityCastStart>durationBasedCall("Hot Aerial w/ Fire", "Bait Far").statusIcon(0x136e);
	private final ModifiableCallout<AbilityCastStart> hotAerialNotFire = ModifiableCallout.<AbilityCastStart>durationBasedCall("Hot Aerial no Fire", "Stay Close").statusIcon(0x136f);
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hotAerialSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5C4),
			(e1, s) -> {
				if (buffs.isStatusOnTarget(state.getPlayer(), 0x136e)) {
					s.updateCall(hotAerialFire, e1);
				}
				else {
					s.updateCall(hotAerialNotFire, e1);
				}
			});

	private final ModifiableCallout<AbilityCastStart> deepAerialCast = ModifiableCallout.durationBasedCall("Deep Aerial: Cast", "Orb");
	private final ModifiableCallout<HeadMarkerEvent> deepAerialWater = new ModifiableCallout<>("Deep Aerial: Water On You", "Stretch, Avoid Water");
	private final ModifiableCallout<HeadMarkerEvent> deepAerialFire = new ModifiableCallout<>("Deep Aerial: Fire On You", "Stretch Through Water");
	private final ModifiableCallout<?> deepAerialNothing = new ModifiableCallout<>("Deep Aerial: No Tether", "Avoid Tethers").disabledByDefault();


	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepAerial = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5E4),
			(e1, s) -> {
				s.updateCall(deepAerialCast, e1);
				//noinspection InfiniteLoopStatement - This trigger is force terminated by deepArialEnd
				while (true) {
					List<HeadMarkerEvent> markers = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> true);
					markers.stream().filter(m -> m.markerIdMatches(635, 636)
					                             && m.getTarget().isThePlayer()).findFirst().ifPresentOrElse(m -> {
						if (m.getMarkerId() == 635) {
							s.updateCall(deepAerialWater, m);
						}
						else {
							s.updateCall(deepAerialFire, m);
						}
					}, () -> {
						s.updateCall(deepAerialNothing);
					});
					s.waitMs(100);
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepAerialEnd = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5E4),
			(e1, s) -> {
				// Wait for orb to die and kill the deep aerial trigger
				s.waitEvent(TargetabilityUpdate.class, tu -> tu.getTarget().npcIdMatches(19292) && !tu.isTargetable());
				deepAerial.stopSilently();
			});


	/*
	Notes:
	Sick Swell still needs some stuff (safe spot and kb direction)
	Missing calls for when there are 2 of them (before snaking) (insane air)
		I think this is Map Effect?

	Snaking: missing TB call
	Missing jumping call
	Missing the sick swells in general

	 */

	/**
	 * Splits arena in two
	 */
	private final ModifiableCallout<AbilityCastStart> flameFloaterSplit = ModifiableCallout.durationBasedCall("Flame Floater (Split)", "Arena Split");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> flameFloater2sq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5D4),
			(e1, s) -> {
				s.updateCall(flameFloaterSplit, e1);
			});

	@NpcCastCallout(0xB596)
	private final ModifiableCallout<AbilityCastStart> freakyPyrotation = ModifiableCallout.durationBasedCall("Freaky Pyro-rotation", "Partners");

	private final ModifiableCallout<AbilityCastStart> exSnaking = ModifiableCallout.durationBasedCall("Xtreme Snaking", "Raidwide");

	@PlayerStatusCallout(value = 0x12DB, cancellable = true)
	private final ModifiableCallout<BuffApplied> exSnakingFire = ModifiableCallout.<BuffApplied>durationBasedCall("Xtreme Snaking: Fire", "Fire").autoIcon();
	@PlayerStatusCallout(value = 0x12DC, cancellable = true)
	private final ModifiableCallout<BuffApplied> exSnakingWater = ModifiableCallout.<BuffApplied>durationBasedCall("Xtreme Snaking: Water", "Water").autoIcon();

	private final ModifiableCallout<?> exSnakingBuster = new ModifiableCallout<>("Xtreme Snaking: Buster", "Buster");
	private final ModifiableCallout<?> exSnakingLightParty = new ModifiableCallout<>("Xtreme Snaking: Light Party", "Light Party");
	private final ModifiableCallout<?> exSnakingProteans = new ModifiableCallout<>("Xtreme Snaking: Proteans", "Proteans");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> exSnakingSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5AE, 0xB5AF),
			(e1, s) -> {
				s.updateCall(exSnaking, e1);
				for (int i = 0; i < 4; i++) {
					// Both bosses do the same action this time, so we only need to look at one event.
					var effect = s.waitEvent(MapEffectEvent.class, M10S::isSurfboardEffect);
					long index = effect.getIndex();
					if (index == BLUE_PROTEAN || index == RED_PROTEAN) {
						s.updateCall(exSnakingProteans);
					}
					else if (index == BLUE_LIGHTPARTY || index == RED_LIGHTPARTY) {
						s.updateCall(exSnakingLightParty);
					}
					else if (index == BLUE_BUSTER || index == RED_BUSTER) {
						s.updateCall(exSnakingBuster);
					}
					s.waitMs(1_000);
				}
			});

	@NpcCastCallout(value = {0xB5FC, 0xB5FD}, suppressMs = 200)
	private final ModifiableCallout<AbilityCastStart> overTheFalls = ModifiableCallout.durationBasedCall("Over the Falls", "Enrage");

	@NpcCastCallout(value = 0xB5FB, suppressMs = 1_000)
	private final ModifiableCallout<AbilityCastStart> steamBurst = ModifiableCallout.<AbilityCastStart>durationBasedCall("Steam Burst", "Avoid Steam Orb")
			.disabledByDefault()
			.extendedDescription("""
					This trigger fires on all generic 'Steam Burst' explosions.""");

	/*
	 */
}

