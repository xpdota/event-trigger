package gg.xp.xivsupport.triggers.ultimate;

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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerConcurrencyMode;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CalloutRepo(name = "FRU Triggers", duty = KnownDuty.FRU)
public class FRU extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(FRU.class);

	// TODO - prios
	private final JobSortSetting defaultPrio;
	private final JobSortOverrideSetting p1tethersPrio;
	private final JobSortOverrideSetting p1towersPrio;
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public FRU(XivState state, PersistenceProvider pers, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.casts = casts;
		this.buffs = buffs;
		String settingKeyBase = "triggers.fru.";
		defaultPrio = new JobSortSetting(pers, settingKeyBase + "defaultPrio", state);
		p1tethersPrio = new JobSortOverrideSetting(pers, settingKeyBase + "p1-tethers-prio", state, defaultPrio);
		p1towersPrio = new JobSortOverrideSetting(pers, settingKeyBase + "p1-towers-prio", state, defaultPrio);
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.FRU);
	}

	private final ModifiableCallout<AbilityCastStart> cyclonicBreakInitialFire = ModifiableCallout.durationBasedCall("Cyclonic Break: Initial (Fire)", "Proteans and Buddies");
	private final ModifiableCallout<AbilityCastStart> cyclonicBreakInitialLightning = ModifiableCallout.durationBasedCall("Cyclonic Break: Initial (Lightning)", "Proteans and Spread");
	private final ModifiableCallout<AbilityUsedEvent> cyclonicBreakMove1fire = new ModifiableCallout<>("Cyclonic Break: Move 1 (Fire)", "Move, Buddies");
	private final ModifiableCallout<AbilityUsedEvent> cyclonicBreakMove1lightning = new ModifiableCallout<>("Cyclonic Break: Move 1 (Lightning)", "Move, Spread");
	private final ModifiableCallout<AbilityUsedEvent> cyclonicBreakMove2 = new ModifiableCallout<>("Cyclonic Break: Move 2", "Move");
	private final ModifiableCallout<AbilityUsedEvent> cyclonicBreakMove3 = new ModifiableCallout<>("Cyclonic Break: Move 3", "Move");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cyclonicBreak = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CD0, 0x9CD4), // 9CD0 is fire, 0CD4 is lightning
			(e1, s) -> {
				boolean isFire = e1.abilityIdMatches(0x9CD0);
				s.updateCall(isFire ? cyclonicBreakInitialFire : cyclonicBreakInitialLightning, e1);
				var e2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CD1));
				s.updateCall(isFire ? cyclonicBreakMove1fire : cyclonicBreakMove1lightning, e2);
				// Debounce
				s.waitMs(1_000);

				var e3 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CD2));
				s.updateCall(cyclonicBreakMove2, e3);
				// Debounce
				s.waitMs(1_000);

				var e4 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CD2));
				s.updateCall(cyclonicBreakMove3, e4);
				// Debounce
				s.waitMs(1_000);
			});

	private final ModifiableCallout<AbilityCastStart> powderMarkInitial = ModifiableCallout.durationBasedCall("Powder Mark Trail: Initial", "Buster on {event.target}");
	private final ModifiableCallout<BuffApplied> powderMarkBomb = ModifiableCallout.<BuffApplied>durationBasedCall("Powder Mark Trail: Debuff Placed", "Powder Mark on {event.target}").autoIcon();
	private final ModifiableCallout<BuffApplied> powderMarkBombSoon = ModifiableCallout.<BuffApplied>durationBasedCall("Powder Mark Trail: 3s Left", "Powder Mark on {event.target}").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> powderMarkTrail = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CE8),
			(e1, s) -> {
				s.updateCall(powderMarkInitial, e1);
				var debuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x1046));
				s.updateCall(powderMarkBomb, debuff);
				Duration targetDuration = Duration.ofSeconds(3);
				long msToWait = debuff.getEstimatedRemainingDuration().minus(targetDuration).toMillis();
				if (msToWait > 0) {
					s.waitMs(msToWait);
				}
				s.updateCall(powderMarkBombSoon, debuff);
			});

	private final ModifiableCallout<AbilityCastStart> utopianSkyStackInitial = ModifiableCallout.durationBasedCallWithoutDurationText("Utopian Sky: Initial (Fire)", "Stack Later");
	private final ModifiableCallout<AbilityCastStart> utopianSkySpreadInitial = ModifiableCallout.durationBasedCallWithoutDurationText("Utopian Sky: Initial (Lightning)", "Spread Later");
	private final ModifiableCallout<AbilityCastStart> utopianSkyStackSafeSpot = ModifiableCallout.durationBasedCall("Utopian Sky: Safe Spot (Fire)", "Stack {safe}");
	private final ModifiableCallout<AbilityCastStart> utopianSkySpreadSafeSpot = ModifiableCallout.durationBasedCall("Utopian Sky: Safe Spot (Lightning)", "Spread {safe}");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> utopianSky = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CDA, 0x9CDB),
			(e1, s) -> {
				boolean isFire = e1.abilityIdMatches(0x9CDA);
				s.updateCall(isFire ? utopianSkyStackInitial : utopianSkySpreadInitial, e1);
				// This is the "raising" animation. We can't use the casts directly since they position themselves in the middle.
				// Could use cast locations but that ends up being more complicated.
				var events = s.waitEventsQuickSuccession(3, ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x3f && acee.getData0() == 0x4);
				List<ArenaSector> safe;
				do {
					List<ArenaSector> tmpSafe = new ArrayList<>(ArenaSector.all);
					events.stream().map(acee -> arenaPos.forCombatant(state.getLatestCombatantData(acee.getTarget())))
							.forEach(pos -> {
								tmpSafe.remove(pos);
								tmpSafe.remove(pos.opposite());
							});
					safe = tmpSafe;
					if (safe.size() != 2) {
						s.waitThenRefreshCombatants(300);
					}
				} while (safe.size() != 2);
				s.setParam("safe", safe);
				// Get the actual cast so that we have something to display
				var cast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9CDE), false);
				s.updateCall(isFire ? utopianSkyStackSafeSpot : utopianSkySpreadSafeSpot, cast);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cyclonicBreak2 = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D89, 0x9D8A), // speculated
			(e1, s) -> {
				boolean isFire = e1.abilityIdMatches(0x9D89);
				// Don't talk over the other calls, and wait for blasting zone to be finished casting
				Optional<CastTracker> blastingZoneMaybe = casts.getActiveCastById(0x9CDE);
				blastingZoneMaybe.ifPresent(castTracker -> s.waitCastFinished(casts, castTracker.getCast()));

				s.updateCall(isFire ? cyclonicBreakInitialFire : cyclonicBreakInitialLightning, e1);
				var e2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CD1));
				s.updateCall(isFire ? cyclonicBreakMove1fire : cyclonicBreakMove1lightning, e2);
				// Debounce
				s.waitMs(1_000);

				var e3 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CD2));
				s.updateCall(cyclonicBreakMove2, e3);
				// Debounce
				s.waitMs(1_000);

				// The final movement is handled by turnNSSafe call below

				// Prior to the proteans (i.e. need another sq), there is, in one example:
				// Turn of the Heavens (6.7s) 9CD7 - determines safe spot?
				// Guessing 9CD6 is opposite, but don't know which is blue/red
				// Burnt Strike (7.7s) 9CE3 - probably E/W big cleave
				// Burnout (9.4s) 9CE4 - probably KB?
			});

	private final ModifiableCallout<AbilityCastStart> turnInitial = new ModifiableCallout<>("Turn of the Heavens: Initial", "{redSafe ? 'Red' : 'Blue'} Safe");
	private final ModifiableCallout<AbilityCastStart> turnNSSafe = ModifiableCallout.durationBasedCall("Turn of the Heavens: Dodge Lightning", "Move, North/South Out");
	private final ModifiableCallout<AbilityCastStart> turnKB = ModifiableCallout.durationBasedCall("Turn of the Heavens: Knockback Cast", "Get Knocked {safe}");
	private final ModifiableCallout<?> turnKBin = new ModifiableCallout<>("Turn of the Heavens: KB Move In", "Move In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> turnOfTheHeavensSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CD6, 0x9CD7),
			(e1, s) -> {
				// How do we determine where red or blue is?
				// For the rings, they are called Halo of Flame (NPC 17821:9710) or Halo of Levin (17822:9711)
				// 9CD6 is blue safe, 9CD7 is red safe
				boolean redSafe = e1.abilityIdMatches(0x9CD7);
				s.setParam("redSafe", redSafe);
				s.updateCall(turnInitial);
				s.waitMs(3_700);


				/* For this sequence:
				9CE3 Burnt Strike is the narrow lightning
				9CE4 Burnout is the wide lightning

				9CE1 Burnt Strike is the fire initial hit
				9CE2 Blastburn is the actual knockback
				*/

				AbilityCastStart wideLightning = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9CE3), false);
				s.updateCall(turnNSSafe, wideLightning);
				// 9CE3 is initial hit
				// 9CE4 is the big hit
				s.waitCastFinished(casts, wideLightning);
				ArenaSector safe;
				do {
					// Find orbs that are east or west
					// We are looking for the safe spot, so if fire is safe, look for the fire orb
					List<ArenaSector> eastWest = state.npcsById(redSafe ? 17821 : 17822).stream()
							//
							.map(fireOrb -> arenaPos.forCombatant(state.getLatestCombatantData(fireOrb)))
							.filter(pos -> pos == ArenaSector.WEST || pos == ArenaSector.EAST)
							.toList();
					if (eastWest.size() == 1) {
						safe = eastWest.get(0);
						break;
					}
					else {
						s.waitThenRefreshCombatants(200);
					}
				} while (true);
				s.setParam("safe", safe);

				AbilityCastStart burnout = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9CE1), false);
				// The KB animation skill is 9CE1 and has a cast
				s.updateCall(turnKB, burnout);
				s.waitCastFinished(casts, burnout);
				s.updateCall(turnKBin);
			});


	@NpcCastCallout(0x9CEA)
	private final ModifiableCallout<AbilityCastStart> burnishedGlory = ModifiableCallout.<AbilityCastStart>durationBasedCall("Burnished Glory", "Raidwide with Bleed").statusIcon(0xB87);

	/*
	 * P1:
	 * Proteans (baited), check lightning/fire
	 * Alternates 4 sets
	 * 1. Baits on player
	 * 2. Hits where #1 set was (i.e. dodge)
	 * 3. Move back
	 * 4. Move back
	 * Spread if lightning, stack if fire
	 * Tankbuster, leaves debuff (2451 / 0x993?)
	 *
	 * Ilusion mechanic
	 * Tank thing at the start
	 * Stack/spread based on last (spread like DSR)
	 *
	 * Check clone for safe spot (red/blue)
	 * Do proteans again
	 * Has stacks/spread mech
	 * Two tethers
	 * TODO: how do we check safe color?
	 *
	 * Four tether mechanic - in order, make priority system (AM later?)
	 *
	 * Finally, tanks to one side, everyone else do towers
	 */


	public enum MechType {
		Fire,
		Lightning,
		Holy
	}

	public static class FruP1TetherEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility {
		@Serial
		private static final long serialVersionUID = -9182985137525672763L;
		private final AbilityCastStart cast;
		private final XivCombatant source;
		private final XivPlayerCharacter target;
		private final MechType mechType;

		public FruP1TetherEvent(AbilityCastStart cast, XivCombatant source, XivPlayerCharacter target) {
			// There are a few fire variants, this is the only lightning
			this.mechType = cast.abilityIdMatches(0x9CCC) ? MechType.Lightning : MechType.Fire;
			this.cast = cast;
			this.source = source;
			this.target = target;
		}

		public MechType getMechType() {
			return mechType;
		}

		@Override
		public XivCombatant getSource() {
			return source;
		}

		@Override
		public XivPlayerCharacter getTarget() {
			return target;
		}

		@Override
		public XivAbility getAbility() {
			return cast.getAbility();
		}

		public AbilityCastStart getCastEvent() {
			return cast;
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> tetherHandler = SqtTemplates.sq(10_000,
			TetherEvent.class, te -> te.tetherIdMatches(0xF9, 0x11F),
			(tether, s) -> {
				// The first set (two tethers) has debuffs associated with the tethers, but not the second set (four tethers)
				var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.getSource().equals(tether.getSource()));
				// Bound of faith (0x9CE5) == Two Fire Tethers
				// Sinblaze (0x9CC9) == Sequential fire tether
				// ? == Sequential lightning tether
				s.accept(new FruP1TetherEvent(cast, cast.getSource(), (XivPlayerCharacter) tether.getTargetMatching(XivCombatant::isPc)));
			}).setConcurrency(SequentialTriggerConcurrencyMode.CONCURRENT); // these are not always well-ordered, but the tethers always come before the cast

	private static ModifiableCallout<FruP1TetherEvent> makeTetherCall(String descPart, int startIndex, int endIndex, boolean isFinal) {
		// If these are the final calls, call out the one that is going off. Otherwise, call the one we just collected
		int ttsIndex = isFinal ? startIndex : endIndex;
		String base = "{events[%d].mechType} on {events[%d].target}";
		String tts = isFinal ? base.formatted(ttsIndex, ttsIndex) : "";
		String text = IntStream.rangeClosed(startIndex, endIndex).mapToObj(idx -> base.formatted(idx, idx)).collect(Collectors.joining(", "));
		return new ModifiableCallout<>("Four Tethers: %s".formatted(descPart), tts, text);
	}

	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl1 = makeTetherCall("First Tether Out", 0, 0, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl2 = makeTetherCall("Second Tether Out", 0, 1, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl3 = makeTetherCall("Third Tether Out", 0, 2, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl4 = makeTetherCall("Fourth Tether Out", 0, 3, false);

	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving1 = makeTetherCall("First Tether Resolving", 0, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving2 = makeTetherCall("Second Tether Resolving", 1, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving3 = makeTetherCall("Third Tether Resolving", 2, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving4 = makeTetherCall("Fourth Tether Resolving", 3, 3, true);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fourTethers = SqtTemplates.sq(60_000,
			FruP1TetherEvent.class, te -> te.cast.abilityIdMatches(0x9CC9, 0x9CCC), /* TODO 9CCC is speculative */
			(e1, s) -> {
				List<FruP1TetherEvent> tethers = new ArrayList<>();
				s.setParam("events", tethers);
				s.updateCall(fourTetherColl1, e1);
				tethers.add(e1);

				FruP1TetherEvent e2 = s.waitEvent(FruP1TetherEvent.class, te -> te.cast.abilityIdMatches(0x9CC9, 0x9CCC));
				tethers.add(e2);
				s.updateCall(fourTetherColl2, e2);

				FruP1TetherEvent e3 = s.waitEvent(FruP1TetherEvent.class, te -> te.cast.abilityIdMatches(0x9CC9, 0x9CCC));
				tethers.add(e3);
				s.updateCall(fourTetherColl3, e3);

				FruP1TetherEvent e4 = s.waitEvent(FruP1TetherEvent.class, te -> te.cast.abilityIdMatches(0x9CC9, 0x9CCC));
				tethers.add(e4);
				s.updateCall(fourTetherColl4, e4);

//				s.waitMs(1_000);

				// Call the first one soon, but for the rest, wait until the previous tether goes off
				s.updateCall(fourTetherResolving1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.updateCall(fourTetherResolving2);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.updateCall(fourTetherResolving3);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.updateCall(fourTetherResolving4);

				// AM Idea: Use binds for lightning, ignores for fire to match the colors
			});

	private final ModifiableCallout<AbilityCastStart> towers = ModifiableCallout.durationBasedCall("Towers: Soak (Non Tank)", "Soak Tower");
	private final ModifiableCallout<AbilityCastStart> towersTank = ModifiableCallout.durationBasedCall("Towers: Don't Soak (Tank)", "Away From Towers");

	private final ModifiableCallout<AbilityCastStart> towersKB = ModifiableCallout.durationBasedCall("Towers: Soak (Non Tank + KB)", "Knockback Into Tower");
	private final ModifiableCallout<AbilityCastStart> towersTankKB = ModifiableCallout.durationBasedCall("Towers: Don't Soak (Tank + KB)", "Knockback Away From Towers");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> towersSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CBA, 0x9CBB, 0x9CBC, 0x9CBD, 0x9CBE, 0x9CBF, 0x9CC3, 0x9CC7),
			(e1, s) -> {
				s.waitMs(1_000);
				boolean isKB = casts.getActiveCastById(0x9CC2).isPresent();
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(isKB ? towersTankKB : towersTank, e1);
				}
				else {
					s.updateCall(isKB ? towersKB : towers, e1);
				}
				// For identifying towers for an eventual prio:
				// 9CC7 - single
				// 9CBE - double
				// 9CBD - triple
				// 9CBF - quad
			}
	);

	@NpcCastCallout(value = 0x9CC0, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> p1enrage = ModifiableCallout.durationBasedCall("P1 Enrage", "Enrage");

	/*
	P2:
	Quadruple slap - buster
	Diamond Dust - raidwide
	Call headmarker or no HM
	axe kick/scythe (0x9DOB) in there as well
	 */

	@NpcCastCallout(0x9CFF)
	private final ModifiableCallout<AbilityCastStart> quadrupleSlap = ModifiableCallout.durationBasedCall("Quadruple Slap", "Buster on {event.target}");
	@NpcCastCallout(0x9D05)
	private final ModifiableCallout<AbilityCastStart> diamondDust = ModifiableCallout.durationBasedCall("Diamond Dust", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> ddAxeWithMarker = ModifiableCallout.<AbilityCastStart>durationBasedCall("DD: Axe Kick with Marker", "Out with Marker, {firstIces} Safe")
			.extendedDescription("""
					{firstIces} is a list of where the first ices are dropping. You can also use the boolean {cardinal} to determine whether \
					cardinals are puddles, e.g. {cardinal ? 'No Swap' : 'Swap'"}.""");
	private final ModifiableCallout<AbilityCastStart> ddAxeNoMarker = ModifiableCallout.durationBasedCall("DD: Axe Kick, no Marker", "Out, Bait, {firstIces} Safe");
	private final ModifiableCallout<AbilityCastStart> ddScytheWithMarker = ModifiableCallout.durationBasedCall("DD: Scythe Kick with Marker", "In with Marker, {firstIces} Safe");
	private final ModifiableCallout<AbilityCastStart> ddScytheNoMarker = ModifiableCallout.durationBasedCall("DD: Scythe Kick, no Marker", "In, Bait, {firstIces} Safe");
	private final ModifiableCallout<?> ddDropPuddle = new ModifiableCallout<>("DD: Drop Puddle", "Drop Puddle");
	private final ModifiableCallout<?> ddAvoidPuddle = new ModifiableCallout<>("DD: Avoid Puddles", "Avoid Puddles");
	private final ModifiableCallout<?> ddKB = new ModifiableCallout<>("DD: KB", "Knockback to {firstIces}");
	private final ModifiableCallout<?> ddKBimmune = new ModifiableCallout<>("DD: KB Immune", "Knockback Immunity");
	private final ModifiableCallout<?> ddStacks = new ModifiableCallout<>("DD: Stacks", "Multiple Stacks, Keep Moving");
	private final ModifiableCallout<?> ddGaze = new ModifiableCallout<>("DD: Gaze", "Look Away from {gazeFrom}");

	private final ModifiableCallout<AbilityCastStart> scytheMirrors1 = ModifiableCallout.durationBasedCall("Scythe Mirrors 1", "Blue Mirror and Boss, In+Proteans");
	private final ModifiableCallout<AbilityCastStart> scytheMirrors2 = ModifiableCallout.durationBasedCall("Scythe Mirrors 2", "Red Mirrors, In+Proteans");
	@NpcCastCallout(0x9D1C)
	private final ModifiableCallout<AbilityCastStart> banishBuddies = ModifiableCallout.durationBasedCall("Banish III (Buddies)", "Buddies");
	@NpcCastCallout(0x9D1D)
	private final ModifiableCallout<AbilityCastStart> banishSpread = ModifiableCallout.durationBasedCall("Banish III (Spread)", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> axeScythe = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D0A, 0x9D0B),
			(e1, s) -> {
				// Axe kick = out
				boolean isAxeKick = e1.abilityIdMatches(0x9D0A);
				// It's the next four HMs, so it should be fine to just look for four
				List<HeadMarkerEvent> headmarkers = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> true);
				s.waitThenRefreshCombatants(100);
				@Nullable HeadMarkerEvent playerMarker = headmarkers.stream()
						.filter(hme -> hme.getTarget().isThePlayer())
						.findFirst()
						.orElse(null);
				// Find first pair of Icycle Impact
				List<ArenaSector> firstIces = casts.getActiveCastsById(0x9D06).stream()
						.map(CastTracker::getCast)
						.map(AbilityCastStart::getSource)
						.map(npc -> arenaPos.forCombatant(state.getLatestCombatantData(npc)))
						.sorted(ArenaSector.northCcwSort)
						.toList();
				if (firstIces.size() != 2) {
					log.error("firstIces size {}, expected 2! Data: {}", firstIces.size(), firstIces);
				}
				s.setParam("firstIces", firstIces);
				s.setParam("cardinal", firstIces.get(0).isCardinal());

				boolean playerHasMarker = playerMarker != null;
				if (isAxeKick) {
					// out
					s.updateCall(playerHasMarker ? ddAxeWithMarker : ddAxeNoMarker, e1);
				}
				else {
					// in
					s.updateCall(playerHasMarker ? ddScytheWithMarker : ddScytheNoMarker, e1);
				}
				s.waitCastFinished(casts, e1);
				// Add a slight delay since things don't go off instantly
				s.waitMs(300);
				// Drop or avoid puddle
				s.updateCall(playerHasMarker ? ddDropPuddle : ddAvoidPuddle);
				var icycleCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D08));
				s.updateCall((playerHasMarker && isAxeKick) ? ddKBimmune : ddKB);
				s.waitCastFinished(casts, icycleCast);
				// TODO: CW vs CCW rotation
				s.updateCall(ddStacks);
				s.waitMs(4_000);
				XivCombatant gazeNpc = state.npcById(17823);
				if (gazeNpc != null) {
					ArenaSector gazeFrom = arenaPos.forCombatant(gazeNpc);
					s.setParam("gazeFrom", gazeFrom);
				}
				s.updateCall(ddGaze);
			}, (e1, s) -> {
				// Mirrors
				s.updateCall(scytheMirrors1, e1);
				var reflectedCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9D0D), false);
				s.updateCall(scytheMirrors2, reflectedCast);
			});

	@NpcCastCallout(0x9D01)
	private final ModifiableCallout<AbilityCastStart> twinStillness = ModifiableCallout.durationBasedCall("Twin Stillness", "Back to Front");
	@NpcCastCallout(0x9D02)
	private final ModifiableCallout<AbilityCastStart> twinSilence = ModifiableCallout.durationBasedCall("Twin Silence", "Front to Back");

	@NpcCastCallout(0x9D12)
	private final ModifiableCallout<AbilityCastStart> hallowedRay = ModifiableCallout.durationBasedCall("Hallowed Ray", "Line Stack");

	private final ModifiableCallout<AbilityCastStart> lrInitial = ModifiableCallout.durationBasedCall("Light Rampant: Initial", "Light Rampant Positions");

	private final ModifiableCallout<?> lrChainNoStack = new ModifiableCallout<>("Light Rampant: Chain, No Weight Debuff", "Chain").statusIcon(0x103D);
	private final ModifiableCallout<?> lrChainWithStack = new ModifiableCallout<>("Light Rampant: Chain + Weight Debuff", "Chain").statusIcon(0x103F);
	private final ModifiableCallout<?> lrHeadmarker = new ModifiableCallout<>("Light Rampant: Puddle", "Puddle");
	private final ModifiableCallout<?> lrCollapse = new ModifiableCallout<>("Light Rampant: After First Towers", "Stacks");
	private final ModifiableCallout<?> lrGoInTower = new ModifiableCallout<>("Light Rampant: Take Final Tower", "Take Tower");
	private final ModifiableCallout<?> lrAvoidTower = new ModifiableCallout<>("Light Rampant: Avoid Final Tower", "Avoid Tower");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> lightRampant = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D14),
			(e1, s) -> {
				// Tether is 6E
				// Also a headmarker and debuffs
				// Chains of Everlasting Light 103D
				// The Weight of Light 103F
				// Lightspeed (stacks) 8D1

				// Gotta move around
				// 2-stacks take final tower
				log.info("Light Rampant: Start");
				s.updateCall(lrInitial, e1);
				// TODO: would be nice to have something like collectEvents() but supporting different types of events
				List<HeadMarkerEvent> markers = s.waitEvents(2, HeadMarkerEvent.class, (e) -> true);
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(6, TetherEvent.class, (e) -> e.tetherIdMatches(0x6E), Duration.ofMillis(100));
				s.waitMs(100);
				@Nullable BuffApplied playerStackDebuff = buffs.findBuff(ba -> ba.buffIdMatches(0x103F) && ba.getTarget().isThePlayer());
				/*
				Possibilities:
				Golden Orb (The Weight of Light 103F) will always go on tether players
				i.e.:
				4x tether, no orb
				2x tether, with orb
				2x puddles

				Luminous Hammer is the puddle drops
				Bright Hunger is the tower hit
				*/
				// TODO: this should have the tether order. Might be a challenge since there isn't really a natural ordering.
				// i.e. what if people aren't standing in a circle? Who is "#1"?
				boolean playerHasTether = tethers.stream().anyMatch(te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				boolean playerHasMarker = markers.stream().anyMatch(hm -> hm.getTarget().isThePlayer());
				if (playerHasTether) {
					if (playerStackDebuff != null) {
						// Player has tether + stacks
						s.updateCall(lrChainWithStack);
					}
					else {
						// Player has tether, no stacks
						s.updateCall(lrChainNoStack);
					}
				}
				else if (playerHasMarker) {
					// Player has headmarker
					s.updateCall(lrHeadmarker);
				}
				else {
					// ???
					log.error("Player has neither tether nor marker, giving up.\nTethers: {}\nMarkers: {}", tethers, markers);
				}
				// Towers going off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D15));
				s.updateCall(lrCollapse);
				// First bursts going off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D1B));
				int playerStacks = buffs.buffStacksOnTarget(state.getPlayer(), 0x8D1);
				if (playerStacks <= 2) {
					s.updateCall(lrGoInTower);
				}
				else {
					s.updateCall(lrAvoidTower);
				}
			});

	@NpcCastCallout(0x9CFD)
	private final ModifiableCallout<AbilityCastStart> houseOfLight = ModifiableCallout.durationBasedCall("The House of Light", "Proteans");
	@NpcCastCallout(0x9D20)
	private final ModifiableCallout<AbilityCastStart> p2enrage = ModifiableCallout.durationBasedCall("P2 Enrage", "Enrage, Knockback");

	@NpcCastCallout(0x9D43)
	private final ModifiableCallout<AbilityCastStart> intermissionEnrage = ModifiableCallout.durationBasedCall("Endless Ice Age (Intermission)", "Kill Crystals, Bait AoEs");
}



