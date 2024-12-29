package gg.xp.xivsupport.triggers.car;

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
import gg.xp.xivsupport.events.actlines.events.abilityeffect.CastInterruptEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@CalloutRepo(name = "CoD (CAR) Triggers", duty = KnownDuty.CodCar)
public class CodCar extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(CodCar.class);
	private XivState state;
	private final StatusEffectRepository buffs;
	private ActiveCastRepository casts;

	public CodCar(XivState state, StatusEffectRepository buffs, ActiveCastRepository casts) {
		this.state = state;
		this.buffs = buffs;
		this.casts = casts;
	}

	public CodCarSection getPlayerSection() {
		if (buffs.isStatusOnTarget(state.getPlayer(), 0x1051)) {
			log.info("My area: INSIDE");
			return CodCarSection.INSIDE;
		}
		else if (buffs.isStatusOnTarget(state.getPlayer(), 0x1052)) {
			var mySection = CodCarSection.forPos(state.getPlayer().getPos());
			log.info("My area: {}", mySection);
			return mySection;
		}
		else {
			return null;
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.CodCar);
	}

	// Intro
	@NpcCastCallout(0x9DFC)
	private final ModifiableCallout<AbilityCastStart> bladeOfDarknessRight = ModifiableCallout.durationBasedCall("Blade of Darkness: Right Safe", "Right");
	@NpcCastCallout(0x9DFE)
	private final ModifiableCallout<AbilityCastStart> bladeOfDarknessLeft = ModifiableCallout.durationBasedCall("Blade of Darkness: Left Safe", "Left");
	@NpcCastCallout(0x9E00)
	private final ModifiableCallout<AbilityCastStart> bladeOfDarknessOut = ModifiableCallout.durationBasedCall("Blade of Darkness: Out", "Out");

	// P1

	private final ModifiableCallout<BuffApplied> grimEmbraceFrontInitial = new ModifiableCallout<BuffApplied>("Grim Embrace: Initial Front Dodge", "Later: Dodge Forwards").autoIcon();
	private final ModifiableCallout<BuffApplied> grimEmbraceRearInitial = new ModifiableCallout<BuffApplied>("Grim Embrace: Initial Rear Dodge", "Later: Dodge Backwards").autoIcon();
	private final ModifiableCallout<BuffApplied> grimEmbraceFrontSoon = new ModifiableCallout<BuffApplied>("Grim Embrace: Front Dodge Soon", "Dodge Forwards").autoIcon();
	private final ModifiableCallout<BuffApplied> grimEmbraceRearSoon = new ModifiableCallout<BuffApplied>("Grim Embrace: Rear Dodge Soon", "Dodge Backwards").autoIcon();
	private final ModifiableCallout<BuffApplied> grimEmbraceFrontMove = new ModifiableCallout<BuffApplied>("Grim Embrace: Rear Dodge Now", "Move").autoIcon();
	private final ModifiableCallout<BuffApplied> grimEmbraceRearMove = new ModifiableCallout<BuffApplied>("Grim Embrace: Rear Dodge Now", "Move").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> grimEmbrace = SqtTemplates.sq(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E39, 0x9E3A),
			(e1, s) -> {
				boolean isFront = e1.abilityIdMatches(0x9E3A);
				List<BuffApplied> buffs = s.waitEventsQuickSuccession(12, BuffApplied.class, ba -> ba.buffIdMatches(0x1055));
				buffs.stream().filter(ba -> ba.getTarget().isThePlayer())
						.findAny()
						.ifPresent(b -> {
							s.updateCall(isFront ? grimEmbraceFrontInitial : grimEmbraceRearInitial, b);
							s.waitDuration(b.remainingDurationPlus(Duration.ofSeconds(-5)));
							s.updateCall(isFront ? grimEmbraceFrontSoon : grimEmbraceRearSoon, b);
							s.waitDuration(b.remainingDurationPlus(Duration.ofMillis(-1_000)));
							s.updateCall(isFront ? grimEmbraceFrontMove : grimEmbraceRearMove, b);
						});
			});

	// Real death IV:
	// Boss casts 0x9E43 (5.3s, no aoe) BEFORE the others
	// Ball of Naught casts 0x9E46 (5.7s, donut)
	// Fake boss casts 0x9E45 (3.7s, circle)
	// Fake boss casts 0x9E44 (vortex, 1.7s, pull-in)
	// KB seems to be scripted?
	// Endeath IV:
	// Boss casts 9E53 beforehand, giving the 0x1056 status
	// Ball casts 9E49 (4.7s, donut)
	// Fake boss casts 9E48 (2.7s, circle)
	// Fake boss casts 9E47 (vortex, 0.7s, pull-in)

	@NpcCastCallout(0x9E4C)
	private final ModifiableCallout<?> aeroIV = new ModifiableCallout<>("Aero IV", "Knockback");

	private final ModifiableCallout<?> deathIV = new ModifiableCallout<>("Death IV", "Out of Middle");
	private final ModifiableCallout<AbilityCastStart> deathIVin = ModifiableCallout.durationBasedCall("Death IV: Inside", "In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> deathIVsq = SqtTemplates.sq(15_000,
			// Start on the real cast
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E43),
			(e1, s) -> {
				s.updateCall(deathIV);
				// Wait for inner circle to pop
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E45));
				// Use the donut cast as the time basis
				var e2 = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9E46), false);
				s.updateCall(deathIVin, e2);
			});

	private static final long[] ALL_BLADE_IDS = {0x9DFC, 0x9DFE, 0x9E00};

	private final ModifiableCallout<AbilityCastStart> enaeroInitial = ModifiableCallout.durationBasedCall("Enaero: Initial", "Stocking Aero");
	private final ModifiableCallout<AbilityCastStart> endeathInitial = ModifiableCallout.durationBasedCall("Endeath: Initial", "Stocking Death");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> enaeroSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E54),
			(e1, s) -> {
				s.updateCall(enaeroInitial, e1);
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(ALL_BLADE_IDS));
				// Don't talk over the other call
				s.waitMs(3_000);
				s.updateCall(aeroIV);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> endeathSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E53),
			(e1, s) -> {
				s.updateCall(endeathInitial, e1);
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(ALL_BLADE_IDS));
				// Don't talk over the other call
				s.waitMs(3_000);
				s.updateCall(deathIV);
				// Wait for inner circle to pop
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E48));
				// Use the donut cast as the time basis
				var e2 = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9E49), false);
				s.updateCall(deathIVin, e2);
			});

	@NpcCastCallout({0x9E3E, 0x9E07})
	private final ModifiableCallout<AbilityCastStart> floodOfDarkness = ModifiableCallout.durationBasedCall("Flood of Darkness", "Raidwide");

	@NpcCastCallout(0x9E0D)
	private final ModifiableCallout<AbilityCastStart> wildCharge = ModifiableCallout.durationBasedCall("Wild Charge", "Stacks, Tanks in Front");

	private final ModifiableCallout<AbilityCastStart> flare = ModifiableCallout.durationBasedCallWithOffset("Flare", "Flare", Duration.ofMillis(9_300));
	private final ModifiableCallout<AbilityCastStart> flareOnYou = ModifiableCallout.durationBasedCallWithOffset("Flare on You", "Out", Duration.ofMillis(9_300));
	private final ModifiableCallout<AbilityCastStart> flareNotOnYou = ModifiableCallout.durationBasedCallWithOffset("Flare Not on You", "Middle", Duration.ofMillis(9_300));

	@AutoFeed
	private final SequentialTrigger<BaseEvent> flareSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E58),
			(e1, s) -> {
				s.updateCall(flare, e1);
				// This fight does not have consistent marker offsets
				s.waitCastFinished(casts, e1);
				List<HeadMarkerEvent> headmarkers = s.waitEventsQuickSuccession(3, HeadMarkerEvent.class, hme -> true);
				headmarkers.stream().filter(hm -> hm.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(h -> s.updateCall(flareOnYou, e1),
								() -> s.updateCall(flareNotOnYou, e1));
			});

	@NpcCastCallout(0xA12D)
	private final ModifiableCallout<AbilityCastStart> unholyDarkness = ModifiableCallout.durationBasedCallWithOffset("Unholy Darkness", "Light Party Stacks", Duration.ofMillis(9_100));
	@NpcCastCallout(0x9E40)
	private final ModifiableCallout<AbilityCastStart> rapidSequence = ModifiableCallout.durationBasedCall("Rapid-sequence Particule Beam", "Wild Charges");

	@NpcCastCallout(0x9E50)
	private final ModifiableCallout<AbilityCastStart> breakIV = ModifiableCallout.durationBasedCall("Break IV", "Look Away");

	@NpcCastCallout(value = 0x9E3F, suppressMs = 30_000)
	private final ModifiableCallout<AbilityCastStart> razingVolley = ModifiableCallout.durationBasedCall("Razing Volley", "Dodge Lasers");


	// P2

	@NpcCastCallout(0x9E10)
	private final ModifiableCallout<AbilityCastStart> diffusive = ModifiableCallout.durationBasedCall("Diffusive-force Particle Beam", "Spread");

	@NpcCastCallout(0x9E0B)
	private final ModifiableCallout<AbilityCastStart> ghastlyGloomDonut = ModifiableCallout.durationBasedCall("Ghastly Gloom (Huge Donut)", "In");

	@NpcCastCallout(0x9E09)
	private final ModifiableCallout<AbilityCastStart> ghastlyGloomCross = ModifiableCallout.durationBasedCall("Ghastly Gloom (Cross)", "Corners");

	@NpcCastCallout(0x9E33)
	private final ModifiableCallout<BuffApplied> curseOfDarknessInitialDamage = ModifiableCallout.durationBasedCall("Curse of Darkness (Initial Damage)", "Raidwide");
	private final ModifiableCallout<BuffApplied> curseOfDarkness = ModifiableCallout.durationBasedCall("Curse of Darkness (In 3 Seconds)", "Look Out");

	private final ModifiableCallout<AbilityCastStart> floodOfDarknessAdds = ModifiableCallout.durationBasedCall("Flood of Darkness (Interruptable)", "Interrupt Adds");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> floodOfDarknessAddsSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E37),
			(e1, s) -> {
				if (doWeCareAboutStygian(e1.getSource()) && state.playerJobMatches(Job::caresAboutInterrupt)) {
					RawModifiedCallout<AbilityCastStart> call = s.updateCall(floodOfDarknessAdds, e1);
					// Unfortunately, AbilityCastCancel currently does not work for interrupts, so we need to get creative
					s.waitEventsUntil(1, AbilityUsedEvent.class, aue -> {
						// Wait for something with an interrupt effect hitting the add
						return aue.getTarget().equals(e1.getSource()) && !aue.getEffectsOfType(CastInterruptEffect.class).isEmpty();
					}, AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
					call.forceExpire();
				}
			});

	@NpcCastCallout({0x9E01, 0x9E3D})
	private final ModifiableCallout<AbilityCastStart> deluge = ModifiableCallout.durationBasedCall("Deluge of Darkness", "Raidwide with Bleed");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> curseOfDarknessSq = SqtTemplates.callWhenDurationIs(
			BuffApplied.class,
			ba -> ba.buffIdMatches(0x953) && ba.getTarget().isThePlayer(),
			curseOfDarkness,
			Duration.ofSeconds(3));

	@NpcCastCallout(value = 0xA2C9, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> loomingChaos = ModifiableCallout.durationBasedCall("Looming Chaos", "Prepare for Swaps");

	@NpcCastCallout(0x9E08)
	private final ModifiableCallout<AbilityCastStart> darkDominion = ModifiableCallout.durationBasedCall("Dark Dominion", "Raidwide");


	// TODO: wild charge

	/*
	 * Third Art of Darkness (cleaves, buddies/spread)
	 * Two initial casts: 9E20 and 9E23
	 * Headmarkers happen during the cast
	 *
	 * Example 1:
	 * 9E20 -> F0 F1 F0, east boss, facing east
	 * North Buddies North
	 *
	 * Example 2:
	 * 9E23 -> EF F1 EF, west boss, facing west
	 * North Buddies North
	 *
	 * Example 3:
	 * 9E20 -> F0 F1 EF, west boss, facing west
	 * South Buddies North
	 *
	 * Example 4:
	 * 9E23 -> EF F0 F1, east boss, facing east
	 * South North Buddies
	 *
	 * Exampel 5:
	 * 9E23 -> EF F0 F2, east boss, facing east
	 * South North Spread
	 *
	 * Seems to be:
	 * EF: Cleaving left
	 * F0: Cleaving right
	 *
	 */

	private enum ArtOfDarknessMech {
		CLEAVE_LEFT,
		CLEAVE_RIGHT,
		BUDDIES,
		PROTEANS;

		private static ArtOfDarknessMech forHm(HeadMarkerEvent hme) {
			return switch ((int) hme.getMarkerId()) {
				case 0xEF -> CLEAVE_LEFT;
				case 0xF0 -> CLEAVE_RIGHT;
				case 0xF1 -> BUDDIES;
				case 0xF2 -> PROTEANS;
				default -> null;
			};
		}
	}

	private final ModifiableCallout<?> artOfDarknessWestCleaveLeft = new ModifiableCallout<>("Third Art of Darkness: West Add Cleaving Left", "North");
	private final ModifiableCallout<?> artOfDarknessWestCleaveRight = new ModifiableCallout<>("Third Art of Darkness: West Add Cleaving Right", "South");
	private final ModifiableCallout<?> artOfDarknessEastCleaveLeft = new ModifiableCallout<>("Third Art of Darkness: East Add Cleaving Left", "South");
	private final ModifiableCallout<?> artOfDarknessEastCleaveRight = new ModifiableCallout<>("Third Art of Darkness: East Add Cleaving Right", "North");
	private final ModifiableCallout<?> artOfDarknessBuddies = new ModifiableCallout<>("Third Art of Darkness: Partner Stacks", "Buddies");
	private final ModifiableCallout<?> artOfDarknessProteans = new ModifiableCallout<>("Third Art of Darkness: Proteans", "Proteans");
	private final ModifiableCallout<?> artOfDarknessTowers = new ModifiableCallout<>("Third Art of Darkness: Towers", "Towers");

	private ModifiableCallout<?> calloutForMech(ArtOfDarknessMech artOfDarknessMech, boolean east) {
		return switch (artOfDarknessMech) {
			case CLEAVE_LEFT -> east ? artOfDarknessEastCleaveLeft : artOfDarknessWestCleaveLeft;
			case CLEAVE_RIGHT -> east ? artOfDarknessEastCleaveRight : artOfDarknessWestCleaveRight;
			case BUDDIES -> artOfDarknessBuddies;
			case PROTEANS -> artOfDarknessProteans;
		};
	}

	private static final long[] ALL_ART_IDS = {0x9E21, 0x9E22, 0x9E25, 0x9E26, 0x9E28};

	private boolean doWeCareAboutStygian(XivCombatant combatant) {
		// Check that this one is the one that the player is near
		// TODO: what if someone is walking on outer ring?
		double x = combatant.getPos().x();
		boolean isEast = x > 100;
		var mySection = getPlayerSection();
		log.info("doWeCareAboutStygian: x={}, mySection={}", x, mySection);
		if (isEast) {
			return mySection == CodCarSection.EAST_OUTSIDE;
		}
		else {
			return mySection == CodCarSection.WEST_OUTSIDE;
		}

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sq = SqtTemplates.selfManagedMultiInvocation(30_000,
			AbilityCastStart.class, acs -> {
				return acs.abilityIdMatches(0x9E20, 0x9E23)
				       && doWeCareAboutStygian(acs.getSource());
			},
			(e1, s, count) -> {
				XivCombatant npc = e1.getSource();
				boolean isEast = npc.getPos().x() > 0;

				var mech1 = ArtOfDarknessMech.forHm(s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().equals(npc)));
				ModifiableCallout<?> mc1 = calloutForMech(mech1, isEast);
				var call1 = s.call(mc1);

				var mech2 = ArtOfDarknessMech.forHm(s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().equals(npc)));
				ModifiableCallout<?> mc2 = calloutForMech(mech2, isEast);
				var call2 = s.call(mc2);

				var mech3 = ArtOfDarknessMech.forHm(s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().equals(npc)));
				ModifiableCallout<?> mc3 = calloutForMech(mech3, isEast);
				var call3 = s.call(mc3);

				// Wait so we don't talk over the call
				s.waitMs(1_500);

				RawModifiedCallout<?> mc1a = s.call(mc1);
				mc1a.setReplaces(call1);

				// Different mechanics take different amounts of time to resolve
				Consumer<ArtOfDarknessMech> waitMech = (mech) -> {
					if (mech == ArtOfDarknessMech.BUDDIES) {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E29));
					}
					else if (mech == ArtOfDarknessMech.PROTEANS) {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E27));
					}
					else {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(ALL_ART_IDS));
					}
				};
				waitMech.accept(mech1);

				mc1a.forceExpire();
				RawModifiedCallout<?> mc2a = s.call(mc2);
				mc2a.setReplaces(call2);
				// Debounce
				s.waitMs(1300);

				waitMech.accept(mech2);

				mc2a.forceExpire();
				RawModifiedCallout<?> mc3a = s.call(mc3);
				mc3a.setReplaces(call3);
				s.waitMs(1300);

				waitMech.accept(mech3);

				if (count == 0) {
					RawModifiedCallout<?> towerCall = s.call(artOfDarknessTowers);
					towerCall.setReplaces(mc3a);
				}
			});

	private final ModifiableCallout<HeadMarkerEvent> evilSeedOnYou = new ModifiableCallout<>("Evil Seed: On You", "Drop Bramble");
	private final ModifiableCallout<?> evilSeedNotOnYou = new ModifiableCallout<>("Evil Seed: Not On You", "Avoid Brambles");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> evilSeed = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E2A),
			(e1, s) -> {
				s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hme -> hme.getMarkerId() == 0x227)
						.stream()
						.filter(hme -> hme.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(h -> {
							s.updateCall(evilSeedOnYou, h);
						}, () -> s.updateCall(evilSeedNotOnYou));
			});

	// TODO: might be nice to have a player-to-player version of this that calls who you're tethered to
	@PlayerStatusCallout(value = 0x1BD, cancellable = true)
	private final ModifiableCallout<BuffApplied> thornyVine = ModifiableCallout.durationBasedCall("Thorny Vine", "Break Tether");

	private final ModifiableCallout<AbilityCastStart> lateralCore = ModifiableCallout.durationBasedCallWithOffset("Lateral-Core Phaser", "Sides then In", Duration.ofMillis(2000));
	private final ModifiableCallout<AbilityCastStart> coreLateral = ModifiableCallout.durationBasedCallWithOffset("Core-Lateral Phaser", "In then Sides", Duration.ofMillis(2000));
	private final ModifiableCallout<?> coreWithTower = new ModifiableCallout<>("Lateral-Core: Follow-Up with Tower", "In then Tower");
	private final ModifiableCallout<?> lateralWithTower = new ModifiableCallout<>("Core-Lateral: Follow-Up with Tower", "Out and Tower");
	private final ModifiableCallout<?> core = new ModifiableCallout<>("Lateral-Core: Follow-Up", "In");
	private final ModifiableCallout<?> lateral = new ModifiableCallout<>("Core-Lateral: Follow-Up", "Out");
	private final ModifiableCallout<?> coreWithPivot = new ModifiableCallout<>("Lateral-Core: Follow-Up with Pivot", "In then {rotationSafe}");
	private final ModifiableCallout<?> lateralWithPivot = new ModifiableCallout<>("Core-Lateral: Follow-Up with Pivot", "Out and {rotationSafe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> phaser = SqtTemplates.selfManagedMultiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E2F, 0x9E30)
			                               && doWeCareAboutStygian(acs.getSource()),
			(e1, s, count) -> {
				log.info("Phaser #{}", count);
				boolean sidesFirst = e1.abilityIdMatches(0x9E2F);
				AbilityCastStart pivotCast = casts.getActiveCastById(0x9E13, 0x9E15).stream().map(CastTracker::getCast).findFirst()
						.orElse(null);
				if (pivotCast == null) {
					// TODO: there is a version of this with neither towers nor pivot
					s.updateCall(sidesFirst ? lateralCore : coreLateral, e1);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E31));
					if (count >= 2) {
						s.updateCall(sidesFirst ? core : lateral);
					}
					else {
						s.updateCall(sidesFirst ? coreWithTower : lateralWithTower);
					}
				}
				else {
					boolean cw = pivotCast.abilityIdMatches(0x9E13);
					var mySection = getPlayerSection();
					if (mySection == CodCarSection.EAST_OUTSIDE) {
						s.setParam("rotationSafe", cw ? ArenaSector.NORTHEAST : ArenaSector.SOUTHEAST);
					}
					else if (mySection == CodCarSection.WEST_OUTSIDE) {
						s.setParam("rotationSafe", cw ? ArenaSector.SOUTHWEST : ArenaSector.NORTHWEST);
					}
					else {
						throw new IllegalStateException("How?");
					}
					s.updateCall(sidesFirst ? lateralCore : coreLateral, e1);
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9E31));
					s.updateCall(sidesFirst ? coreWithPivot : lateralWithPivot);
				}
			});

	private final ModifiableCallout<AbilityCastStart> pivotCW = ModifiableCallout.durationBasedCall("Pivot: Clockwise", "Clockwise");
	private final ModifiableCallout<AbilityCastStart> pivotCCW = ModifiableCallout.durationBasedCall("Pivot: Counter-Clockwise", "Counter-Clockwise");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> pivot = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E13, 0x9E15),
			(e1, s) -> {
				var mySection = getPlayerSection();
				if (mySection == CodCarSection.INSIDE) {
					s.updateCall(e1.abilityIdMatches(0x9E13) ? pivotCW : pivotCCW);
				}
			});

	private final ModifiableCallout<AbilityCastStart> excruciate = ModifiableCallout.durationBasedCall("Excruciate on You", "Tank Buster");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> excuciateSq = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9E36) && acs.getTarget().isThePlayer(),
			(e1, s) -> {
				s.updateCall(excruciate, e1);
			});

	@PlayerHeadmarker(0xC5)
	private final ModifiableCallout<HeadMarkerEvent> chaser = new ModifiableCallout<>("Feint Particle Beam", "Chasing AoE");

	@NpcCastCallout(value = 0x9E55, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> enrage = ModifiableCallout.durationBasedCall("Deluge of Darkness (Enrage)", "Enrage");
}
