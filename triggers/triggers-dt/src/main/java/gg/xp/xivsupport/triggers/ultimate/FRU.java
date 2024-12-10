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
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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
	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);
	private final ArenaPos arenaPosNarrow = new ArenaPos(100, 100, 5, 5);

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
	private final ModifiableCallout<AbilityCastStart> turnKB = ModifiableCallout.durationBasedCall("Turn of the Heavens: Knockback Cast (No Tether)", "Get Knocked {safe}");
	private final ModifiableCallout<AbilityCastStart> turnKBclose = ModifiableCallout.durationBasedCall("Turn of the Heavens: Knockback Cast (With Close Tether)", "Get Knocked {safe}, Close Tether");
	private final ModifiableCallout<AbilityCastStart> turnKBfar = ModifiableCallout.durationBasedCall("Turn of the Heavens: Knockback Cast (With Far Tether)", "Get Knocked {safe}, Far Tether");
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
				List<FruP1TetherEvent> tetherEvents = s.waitEvents(2, FruP1TetherEvent.class, fp -> true);
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

				tetherEvents.stream().filter(te -> te.getTarget().isThePlayer()).findFirst().ifPresentOrElse(te -> {
					ArenaSector tetherSector = arenaPos.forCombatant(state.getLatestCombatantData(te.source));
					// Check if tether is close or far
					if (tetherSector.isStrictlyAdjacentTo(safe)) {
						s.updateCall(turnKBclose, burnout);
					}
					else if (tetherSector.isStrictlyAdjacentTo(safe.opposite())) {
						s.updateCall(turnKBfar, burnout);
					}
					else {
						log.error("Unknown tether configuration");
						// Fallback
						s.updateCall(turnKB, burnout);
					}
				}, () -> {
					s.updateCall(turnKB, burnout);
				});
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

		@SuppressWarnings("unused")
		public boolean isLightning() {
			return mechType == MechType.Lightning;
		}

		@SuppressWarnings("unused")
		public boolean isFire() {
			return mechType == MechType.Fire;
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

	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl1 = makeTetherCall("First Tether Out", 0, 0, true)
			.extendedDescription("""
					For these callouts, `events` is a list of tethers. In addition to `.target` and `.mechType`, you can also use `.fire` and `.lightning` booleans to\
					directly check if one is fire or lightning, e.g. {events[0].fire ? 'F' : 'L'} to call 'F' or 'L'.""");
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl2 = makeTetherCall("Second Tether Out", 0, 1, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl3 = makeTetherCall("Third Tether Out", 0, 2, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl4 = makeTetherCall("Fourth Tether Out", 0, 3, false);

	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving1 = makeTetherCall("First Tether Resolving", 0, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving2 = makeTetherCall("Second Tether Resolving", 1, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving3 = makeTetherCall("Third Tether Resolving", 2, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving4 = makeTetherCall("Fourth Tether Resolving", 3, 3, true);

	// This does not overlap IDs with the 2x tether mechanic
	@AutoFeed
	private final SequentialTrigger<BaseEvent> fourTethers = SqtTemplates.sq(30_000,
			FruP1TetherEvent.class, te -> te.cast.abilityIdMatches(0x9CC9, 0x9CCC),
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

				s.waitMs(1_000);

				// Call the first one soon, but for the rest, wait until the previous tether goes off
				s.updateCall(fourTetherResolving1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.waitMs(1_000);
				s.updateCall(fourTetherResolving2);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.waitMs(1_000);
				s.updateCall(fourTetherResolving3);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
				s.waitMs(1_000);
				s.updateCall(fourTetherResolving4);

				// AM Idea: Use binds for lightning, ignores for fire to match the colors
			});

	private final ModifiableCallout<AbilityCastStart> towers = ModifiableCallout.durationBasedCall("Towers: Soak (Non Tank)", "Soak Tower");
	private final ModifiableCallout<AbilityCastStart> towersTank = ModifiableCallout.durationBasedCall("Towers: Don't Soak (Tank)", "Away From Towers");

	private final ModifiableCallout<AbilityCastStart> towersKB = ModifiableCallout.durationBasedCall("Towers: Soak (Non Tank + KB)", "Knockback Into Tower");
	private final ModifiableCallout<AbilityCastStart> towersTankKB = ModifiableCallout.durationBasedCall("Towers: Don't Soak (Tank + KB)", "Knockback Away From Towers");
	private final ModifiableCallout<?> towersKBmoveIn = new ModifiableCallout<>("Towers: Move in for KB", "Move In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> towersSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CBA, 0x9CBB, 0x9CBC, 0x9CBD, 0x9CBE, 0x9CBF, 0x9CC3, 0x9CC7),
			(e1, s) -> {
				s.waitMs(1_000);
				boolean isKB = casts.getActiveCastById(0x9CC2).isPresent();
				boolean isTank = state.playerJobMatches(Job::isTank);
				if (isKB) {
					if (isTank) {
						s.updateCall(towersTankKB, e1);
					}
					else {
						s.updateCall(towersKB, e1);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CC1));
					s.updateCall(towersKBmoveIn);
				}
				else {
					if (isTank) {
						s.updateCall(towersTank, e1);
					}
					else {
						s.updateCall(towers, e1);
					}

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
					{firstIces} is a list of where the first ices are dropping. You can also use the boolean `cardinal` to determine whether \
					cardinals are puddles, e.g. {cardinal ? 'No Swap' : 'Swap'}.""");
	private final ModifiableCallout<AbilityCastStart> ddAxeNoMarker = ModifiableCallout.durationBasedCall("DD: Axe Kick, no Marker", "Out, Bait, {firstIces} Safe");
	private final ModifiableCallout<AbilityCastStart> ddScytheWithMarker = ModifiableCallout.durationBasedCall("DD: Scythe Kick with Marker", "In with Marker, {firstIces} Safe");
	private final ModifiableCallout<AbilityCastStart> ddScytheNoMarker = ModifiableCallout.durationBasedCall("DD: Scythe Kick, no Marker", "In, Bait, {firstIces} Safe");
	//	private final ModifiableCallout<?> ddDropPuddle = new ModifiableCallout<>("DD: Drop Puddle", "Drop Puddle");
//	private final ModifiableCallout<?> ddAvoidPuddle = new ModifiableCallout<>("DD: Avoid Puddles", "Avoid Puddles");
	private final ModifiableCallout<?> ddAxeDropPuddle = new ModifiableCallout<>("DD: Drop Puddle", "Stay, Drop Puddle");
	private final ModifiableCallout<?> ddScytheDropPuddle = new ModifiableCallout<>("DD: Drop Puddle", "Out, Drop Puddle");
	private final ModifiableCallout<?> ddKB = new ModifiableCallout<>("DD: KB after Scythe", "In, Knockback to {firstIces}");
	private final ModifiableCallout<?> ddKbAxe = new ModifiableCallout<>("DD: KB after Axe", "In, Knockback to {firstIces}");
	// TODO: call out which way to go
	private final ModifiableCallout<?> ddStacks = new ModifiableCallout<>("DD: Stacks", "Multiple Stacks, Keep Moving")
			.extendedDescription("""
					The `{shivaAt}` variable indicates the location of the boss (not the gaze).""");
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
					// wait for axe to finish
					s.waitCastFinished(casts, e1);
					if (playerHasMarker) {
						// Stay out, drop
						s.updateCall(ddAxeDropPuddle);
						// puddle drop
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D07));
						// move in for kb
						s.updateCall(ddKbAxe);
					}
					else {
						// If you're baiting, and it's axe, you can move in as soon as axe goes off, since you won't mess up the bait
						// by moving directly in
						s.updateCall(ddKbAxe);
					}
				}
				else {
					// in
					s.updateCall(playerHasMarker ? ddScytheWithMarker : ddScytheNoMarker, e1);
					// wait for scythe to finish
					s.waitCastFinished(casts, e1);
					if (playerHasMarker) {
						// If it's scythe, and you have a marker, you can move out as soon as scythe goes off
						s.updateCall(ddScytheDropPuddle);
						// puddle drop
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D07));
						// move in for kb
						s.updateCall(ddKB);
					}
					else {
						// If you're baiting, wait for the bait to snapshot, then move in for kb
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D0E));
						s.updateCall(ddKB);
					}
				}
				var icycleCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9D08), false);
				s.waitCastFinished(casts, icycleCast);
				// TODO: CW vs CCW rotation - there are different strats for this
				casts.getActiveCastById(0x9D10)
						.ifPresent(ct -> {
							XivCombatant shiva = state.getLatestCombatantData(state.getLatestCombatantData(ct.getCast().getSource()));
							log.info("shivaAt: {}", shiva.getPos());
							s.setParam("shivaAt", arenaPosNarrow.forCombatant(shiva));
						});
				s.updateCall(ddStacks);
				s.waitMs(4_000);
				XivCombatant gazeNpc = state.npcById(17823);
				if (gazeNpc != null) {
					ArenaSector gazeFrom = arenaPos.forCombatant(gazeNpc);
					s.setParam("gazeFrom", gazeFrom);
				}
				// TODO: call look in vs out
				s.updateCall(ddGaze);
			}, (e1, s) -> {
				// Mirrors
				s.updateCall(scytheMirrors1, e1);
				var reflectedCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9D0D), false);
				s.updateCall(scytheMirrors2, reflectedCast);
			});

	private final ModifiableCallout<AbilityCastStart> twinStillness = ModifiableCallout.durationBasedCall("Twin Stillness", "Back to Front");
	private final ModifiableCallout<?> twinStillnessMove = new ModifiableCallout<>("Twin Stillness: Move", "Front");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twinSillnessSq = SqtTemplates.beginningAndEndingOfCast(
			acs -> acs.abilityIdMatches(0x9D01),
			twinStillness,
			twinStillnessMove);

	private final ModifiableCallout<AbilityCastStart> twinSilence = ModifiableCallout.durationBasedCall("Twin Silence", "Front to Back");
	private final ModifiableCallout<?> twinSilenceMove = new ModifiableCallout<>("Twin Silence: Move", "Back");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twinSilenceSq = SqtTemplates.beginningAndEndingOfCast(
			acs -> acs.abilityIdMatches(0x9D02),
			twinSilence,
			twinSilenceMove);

	@NpcCastCallout(0x9D12)
	private final ModifiableCallout<AbilityCastStart> hallowedRay = ModifiableCallout.durationBasedCall("Hallowed Ray", "Line Stack");

	private final ModifiableCallout<AbilityCastStart> lrInitial = ModifiableCallout.durationBasedCall("Light Rampant: Initial", "Raidwide, Light Rampant Positions");

	private final ModifiableCallout<?> lrChainNoStack = new ModifiableCallout<>("Light Rampant: Chain, No Weight Debuff", "Chain").statusIcon(0x103D);
	private final ModifiableCallout<?> lrChainWithStack = new ModifiableCallout<>("Light Rampant: Chain + Weight Debuff", "Chain and Stack").statusIcon(0x103F);
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

	// TODO: there is a raidwide (Junction 9D22) between intermission and p3

	@NpcCastCallout(0x9D49)
	private final ModifiableCallout<AbilityCastStart> hellsJudgment = ModifiableCallout.durationBasedCall("Hell's Judgment", "1 HP");

	private final ModifiableCallout<AbilityCastStart> ultimateRelativityInit = ModifiableCallout.durationBasedCall("Ultimate Relativity: Initial", "Raidwide");

	private static Predicate<HasDuration> initDurLessThan(int seconds) {
		return ba -> ba.getInitialDuration().toSeconds() < seconds;
	}

	private static Predicate<HasDuration> initDurGreaterThan(int seconds) {
		return ba -> ba.getInitialDuration().toSeconds() > seconds;
	}

	private static Predicate<HasDuration> initDurBetween(int secondsMin, int secondsMax) {
		return ba -> {
			long initDur = ba.getInitialDuration().toSeconds();
			return initDur >= secondsMin && initDur <= secondsMax;
		};
	}

	private final ModifiableCallout<?> ultimateRelativityRelNorth = new ModifiableCallout<>("Ultimate Relativity: Relative North", "{relNorth} is North")
			.extendedDescription("Calls which direction is 'north' for the purposes of the mechanic. Assumes normal Y configuration. To invert, use {relNorth.opposite()}.");
	private final ModifiableCallout<BuffApplied> relDpsShortFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Short Fire", "Short Fire").autoIcon()
			.extendedDescription("Note that these triggers are designed for the 'Macroless Y Runytivity' strategy.");
	private final ModifiableCallout<BuffApplied> relDpsMediumFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Medium Fire", "Medium Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> relDpsLongFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Long Fire", "Long Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> relDpsIce = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Ice", "Ice").autoIcon();
	private final ModifiableCallout<BuffApplied> relThShortFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Support Short Fire", "Short Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> relThMediumFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Support Medium Fire", "Medium Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> relThLongFire = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Support Long Fire", "Long Fire").autoIcon();
	private final ModifiableCallout<BuffApplied> relThIce = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Support Ice", "Ice").autoIcon();

	private final ModifiableCallout<BuffApplied> relShortFirePop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Short Fire Popping", "Move Out").autoIcon()
			.extendedDescription("The short fire/stack pop calls happen about 5 seconds in.");
	private final ModifiableCallout<BuffApplied> relShortStackPop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Short Stack Popping", "Stack").autoIcon();

	private final ModifiableCallout<?> relLongRewind = new ModifiableCallout<>("Relativity: Long Rewind", "Bait Spinny")
			.extendedDescription("This call happens after the first fire/stack pop, if you have long rewind.");
	private final ModifiableCallout<BuffApplied> relShortRewindEruption = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Short Rewind w/ Eruption", "Stand on Light").autoIcon()
			.extendedDescription("This call happens after the first fire/stack pop, if you have short rewind and have eruption (no water).");
	private final ModifiableCallout<BuffApplied> relShortRewindWater = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: Short Rewind w/ Water", "Stand on Light").autoIcon()
			.extendedDescription("This call happens after the first fire/stack pop, if you have short rewind and have water (no eruption).");

	private final ModifiableCallout<BuffApplied> relMedFirePop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Medium Fire Popping", "Move Out").autoIcon()
			.extendedDescription("The medium fire/stack pop calls happen about 15 seconds in.");
	private final ModifiableCallout<BuffApplied> relMedStackPop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Medium Stack Popping", "Stack").autoIcon();

	private final ModifiableCallout<?> relShortRewindBait = new ModifiableCallout<>("Relativity: Short Rewind Part 2", "Bait Spinny")
			.extendedDescription("This call happens after the second fire/stack pop, if you have short rewind and do not have medium fire.");
	private final ModifiableCallout<?> relShortRewindMedFire = new ModifiableCallout<>("Relativity: Short Rewind Part 2 (Med Fire)", "AFK")
			.extendedDescription("This call happens after the second fire/stack pop, if you have short rewind and had medium fire.");
	private final ModifiableCallout<?> relLongRewind2 = new ModifiableCallout<>("Relativity: Long Rewind Part 2", "Stand Middle")
			.extendedDescription("This call happens after the first fire/stack pop, if you have long rewind.");

	private final ModifiableCallout<BuffApplied> relLongFirePop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Long Fire Popping", "Move Out").autoIcon()
			.extendedDescription("The long fire/stack pop calls happen about 25 seconds in.");
	private final ModifiableCallout<BuffApplied> relLongStackPop = ModifiableCallout.<BuffApplied>durationBasedCall("Relativity: DPS Long Stack Popping", "Stack").autoIcon();

	private final ModifiableCallout<?> relMedFireBaitLookOut = new ModifiableCallout<>("Relativity: Medium Fire Final Baits", "Bait Light, Look Outside").statusIcon(0x998)
			.extendedDescription("Final Traffic Light Bait, with Medium Fire");
	private final ModifiableCallout<?> relFinalCenterLookOut = new ModifiableCallout<>("Relativity: Final Mechanics", "Look Outside").statusIcon(0x998)
			.extendedDescription("Final Traffic Light Bait, not Medium Fire");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ultimateRelativityTetherSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D4A),
			(e1, s) -> {
				// It *seems* like the "north" is always the first tether, but this relies on less assumptions.
				log.info("ultimateRelativityTetherSq: start");
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(3, TetherEvent.class, te -> te.tetherIdMatches(0x86));
				log.info("ultimateRelativityTetherSq: got tethers");
				s.waitThenRefreshCombatants(100);
				List<ArenaSector> sectors = tethers.stream().map(te -> {
					XivCombatant target = te.getTargetMatching(cbt -> cbt.npcIdMatches(17832));
					if (target == null) {
						throw new IllegalStateException("Bad tether: %s".formatted(te));
					}
					return arenaPosNarrow.forCombatant(state.getLatestCombatantData(target));
				}).toList();
				log.info("ultimateRelativityTetherSq: sectors {}", sectors);
				ArenaSector fakeNorth = null;
				outer:
				for (ArenaSector sector : sectors) {
					// What we need to do is check that for each sector, every other sector in the list is either +3/8 of a circle, or -3/8,
					// or is the same tether. If that is true, then this is the middle tether.
					for (ArenaSector otherSector : sectors) {
						int delta = sector.eighthsTo(otherSector);
						if (!(delta == 0 || delta == 3 || delta == -3)) {
							// Try next one
							continue outer;
						}
					}
					fakeNorth = sector.opposite();
				}
				if (fakeNorth == null) {
					log.error("ultiamteRelativityTetherSq: Could not figure out tether arrangement! Sectors: {}", sectors);
					return;
				}
				s.setParam("relNorth", fakeNorth);
				s.updateCall(ultimateRelativityRelNorth);
				log.info("ultimateRelativityTetherSq: end");
			});

	private static final Predicate<BuffApplied> isPlayer = ba -> ba.getTarget().isThePlayer();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ultimateRelativity = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D4A),
			(e1, s) -> {
				s.updateCall(ultimateRelativityInit, e1);
				/*
				Debuff vomit:
				7 players get fire 997, either 11s, 21s, or 31s
					Defamation
				Player without fire gets blizzard 99E 21s
					Donut
				One player gets water 99D 43s
					4-person stack
				All 8 get return 9A0, some get 16s while others get 26s (seems to be 5-3 split?)
					Returns you like in e12s
				3 get unholy darkness 996, 11/21/31s (one each)
					Stack
				4 get eruption 99C 43s
					Spread
				3 get gaze 998 43s

				*/
				boolean isDps = state.playerJobMatches(Job::isDps);
				s.waitCastFinished(casts, e1);
				Predicate<BuffApplied> iceCond = ba -> ba.buffIdMatches(0x99E);
				Predicate<BuffApplied> fireCond = ba -> ba.buffIdMatches(0x997);
				Predicate<BuffApplied> returnCond = ba -> ba.buffIdMatches(0x9A0);
				Predicate<BuffApplied> darknessCond = ba -> ba.buffIdMatches(0x996);
				Predicate<BuffApplied> waterCond = ba -> ba.buffIdMatches(0x99D);
				Predicate<BuffApplied> eruptionCond = ba -> ba.buffIdMatches(0x99C);
				Predicate<BuffApplied> gazeCond = ba -> ba.buffIdMatches(0x998);

				var shortCond = initDurLessThan(17);
				var medCond = initDurBetween(17, 27);
				var longCond = initDurGreaterThan(27);

				var shortFireC = new EventCollector<>(fireCond.and(shortCond));
				var medFireC = new EventCollector<>(fireCond.and(medCond));
				var longFireC = new EventCollector<>(fireCond.and(longCond));
				var iceC = new EventCollector<>(iceCond);
				var shortStackC = new EventCollector<>(darknessCond.and(shortCond));
				var medStackC = new EventCollector<>(darknessCond.and(medCond));
				var longStackC = new EventCollector<>(darknessCond.and(longCond));
				var shortRewindC = new EventCollector<>(returnCond.and(shortCond));
				var longRewindC = new EventCollector<>(returnCond.and(medCond));
				s.collectEvents(27, 1200, BuffApplied.class, true, List.of(shortFireC, longFireC, medFireC, iceC, shortStackC, medStackC, longStackC, shortRewindC, longRewindC));

				// Initial calls
				shortFireC.findAny(isPlayer).ifPresent(v -> s.updateCall(isDps ? relDpsShortFire : relThShortFire, v));
				medFireC.findAny(isPlayer).ifPresent(v -> s.updateCall(isDps ? relDpsMediumFire : relThMediumFire, v));
				longFireC.findAny(isPlayer).ifPresent(v -> s.updateCall(isDps ? relDpsLongFire : relThLongFire, v));
				iceC.findAny(isPlayer).ifPresent(v -> s.updateCall(isDps ? relDpsIce : relThIce, v));

				// Give time to move out for short fire pops/stack middle if not fire
				s.waitMs(5_000); // T=5
				shortFireC.findAny(isPlayer).ifPresentOrElse(
						e -> s.updateCall(relShortFirePop, e), () -> {
							shortStackC.findAny(e -> true).ifPresent(e -> s.updateCall(relShortStackPop, e));
						});

				// Short fires pop
				// TODO: make a real wait condition for this
				s.waitMs(6_000); // T=11
				/*
				At this point,
				if you have short rewind, you would stand on traffic light, unless you also have water, in which case you stand center
				If you have long rewind, bait spinny
				*/
				shortRewindC.findAny(isPlayer).ifPresentOrElse(
						e -> {
							BuffApplied playerWater = buffs.findStatusOnTarget(state.getPlayer(), waterCond);
							if (playerWater != null) {
								s.updateCall(relShortRewindWater, e);
							}
							else {
								s.updateCall(relShortRewindEruption, e);
							}
						},
						() -> s.updateCall(relLongRewind));

				s.waitMs(5_000); // T=16

				medFireC.findAny(isPlayer).ifPresentOrElse(
						e -> s.updateCall(relMedFirePop, e), () -> {
							medStackC.findAny(e -> true).ifPresent(e -> s.updateCall(relMedStackPop, e));
						});

				s.waitMs(5_000); // T=21

				// Now, short rewinds bait stoplight lasers
				shortRewindC.findAny(isPlayer).ifPresentOrElse(
						// Bait or do nothing
						e -> medFireC.findAny(isPlayer).ifPresentOrElse(
								ignored -> s.updateCall(relShortRewindMedFire),
								() -> s.updateCall(relShortRewindBait)),
						() -> {
							s.updateCall(relLongRewind2);
						}
				);
				s.waitMs(5_000); // T=26

				longFireC.findAny(isPlayer).ifPresentOrElse(
						e -> s.updateCall(relLongFirePop, e), () -> {
							longStackC.findAny(e -> true).ifPresent(e -> s.updateCall(relLongStackPop, e));
						});

				s.waitMs(5_000); // T=31

				medFireC.findAny(isPlayer).ifPresentOrElse(
						e -> s.updateCall(relMedFireBaitLookOut),
						() -> s.updateCall(relFinalCenterLookOut)
				);
			});


	@NpcCastCallout(0x9D5E)
	private final ModifiableCallout<AbilityCastStart> shellCrusher = ModifiableCallout.durationBasedCall("Shell Crusher", "Stack");
	@NpcCastCallout(0x9D5A)
	private final ModifiableCallout<AbilityCastStart> shockwavePulsar = ModifiableCallout.durationBasedCall("Shockwave Pulsar", "Raidwide");
	@NpcCastCallout(0x9D62)
	private final ModifiableCallout<AbilityCastStart> blackHalo = ModifiableCallout.durationBasedCall("Black Halo", "Tankbuster on {event.target}");

	/*
	https://clips.twitch.tv/BenevolentEmpathicHorseradishFunRun-cC3p-lzm7nxoAnSk
	First stacks happen before any explosions
	Then, you see lights on center and two opposite outers
	Those start going CW or CCW while more sets spawn
	For example, the pattern of hits might be (clockwise):
	Short Stacks
	Spread for Spirit Taker
	(Some time between mechs)
	Spreads in NE/SW (not the same as the spirit taker spread)
	Middle + E/W explosions
	Middle + E/W + SE/NW explosions
	(spreads resolve)
	(start running in)
	E/W + SE/NW + S/N
	(in for stacks)
	SE/NW + S/N + SW/NE
	S/N + SW/NE + W/E
	(stacks #2 resolve)
	SW/NE + W/E + NW/SE
	boss jumps
	kb
	final stacks
	*/

	/*
	Example log seems to be N/S start with CCW rotation
	Events:
	All events are ActorControlExtraEvent with 19D 4:N:0:0
	*Maybe* this also determines direction, and thus we don't need to wait as long?
	Initial:
	First, a set of four - two in the middle (N==1), two at the first hits (N/S here) (N==0x40)
	Then, a set of 8 events is 2 with x==1 for the center, one at N, one at S, as well as duplicates of those
	two, then two final ones for NW and SE (where the rotation is headed)
	There are further sets of 8 but we know what to do at this point.
	So it should be possible to determine safe spot and rotation direction.
	I'm guessing the duplicates
	 */
	// TODO: would be nice to prio this
	private final ModifiableCallout<?> apocCheckStacks = new ModifiableCallout<>("Apoc: Check Stack Timers", "Check Timers").statusIcon(0x99D);
	private final ModifiableCallout<BuffApplied> apocStacks = ModifiableCallout.<BuffApplied>durationBasedCall("Apoc: First Stacks", "Stacks").statusIcon(0x99D);
	private final ModifiableCallout<?> apocSpiritTakerSpread = new ModifiableCallout<>("Apoc: Spirit Taker", "Spread")
			.extendedDescription("""
					This call and calls after it can make use of the variables `clockwise` (boolean), `firstHits` (list of the first hits), \
					`initialSafeSpots (the safe spots for the eruption spreads), and `finalSafeSpots` (the safe spots for a to take the jump).""");
	private final ModifiableCallout<?> apocEruptionSpread = new ModifiableCallout<>("Apoc: Eruption", "Spread {initialSafeSpots}");
	private final ModifiableCallout<BuffApplied> apocStacks2moveIn = ModifiableCallout.<BuffApplied>durationBasedCall("Apoc: Second Stacks", "Move in for Stacks").statusIcon(0x99D);
	private final ModifiableCallout<BuffApplied> apocStacks2followup = ModifiableCallout.<BuffApplied>durationBasedCall("Apoc: Second Stacks", "Stacks then Tank Bait {finalSafeSpots}").statusIcon(0x99D);
	private final ModifiableCallout<?> apocDarkestDance = new ModifiableCallout<>("Apoc: Darkest Dance", "Tank Bait {finalSafeSpots}");
	private final ModifiableCallout<BuffApplied> apocStacks3 = ModifiableCallout.<BuffApplied>durationBasedCall("Apoc: Third Stacks", "Knockback into Stacks").statusIcon(0x99D);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> apocSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D4E),
			(e1, s) -> {
				// Collect 6 "Spell-in-Waiting: Dark Water III" debuffs
				var stackBuffs = s.waitEventsQuickSuccession(6, BuffApplied.class, ba -> ba.buffIdMatches(0x99D));
				s.updateCall(apocCheckStacks);
				s.waitMs(5_000);
				stackBuffs.stream().filter(initDurLessThan(15)).findFirst().ifPresent(
						e -> s.updateCall(apocStacks, e)
				);
				var ade = s.waitEvent(ApocDirectionsEvent.class);
				s.setParam("clockwise", ade.isClockwise());
				s.setParam("firstHits", ade.getFirstHits());
				s.setParam("initialSafeSpots", ade.getInitialSafeSpots());
				s.setParam("finalSafeSpots", ade.getFinalSafeSpots());
				// Stacks resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D4F));
				s.updateCall(apocSpiritTakerSpread);

				// Spirit taker resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D61));
				s.updateCall(apocEruptionSpread);

				// Eruption resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D52));

				stackBuffs.stream().filter(initDurBetween(15, 35)).findFirst().ifPresent(
						e -> {
							s.updateCall(apocStacks2moveIn, e);
							s.waitMs(2_000);
							s.updateCall(apocStacks2followup, e);
						}
				);

				// Stacks resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D4F));
				s.updateCall(apocDarkestDance);

				// Darkest Dance resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CF5));
				stackBuffs.stream().filter(initDurGreaterThan(35)).findFirst().ifPresent(
						e -> s.updateCall(apocStacks3, e)
				);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> apocDirectionColl = SqtTemplates.sq(60_000,
			// Start on Dark Water III
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D4E),
			(e1, s) -> {
				// It appears that we can use the ACEE data1 attribute to directly determine direction.
				// 0x1 = center, 0x10 = CW, 0x40 = CCW?
				// We can also add the safe spot as a parameter to the first "Spread" call for people who want to know it earlier
				Predicate<ActorControlExtraEvent> filter = e -> e.getCategory() == 0x19D && e.getData0() == 0x4
				                                                && (e.getData1() == 0x10 || e.getData1() == 0x40);
				// This is the center + first explosion. 0x10 == clockwise, 0x40 == ccw.
				List<ActorControlExtraEvent> events = s.waitEventsQuickSuccession(2, ActorControlExtraEvent.class, filter);
				List<ArenaSector> sectors = events.stream()
						.map(e -> arenaPos.forCombatant(state.getLatestCombatantData(e.getTarget())))
						.sorted(ArenaSector.northCcwSort)
						.toList();
				boolean clockwise = events.get(0).getData1() == 0x10;
				ApocDirectionsEvent directions = new ApocDirectionsEvent(sectors, clockwise);
				s.accept(directions);
			});


	public static final class ApocDirectionsEvent extends BaseEvent {
		@Serial
		private static final long serialVersionUID = 5880287039218528050L;
		private final List<ArenaSector> firstHits;
		private final List<ArenaSector> initialSafeSpots;
		private final List<ArenaSector> finalSafeSpots;
		private final boolean clockwise;

		private ApocDirectionsEvent(List<ArenaSector> firstHits, boolean clockwise) {
			this.firstHits = firstHits;
			this.clockwise = clockwise;
			// For clockwise rotation, we need to start 1/8 CCW from the initial hits, and vice-versa
			int delta = clockwise ? -1 : 1;
			initialSafeSpots = firstHits.stream().map(hit -> hit.plusEighths(delta)).toList();
			// Final safe spots are always "opposite" initial hits, e.g. N/S becomes E/W
			finalSafeSpots = firstHits.stream().map(hit -> hit.plusQuads(1)).toList();
		}

		public List<ArenaSector> getFirstHits() {
			return Collections.unmodifiableList(firstHits);
		}

		public boolean isClockwise() {
			return clockwise;
		}

		public List<ArenaSector> getInitialSafeSpots() {
			return initialSafeSpots;
		}

		public List<ArenaSector> getFinalSafeSpots() {
			return finalSafeSpots;
		}
	}

	@NpcCastCallout(value = 0x9D6C, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> p3enrage = ModifiableCallout.durationBasedCall("P3 Enrage", "Enrage");

	private final ModifiableCallout<?> p4stack = new ModifiableCallout<>("P4 Start: Stack", "Stack");
	private final ModifiableCallout<AbilityCastStart> p4dodge = ModifiableCallout.durationBasedCall("P4 Start: Dodge", "Dodge");
	private final ModifiableCallout<AbilityCastStart> p4startDmg = ModifiableCallout.durationBasedCall("P4 Start: Damage", "Raidwide");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p4start = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D36),
			(e1, s) -> {
				s.waitMs(8_000);
				s.updateCall(p4stack);
				var edgeCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CEE));
				var akhRhaiCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D2D));
				s.updateCall(p4dodge, akhRhaiCast);
				s.waitCastFinished(casts, akhRhaiCast);
				s.updateCall(p4startDmg, edgeCast);
			});

	private final ModifiableCallout<AbilityCastStart> darklitRaidwide = ModifiableCallout.durationBasedCall("Darklit Dragonsong: Initial", "Raidwide");

	private final ModifiableCallout<BuffApplied> darklitTetherStack = ModifiableCallout.durationBasedCall("Darklit Dragonsong: Stack + Tether", "Tether and Stack");
	private final ModifiableCallout<BuffApplied> darklitStack = ModifiableCallout.durationBasedCall("Darklit Dragonsong: Stack, No Tether", "Stack");
	private final ModifiableCallout<?> darklitTether = new ModifiableCallout<>("Darklit Dragonsong: Tether, No Stack", "Tether");
	private final ModifiableCallout<?> darklitNothing = new ModifiableCallout<>("Darklit Dragonsong: Neither Tether nor Stack", "Nothing");

	private final ModifiableCallout<AbilityCastStart> darklitTowerWithTether = ModifiableCallout.<AbilityCastStart>durationBasedCall("Darklit Dragonsong: Tower with Tether", "Soak Tower")
			.extendedDescription("""
					This call and the one below are set up for the typical strat, where tethered players soak towers and non-tethered players bait cleaves.""");
	private final ModifiableCallout<AbilityCastStart> darklitTowerNoTether = ModifiableCallout.durationBasedCall("Darklit Dragonsong: Tower, no Tether", "Bait Cleave");

	private final ModifiableCallout<?> darklitSpiritTaker = new ModifiableCallout<>("Darklit Dragonsong: Spirit Taker", "Spread");
	private final ModifiableCallout<AbilityCastStart> darklitStacks = ModifiableCallout.durationBasedCall("Darklit Dragonsong: Stacks", "Stacks");
	private final ModifiableCallout<?> darklitTankBaits = new ModifiableCallout<>("Darklit Dragonsong: Tank Baits", "Tank Baits");

	// This can't be an @NpcCastCallout because it can overlap with other calls
	private final ModifiableCallout<AbilityCastStart> edgeOfOblivion = ModifiableCallout.durationBasedCall("Edge of Oblivion", "Raidwide");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> darklitDragonsong = SqtTemplates.sq(60_000,
			// Technically, we miss the headmarkers by doing this, but the players with stacks get a stack debuff anyway
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D2F),
			(e1, s) -> {
				s.updateCall(darklitRaidwide, e1);

				var stacks = s.waitEvents(2, BuffApplied.class, ba -> ba.buffIdMatches(0x99D));
				var tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(0x6E));

				BuffApplied playerStack = stacks.stream().filter(stack -> stack.getTarget().isThePlayer()).findFirst().orElse(null);
				TetherEvent playerTether = tethers.stream().filter(tether -> tether.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst().orElse(null);

				if (playerStack != null) {
					if (playerTether != null) {
						s.updateCall(darklitTetherStack, playerStack);
					}
					else {
						s.updateCall(darklitStack, playerStack);
					}
				}
				else {
					if (playerTether != null) {
						s.updateCall(darklitTether);
					}
					else {
						s.updateCall(darklitNothing);
					}
				}
				var towerCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CFB));
				// Wait for previous call
				s.waitMs(500);
				if (playerTether != null) {
					// In the typical strat, tethered players take the tower
					s.updateCall(darklitTowerWithTether, towerCast);
				}
				else {
					// In the typical strat, non-tethered players bait the chains
					s.updateCall(darklitTowerNoTether, towerCast);
				}

				// Tower actual hit (9CFB is too early by about 800ms)
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CFE));

				s.updateCall(darklitSpiritTaker);

				var hallowedWings = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D23, 0x9D24));
				s.setParam("hallowedSafe", hallowedWings.abilityIdMatches(0x9D23) ? ArenaSector.WEST : ArenaSector.EAST);
				s.updateCall(darklitStacks, hallowedWings);
				s.waitCastFinished(casts, hallowedWings);
				s.updateCall(darklitTankBaits);
				var edgeCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CEE));
				s.updateCall(edgeOfOblivion, edgeCast);
			});

	private final ModifiableCallout<AbilityCastStart> akhMornStacks = ModifiableCallout.durationBasedCall("Akh Morn (Stacks)", "Stacks");
	private final ModifiableCallout<AbilityCastStart> mornAfahStack = ModifiableCallout.durationBasedCall("Morn Afah (Stack)", "Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> akhMorn = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D37),
			(e1, s) -> {
				s.updateCall(akhMornStacks, e1);
				var mornAfahCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9D39), false);
				s.updateCall(mornAfahStack, mornAfahCast);
			});

	private final ModifiableCallout<AbilityCastStart> crystallizeTimeInitial = ModifiableCallout.durationBasedCall("Crystallize Time: Initial", "Raidwide");

	private final ModifiableCallout<BuffApplied> crystallizeAero = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Aero + Long Red", "Aero").statusIcon(0x99F);
	private final ModifiableCallout<BuffApplied> crystallizeIceShortRed = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Ice + Short Red", "Ice and Short Red, Pop Head").statusIcon(0xCBF);
	private final ModifiableCallout<BuffApplied> crystallizeIceBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Ice + Blue", "Ice and Blue").statusIcon(0x99E);
	private final ModifiableCallout<BuffApplied> crystallizeEruption = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Eruption + Blue", "Eruption and Blue").statusIcon(0x99C);
	private final ModifiableCallout<BuffApplied> crystallizeStack = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Stack + Blue", "Stack and Blue").statusIcon(0x996);
	private final ModifiableCallout<BuffApplied> crystallizeWater = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Water + Blue", "Water and Blue").statusIcon(0x99D);

	private final ModifiableCallout<?> crystallizeTethers = new ModifiableCallout<>("Crystallize Time: Tethers", "")
			.disabledByDefault()
			.extendedDescription("""
					During and after this callout, you can use variables `{shortLights}`, `{longLights}`, and `{nonTethered}` to reference the locations of the short (always N/S), long, and un-tethered lights, respectively.\
					In addition, `{kbSafe}` is the knockback safe spot, i.e. whichever `longLights` is northernmost.""");

	private final ModifiableCallout<BuffApplied> crystallize2Aero = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Aero + Long Red", "Move, Push Party").statusIcon(0x99F);
	private final ModifiableCallout<BuffApplied> crystallize2IceShortRed = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Ice + Short Red", "Pop Head").statusIcon(0xCBF);
	private final ModifiableCallout<BuffApplied> crystallize2IceBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Ice + Blue", "Move, Get Pushed {kbSafe}").statusIcon(0x99E);
	private final ModifiableCallout<BuffApplied> crystallize2Eruption = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Eruption + Blue", "Eruption").statusIcon(0x99C);
	private final ModifiableCallout<BuffApplied> crystallize2Stack = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Stack + Blue", "Move, Get Pushed {kbSafe}").statusIcon(0x996);
	private final ModifiableCallout<BuffApplied> crystallize2Water = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After First Lights: Water + Blue", "Move, Get Pushed {kbSafe}").statusIcon(0x99D);

	private final ModifiableCallout<BuffApplied> crystallize3Aero = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Aero + Long Red", "Avoid Lights").statusIcon(0x99F);
	private final ModifiableCallout<BuffApplied> crystallize3IceShortRed = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Ice + Short Red", "Stack/Avoid Light").statusIcon(0xCBF);
	private final ModifiableCallout<BuffApplied> crystallize3IceBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Ice + Blue", "Stack").statusIcon(0x99E);
	private final ModifiableCallout<BuffApplied> crystallize3Eruption = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Eruption + Blue", "Stack").statusIcon(0x99C);
	private final ModifiableCallout<BuffApplied> crystallize3Stack = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Stack + Blue", "Stack").statusIcon(0x996);
	private final ModifiableCallout<BuffApplied> crystallize3Water = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time, After KB: Water + Blue", "Stack").statusIcon(0x99D);

	private final ModifiableCallout<?> crystallize4Aero = new ModifiableCallout<>("Crystallize Time, After Stacks: Aero + Long Red", "Dodge and Pop Heads").statusIcon(0xCBF);
	private final ModifiableCallout<?> crystallize4IceShortRed = new ModifiableCallout<>("Crystallize Time, After Stacks: Ice + Short Red", "Dodge");
	private final ModifiableCallout<?> crystallize4IceBlue = new ModifiableCallout<>("Crystallize Time, After Stacks: Ice + Blue", "Dodge and Cleanse").statusIcon(0xCC0);
	private final ModifiableCallout<?> crystallize4Eruption = new ModifiableCallout<>("Crystallize Time, After Stacks: Eruption + Blue", "Dodge and Cleanse").statusIcon(0xCC0);
	private final ModifiableCallout<?> crystallize4Stack = new ModifiableCallout<>("Crystallize Time, After Stacks: Stack + Blue", "Dodge and Cleanse").statusIcon(0xCC0);
	private final ModifiableCallout<?> crystallize4Water = new ModifiableCallout<>("Crystallize Time, After Stacks: Water + Blue", "Dodge and Cleanse").statusIcon(0xCC0);

	private final ModifiableCallout<BuffApplied> crystallize5quietus = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Quietus", "Raidwide").statusIcon(0x104E);
	private final ModifiableCallout<BuffApplied> crystallize5dropRewind = ModifiableCallout.<BuffApplied>durationBasedCall("Crystallize Time: Drop Rewind", "Drop Rewind {tidalFrom}").statusIcon(0x1070);

	private final ModifiableCallout<AbilityCastStart> crystallize6cleanse = ModifiableCallout.<AbilityCastStart>durationBasedCall("Crystallize Time, After Rewind: Cleanse", "Cleanse and Spread").statusIcon(0xCC0);
	private final ModifiableCallout<AbilityCastStart> crystallize6nothing = ModifiableCallout.durationBasedCall("Crystallize Time, After Rewind: Nothing", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> crystallizeTimeSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D30),
			(e1, s) -> {
				log.info("Crystallize Time: Initial");
				s.updateCall(crystallizeTimeInitial, e1);
				s.waitCastFinished(casts, e1);

				// Collect debuffs
				// Red debuff
				var wyrmclawColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0xCBF));
				// Blue debuff
				var wyrmfangColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0xCC0));
				// Ice
				var blizzColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x99E));
				// Wind
				var aeroColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x99F));
				// Water
				var waterColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x99D));
				// Stack
				var darknessColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x996));
				// Spread
				var eruptionColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x99C));
				// Quietus
				var quietusColl = new EventCollector<BuffApplied>(ba -> ba.buffIdMatches(0x104E));

				s.collectEvents(19, 1_500, BuffApplied.class, true, List.of(wyrmclawColl, wyrmfangColl, blizzColl, aeroColl, waterColl, darknessColl, eruptionColl, quietusColl));

				// Aero + Long Red
				aeroColl.findAny(isPlayer).ifPresent(aero -> s.updateCall(crystallizeAero, aero));
				// Ice
				blizzColl.findAny(isPlayer).ifPresent(ice -> {
					// If you have ice, you can either have short red or blue
					wyrmclawColl.findAny(isPlayer).ifPresentOrElse(redIgnored -> {
						// Ice + Short Red
						s.updateCall(crystallizeIceShortRed, ice);
					}, () -> {
						// Ice + Blue
						s.updateCall(crystallizeIceBlue, ice);
					});
				});
				eruptionColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallizeEruption, erupt));
				darknessColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallizeStack, erupt));
				waterColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallizeWater, erupt));

				// TODO: is the raidwide (9CEE) worth calling here?

				// Collect stoplight tethers
				var longTethers = s.waitEvents(2, TetherEvent.class, te -> te.tetherIdMatches(0x85));
				List<ArenaSector> shortSectors = List.of(ArenaSector.NORTH, ArenaSector.SOUTH);
				List<ArenaSector> longSectors = longTethers.stream()
						.map(t -> arenaPosNarrow.forCombatant(state.getLatestCombatantData(Objects.requireNonNull(t.getTargetMatching(cbt -> cbt.npcIdMatches(17837))))))
						.toList();
				s.setParam("shortLights", shortSectors);
				s.setParam("longLights", longSectors);
				ArenaSector kbSafe = longSectors.stream().filter(sect -> sect.isStrictlyAdjacentTo(ArenaSector.NORTH)).findFirst().orElseGet(() -> {
					log.error("Could not find kb safe spot!");
					return ArenaSector.NORTH;
				});
				s.setParam("kbSafe", kbSafe);
				// Compute safe by starting with all possible positions and removing the tethered lights
				var nonTethered = new ArrayList<>(List.of(ArenaSector.NORTH, ArenaSector.SOUTH, ArenaSector.NORTHWEST, ArenaSector.NORTHEAST, ArenaSector.SOUTHWEST, ArenaSector.SOUTHEAST));
				nonTethered.removeAll(shortSectors);
				nonTethered.removeAll(longSectors);
				s.setParam("nonTethered", nonTethered);
				s.call(crystallizeTethers);

				// Wait for first stoplight
				s.waitEvent(AbilityCastStart.class, aue -> aue.abilityIdMatches(0x9D6B));

				// First stoplight has just gone off at this point
				// Aero + Long Red
				aeroColl.findAny(isPlayer).ifPresent(aero -> s.updateCall(crystallize2Aero, aero));
				// Ice
				blizzColl.findAny(isPlayer).ifPresent(ice -> {
					// If you have ice, you can either have short red or blue
					wyrmclawColl.findAny(isPlayer).ifPresentOrElse(redIgnored -> {
						// Ice + Short Red
						s.updateCall(crystallize2IceShortRed, ice);
					}, () -> {
						// Ice + Blue
						s.updateCall(crystallize2IceBlue, ice);
					});
				});
				eruptionColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize2Eruption, erupt));
				darknessColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize2Stack, erupt));
				waterColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize2Water, erupt));

				// This is the knockback
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D58));

				BuffApplied stackBuff = buffs.findBuffById(0x996);
				// Aero + Long Red
				aeroColl.findAny(isPlayer).ifPresent(aero -> s.updateCall(crystallize3Aero, stackBuff));
				// Ice
				blizzColl.findAny(isPlayer).ifPresent(ice -> {
					// If you have ice, you can either have short red or blue
					wyrmclawColl.findAny(isPlayer).ifPresentOrElse(redIgnored -> {
						// Ice + Short Red
						s.updateCall(crystallize3IceShortRed, stackBuff);
					}, () -> {
						// Ice + Blue
						s.updateCall(crystallize3IceBlue, stackBuff);
					});
				});
				eruptionColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize3Eruption, stackBuff));
				darknessColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize3Stack, stackBuff));
				waterColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize3Water, stackBuff));

				// Stack popping
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D55));

				// Aero + Long Red
				aeroColl.findAny(isPlayer).ifPresent(aero -> s.updateCall(crystallize4Aero));
				// Ice
				blizzColl.findAny(isPlayer).ifPresent(ice -> {
					// If you have ice, you can either have short red or blue
					wyrmclawColl.findAny(isPlayer).ifPresentOrElse(redIgnored -> {
						// Ice + Short Red
						s.updateCall(crystallize4IceShortRed);
					}, () -> {
						// Ice + Blue
						s.updateCall(crystallize4IceBlue);
					});
				});
				eruptionColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize4Eruption));
				darknessColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize4Stack));
				waterColl.findAny(isPlayer).ifPresent(erupt -> s.updateCall(crystallize4Water));

				TidalLightSafeSpotEvent safeSpotEvent = s.waitEvent(TidalLightSafeSpotEvent.class);


				// Quietus resolving
				var rewindBuff = buffs.findBuffById(0x1070);
				s.setParam("tidalFrom", safeSpotEvent.getSafe());
				s.updateCall(crystallize5dropRewind, rewindBuff);

				// Squeeze in the quietus call
				s.waitMs(3_500);
				quietusColl.getEvents().stream().findAny().ifPresent(quietus -> s.call(crystallize5quietus, quietus));

				// "Spell-in-Waiting: Return" is removed, and you get "Return" instead
				var spiritTakerCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D60));
				if (buffs.isStatusOnTarget(state.getPlayer(), 0xCC0)) {
					s.updateCall(crystallize6cleanse, spiritTakerCast);
				}
				else {
					s.updateCall(crystallize6nothing, spiritTakerCast);
				}
				log.info("Crystallize Time: Finished");
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> tidalLightColl = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D3B),
			(e1, s) -> {
				ArenaSector first = arenaPos.forCombatant(state.getLatestCombatantData(e1.getSource()));
				var e2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D3B));
				ArenaSector second = arenaPos.forCombatant(state.getLatestCombatantData(e2.getSource()));
				var out = new TidalLightSafeSpotEvent(first, second);
				log.info("tidalLightColl: {} -> {} ==> {}", first, second, out.getSafe());
				s.accept(out);
			});

	private static class TidalLightSafeSpotEvent extends BaseEvent {
		@Serial
		private static final long serialVersionUID = -8145814631605864973L;
		private final ArenaSector firstDirection;
		private final ArenaSector secondDirection;

		private TidalLightSafeSpotEvent(ArenaSector firstDirection, ArenaSector secondDirection) {
			this.firstDirection = firstDirection;
			this.secondDirection = secondDirection;
		}

		public ArenaSector getFirstDirection() {
			return firstDirection;
		}

		public ArenaSector getSecondDirection() {
			return secondDirection;
		}

		public ArenaSector getSafe() {
			return ArenaSector.tryCombineTwoCardinals(List.of(
					firstDirection,
					secondDirection
			));
		}
	}


	@NpcCastCallout(value = 0x9D71, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> memorysEnd = ModifiableCallout.durationBasedCall("Memory's End", "Enrage");


	// P5
	private final ModifiableCallout<AbilityCastStart> fulgentBlade = ModifiableCallout.durationBasedCall("Fulgent Blade: Initial", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> fulgentBladeCw = ModifiableCallout.durationBasedCall("Fulgent Blade: CW");
	private final ModifiableCallout<AbilityCastStart> fulgentBladeCcw = ModifiableCallout.durationBasedCall("Fulgent Blade: CCW");
	private final ModifiableCallout<?> fulgentBladeCw1 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 1", "Move")
			.extendedDescription("Please take note that in order to reduce spammy calls, only the first hit is called by default. The rest can be enabled below.");
	private final ModifiableCallout<?> fulgentBladeCw2 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 2", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCw3 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 3", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCw4 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 4", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCw5 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 5", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCw6 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 6", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCw7 = new ModifiableCallout<>("Fulgent Blade: Clockwise, Hit 7", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw1 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 1", "Move")
			.extendedDescription("""
					Please take note that in order to reduce spammy calls, only the first hit is called by default. The rest can be enabled below.
										
					Depending on your strategy, you may not need all of these.""");
	private final ModifiableCallout<?> fulgentBladeCcw2 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 2", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw3 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 3", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw4 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 4", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw5 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 5", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw6 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 6", "Move").disabledByDefault();
	private final ModifiableCallout<?> fulgentBladeCcw7 = new ModifiableCallout<>("Fulgent Blade: Counter-Clockwise, Hit 7", "Move").disabledByDefault();

	private final ArenaPos apFulgent = new ArenaPos(100.0, 100.0, 1, 1);

	private boolean fulgentIsCw(List<ArenaSector> firstHits, List<ArenaSector> subsequentHits) {
		for (ArenaSector hit : firstHits) {
			Optional<ArenaSector> adjacent = subsequentHits.stream().filter(otherHit -> otherHit.isStrictlyAdjacentTo(hit)).findFirst();
			if (adjacent.isPresent()) {
				ArenaSector nextHit = adjacent.get();
				if (hit.eighthsTo(nextHit) == 1) {
					// clockwise
					return true;
				}
				else if (hit.eighthsTo(nextHit) == -1) {
					// ccw
					return false;
				}
			}
		}
		throw new IllegalArgumentException("Unable to determine rotation direction for fulgent: %s -> %s".formatted(firstHits, subsequentHits));

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fulgentSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D72),
			(e1, s) -> {
				s.updateCall(fulgentBlade, e1);
				// There are three sets of lines casting. in the fru_anon.log example, the first set intersects N-NW (hitting E-W and NE-SW),
				// the second set intsersects W-SW (hitting N-S and NW-SE), and the third set intersects S-SE (hitting E-W and NE-SW like
				// the first set).
				// There are also ACEEs, where the first 6 events are all 19D 1:2:0:0. These *might* already indicate the order, but not
				// sure.
				// The second set (2s later) is two 19D 20:10:0:0 and seems to represent the first two hits.
				// The third set (~4s after 2nd set) is two more of the same, and seems to represent the second pair of hits.
				// Fourth set (~3s after 3rd) is two more.
				// In addition, each wave starts with two 0x9CB6 Path of Darkness and two 0x9D73 Path of Light.
				// Two "lines" per wave, plus one going in + one going out = 4

				// All 6
				var initials = s.waitEvents(6, ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x19D && acee.getData0() == 1);
				s.refreshCombatants();

				// First 2
				// We might not need these, if it turns out that the first 2 initials are always the s tart
				var firstHit = s.waitEvents(2, ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x19D && acee.getData0() == 0x20);
				// The rotation direction seems to be less obnoxious than previously thought.
				// Every dummy actor is facing either directly towards or directly away from (100, 100)
				// Thus, we should be able to use a very narrow ArenaPos to determine directions
				List<ArenaSector> firstHits = firstHit.stream().map(e -> apFulgent.forCombatant(state.getLatestCombatantData(e.getTarget()))).toList();
				List<ArenaSector> subsequentHits = initials.stream().map(e -> apFulgent.forCombatant(state.getLatestCombatantData(e.getTarget()))).filter(item -> !firstHits.contains(item)).toList();
				boolean isCw = fulgentIsCw(firstHits, subsequentHits);
				var cast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9CB6, 0x9D73), false);
				s.updateCall(isCw ? fulgentBladeCw : fulgentBladeCcw, cast);

				// First cast goes off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CB6, 0x9D73));
				s.updateCall(isCw ? fulgentBladeCw1 : fulgentBladeCcw1);
				s.waitMs(200); // debounce
				// First followup from cast 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw2 : fulgentBladeCcw2);
				s.waitMs(300); // debounce
				// Second cast + second followup from 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw3 : fulgentBladeCcw3);
				s.waitMs(300); // debounce
				// first followup from 2 + third followup from 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw4 : fulgentBladeCcw4);
				s.waitMs(400); // debounce
				// third cast + second followup from 2 + fourth followup from 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw5 : fulgentBladeCcw5);
				s.waitMs(400); // debounce
				// first followup from 3 + third followup from 2 + fifth followup from 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw6 : fulgentBladeCcw6);
				s.waitMs(400); // debounce
				// second followup from 3 + fourth followup from 2 + sixth followup from 1
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D74, 0x9D75));
				s.updateCall(isCw ? fulgentBladeCw7 : fulgentBladeCcw7);
			});
	// Old notes, for posterity

	// Problem: It should be possible to determine rotation direction from the "full set" and the initial pair.
	// However, this is harder than it sounds, because the absolute positions are all over the place.
	// For example:
	// Look at the first 2 getting hit.
	// Find the two opposite by checking for actors that are either facing the same or opposite direction of the first two.
	// Find which one is facing one- or three-eighths CW or CCW from one of the first two.
	// e.g. fru_anon.log
	// first two face N and SE
	// second two face W and NE
	// third two face S and NW
	// Our set is N, NE, W, NW, S, SE
	// We eliminate S and NW because they are opposite of N and SE.
	// This leaves us with [N, SE] initial and [W, NE]
	// But this could indicate either CCW (N -> NW -> W) or CW (NW -> N -> NE)

	@NpcCastCallout(0x9D76)
	private final ModifiableCallout<AbilityCastStart> p5akhMorn = ModifiableCallout.durationBasedCall("P5 Akh Morn", "Stacks");

	// TODO: what does the Paradise Regained (9D7F) cast actually do?

	private final ModifiableCallout<AbilityCastStart> paradiseRegainedDarkCleaveDarkTether = ModifiableCallout.durationBasedCall("Paradise Regained: Dark Wing + Dark Tether", "Tanks Left, Party Out");
	private final ModifiableCallout<AbilityCastStart> paradiseRegainedDarkCleaveLightTether = ModifiableCallout.durationBasedCall("Paradise Regained: Dark Wing + Light Tether", "Tanks Left, Party In");
	private final ModifiableCallout<AbilityCastStart> paradiseRegainedLightCleaveDarkTether = ModifiableCallout.durationBasedCall("Paradise Regained: Light Wing + Dark Tether", "Tanks Right, Party Out");
	private final ModifiableCallout<AbilityCastStart> paradiseRegainedLightCleaveLightTether = ModifiableCallout.durationBasedCall("Paradise Regained: Light Wing + Light Tether", "Tanks Right, Party In");

	private final ModifiableCallout<?> paradiseRegained2DarkCleaveDarkTether = new ModifiableCallout<>("Paradise Regained 2nd Cleave: Dark Wing + Dark Tether", "Party Out");
	private final ModifiableCallout<?> paradiseRegained2DarkCleaveLightTether = new ModifiableCallout<>("Paradise Regained 2nd Cleave: Dark Wing + Light Tether", " Party In");
	private final ModifiableCallout<?> paradiseRegained2LightCleaveDarkTether = new ModifiableCallout<>("Paradise Regained 2nd Cleave: Light Wing + Dark Tether", "Party Out");
	private final ModifiableCallout<?> paradiseRegained2LightCleaveLightTether = new ModifiableCallout<>("Paradise Regained 2nd Cleave: Light Wing + Light Tether", "Party In");

	private final ModifiableCallout<?> paradiseRegained3 = new ModifiableCallout<>("Paradise Regained 3rd Tower", "Third Tower");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> paradiseRegainedSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D7F),
			(e1, s) -> {
				/*
				Mech explanation:
				First tower spawns, this is relative south
				Second tower spawns, rel NW/NE
				Third tower spawns, rel NE/NW
				Dark or light wing starts to glow. This will do a half room cleave, plus an extra 30 degrees towards the boss side.
				Boss needs to turn so that first tower is safe.
				This means the MT should get in one of the two north towers, leaving the rel south tower safe.
				*/
				// Wings light or dark cast, this is what does the glowy wings
				// 9D29 indicates cleaving right then left (dark glowing first)
				// 9D79 indicates cleaving left then right (light glowing first)
				// Tether ID 1 = close
				// Tether ID 2 = far
				TetherEvent firstTether = s.waitEvent(TetherEvent.class, te -> te.tetherIdMatches(1, 2));
				AbilityCastStart cast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9D29, 0x9D79), false);

				boolean darkTether = firstTether.tetherIdMatches(1);
				boolean darkWingFirst = cast.abilityIdMatches(0x9D29);
				if (darkTether) {
					if (darkWingFirst) {
						s.updateCall(paradiseRegainedDarkCleaveDarkTether, cast);
					}
					else {
						s.updateCall(paradiseRegainedLightCleaveDarkTether, cast);
					}
				}
				else {
					if (darkWingFirst) {
						s.updateCall(paradiseRegainedDarkCleaveLightTether, cast);
					}
					else {
						s.updateCall(paradiseRegainedLightCleaveLightTether, cast);
					}
				}

				s.waitCastFinished(casts, cast);

				// These are just inverted
				if (!darkTether) {
					if (!darkWingFirst) {
						s.updateCall(paradiseRegained2DarkCleaveDarkTether);
					}
					else {
						s.updateCall(paradiseRegained2LightCleaveDarkTether);
					}
				}
				else {
					if (!darkWingFirst) {
						s.updateCall(paradiseRegained2DarkCleaveLightTether);
					}
					else {
						s.updateCall(paradiseRegained2LightCleaveLightTether);
					}
				}


				// Map effects seem to indicate tower locations

				// Mech order seems to be:
				// Paradise Regained cast (3.7s)
				// Paradise regained snaps
				// ~3s later, First tether appears
				// Cast starts (6.6s)
				// Cast snaps
				// ~0.5s later, first tower and cleave
				// ~0.8s later, first tether hits
				// ~2.5s later, second tower and cleave
				// ~0.3s later, second cleave hits
				// ~0.8s later, second tether hits
				// ~2.3s later, third tower
			});

	private final ModifiableCallout<AbilityCastStart> polarizingInitial = ModifiableCallout.durationBasedCall("Polarizing Strikes", "Line Stacks");
	private final ModifiableCallout<?> polarizingHit = new ModifiableCallout<>("Polarizing Strikes Hit (No Swap)", "Move");
	private final ModifiableCallout<?> polarizingHitSwap = new ModifiableCallout<>("Polarizing Strikes Swap", "Swap");
	private final ModifiableCallout<?> polarizingMoveBack = new ModifiableCallout<>("Polarizing Strikes: Move Back to Stacks", "Stacks")
			.extendedDescription("""
					The variable `i` ranges from 1 to 4 and indicates which hit just happened.
					e.g. If you want to take the third hit, set the callout to {i == 2 ? 'Front' 'Back'}""");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> polarizingSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D7C),
			(e1, s) -> {
				s.updateCall(polarizingInitial, e1);
				for (int i = 1; i <= 4; i++) {
					s.setParam("i", i);
					var hits = s.waitEventsQuickSuccession(8, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9D7D, 0x9D7E));
					// Don't bother calling for a swap on the 4th set
					if (i < 4) {
						// Check if player got hit, and if so, if they got the debuff
						hits.stream().filter(aue -> aue.getTarget().isThePlayer())
								.flatMap(hit -> hit.getEffectsOfType(StatusAppliedEffect.class).stream())
								.filter(sae -> sae.buffIdMatches(0xCFB, 0x1044))
								.findFirst()
								.ifPresentOrElse(
										ignored -> s.updateCall(polarizingHitSwap),
										() -> s.updateCall(polarizingHit));
					}
					else {
						s.updateCall(polarizingHit);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CB7, 0x9CB8));
					if (i < 4) {
						s.updateCall(polarizingMoveBack);
					}
				}

			});

	@NpcCastCallout(0x9D86)
	private final ModifiableCallout<AbilityCastStart> pandorasBox = ModifiableCallout.durationBasedCall("Pandora's Box", "Tank LB");

	@NpcCastCallout(value = 0x9D88, cancellable = true)
	private final ModifiableCallout<AbilityCastStart> p5enrage = ModifiableCallout.durationBasedCall("P5 Enrage", "Enrage");

}



