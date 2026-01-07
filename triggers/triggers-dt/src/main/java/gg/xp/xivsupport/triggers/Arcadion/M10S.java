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
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CalloutRepo(name = "M10S", duty = KnownDuty.M10S)
public class M10S extends AutoChildEventHandler implements FilteredEventHandler {

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

	@NpcCastCallout(0xB5B6)
	private final ModifiableCallout<AbilityCastStart> hotImpact = ModifiableCallout.durationBasedCall("Hot Impact", "Buster on {event.target}");

	private final ModifiableCallout<BuffApplied> flameFloater1 = new ModifiableCallout<BuffApplied>("Flame Floater", "First", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater2 = new ModifiableCallout<BuffApplied>("Flame Floater", "Second", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater3 = new ModifiableCallout<BuffApplied>("Flame Floater", "Third", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloater4 = new ModifiableCallout<BuffApplied>("Flame Floater", "Fourth", 30_000).autoIcon();
	private final ModifiableCallout<BuffApplied> flameFloaterNothing = new ModifiableCallout<BuffApplied>("Flame Floater", "Nothing", 30_000);

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

	private final ModifiableCallout<AbilityCastStart> alleyOop = ModifiableCallout.durationBasedCall("Alley-oop Inferno", "Spread");
	private final ModifiableCallout<?> alleyOopMove = new ModifiableCallout<>("Alley-oop Inferno, Move", "Move");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> alleyOopSq = SqtTemplates.beginningAndEndingOfCast(
			acs -> acs.abilityIdMatches(0xB5C1) && acs.getTarget().isThePlayer(),
			alleyOop,
			alleyOopMove
	);

	@NpcCastCallout(0xB5C9)
	private final ModifiableCallout<AbilityCastStart> cutbackBlaze = ModifiableCallout.durationBasedCall("Cutback Blaze", "Raidwide then Find Safe Spot");

	@NpcCastCallout(0xB5C2)
	private final ModifiableCallout<AbilityCastStart> pyrotation = ModifiableCallout.durationBasedCall("Pyrotation", "Stacks and Puddles");
	// TODO: move call?

	/*
	-8.2 Sick Swell B5CB
	+0 Sickest Take-off 3.7s B5CD - indicates later mechanic? spread? spread also has vfxloop 0x3ee
	+6 Sickest Take-off 6.7s B5CE
	+6 Sick Swell B5CC - raidwide?
	+15 Awesome Splash (spread)?
	+15 Awesome Slab (light parties) 0x3ED vfx



	Reverse Alley-oop B5E0, followed by B5E1
	 */

	private final ModifiableCallout<StatusLoopVfxApplied> sickestTakeOffSpreadLater = new ModifiableCallout<>("Sickest Take-off Spread Later", "Spread Later");
	private final ModifiableCallout<StatusLoopVfxApplied> sickestTakeOffLightPartyLater = new ModifiableCallout<>("Sickest Take-off LP Later", "Light Party Later");
	private final ModifiableCallout<AbilityCastStart> sickestTakeOffSpread = ModifiableCallout.durationBasedCall("Sickest Take-off Spread Now", "Raidwide then Spread");
	private final ModifiableCallout<AbilityCastStart> sickestTakeOffLightParty = ModifiableCallout.durationBasedCall("Sickest Take-off LP Now", "Raidwide then Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sickestTakeOff = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5CB),
			(e1, s) -> {
				var vfx = s.waitEvent(StatusLoopVfxApplied.class, v -> v.getTarget().npcIdMatches(19288));
				boolean spread = vfx.vfxIdMatches(0x3EE);
				s.updateCall(spread ? sickestTakeOffSpreadLater : sickestTakeOffLightPartyLater, vfx);
				var raidwide = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xB5CC), false);
				s.updateCall(spread ? sickestTakeOffSpread : sickestTakeOffLightParty, raidwide);
			});

	@NpcCastCallout(value = {0xB5B8, 0xB5B9}, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> diverDare = ModifiableCallout.durationBasedCall("Diver's Dare", "Raidwide");

	// Alley-oop double dip proteans

	// Deep Impact something with tanks?


	@NpcCastCallout(0xB5DD) // TODO: this needs to be locked out during the water/fire phase
	private final ModifiableCallout<AbilityCastStart> alleyOopDoubleDip = ModifiableCallout.durationBasedCall("Alley-oop Double Dip", "Proteans");

	private final ModifiableCallout<AbilityCastStart> deepImpactTank = ModifiableCallout.durationBasedCall("Deep Impact (Tank)", "Bait Buster Far");
	private final ModifiableCallout<AbilityCastStart> deepImpactNonTank = ModifiableCallout.durationBasedCall("Deep Impact (Non-tank)", "In, Avoid Tank");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepImpact = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5B7),
			(e1, s) -> {
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(deepImpactTank, e1);
				}
				else {
					s.updateCall(deepImpactNonTank, e1);
				}
			});

	@NpcCastCallout(0xB5A4)
	private final ModifiableCallout<AbilityCastStart> xtremeSpectacular = ModifiableCallout.durationBasedCall("Xtreme Spectacular", "Out, Multiple Raidwides");

	// these happen in pairs - annoying for a trigger!
	// insane air protean, light party, or buster

	private final ModifiableCallout<AbilityCastStart> snaking = ModifiableCallout.durationBasedCall("Snaking", "Raidwide");
	private final ModifiableCallout<?> snakingFire = new ModifiableCallout<>("Snaking: Fire", "Fire");
	private final ModifiableCallout<?> snakingWater = new ModifiableCallout<>("Snaking: Water", "Water");
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
				long expectedNameId = fire ? 14370 : 14369;
				for (int i = 0; i < 4; i++) {
					s.findOrWaitForCast(casts, acs -> {
						return acs.getSource().getbNpcNameId() == expectedNameId && acs.abilityIdMatches(
								// Proteans
								0xB5DD,
								// Spread (b5c0 is the actual boss cast)
								0xB5C1,
								// Steam Burst (out) TODO this doesn't come from boss
								0xB5FB,
								// Sick Swell (?)
								0xB5CB,
								// Deep Varial (5s)
								0xB891,
								// Deep Varial (6.5s)
								0xB5D3

						);
					}, false);
				}
			});

	private final ModifiableCallout<HeadMarkerEvent> deepAerialWater = new ModifiableCallout<>("Deep Aerial: Water On You", "Stretch, Avoid Water");
	private final ModifiableCallout<HeadMarkerEvent> deepAerialFire = new ModifiableCallout<>("Deep Aerial: Fire On You", "Stretch Through Water");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> deepAerial = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB5E4),
			(e1, s) -> {
				// TODO: how many are there? does it just go until the orb dies?
				// Should just force terminate this when the orb dies
				while (true) {
					List<HeadMarkerEvent> markers = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> true);
					markers.stream().filter(m -> m.getTarget().isThePlayer()).findFirst().ifPresent(m -> {
						if (m.getMarkerId() == 635) {
							s.updateCall(deepAerialWater, m);
						}
						else {
							s.updateCall(deepAerialFire, m);
						}
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

}

