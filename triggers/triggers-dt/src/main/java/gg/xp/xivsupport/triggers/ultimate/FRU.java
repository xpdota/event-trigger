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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CalloutRepo(name = "FRU Triggers", duty = KnownDuty.FRU)
public class FRU extends AutoChildEventHandler implements FilteredEventHandler {
	private final JobSortSetting defaultPrio;
	private final JobSortOverrideSetting p1tethersPrio;
	private final JobSortOverrideSetting p1towersPrio;
	private XivState state;
	private ActiveCastRepository casts;

	public FRU(XivState state, PersistenceProvider pers, ActiveCastRepository casts) {
		this.casts = casts;
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

	private final ModifiableCallout<AbilityCastStart> utopianSkyStackInitial = ModifiableCallout.durationBasedCall("Utopian Sky: Initial (Fire)", "Stack Later");
	private final ModifiableCallout<AbilityCastStart> utopianSkySpreadInitial = ModifiableCallout.durationBasedCall("Utopian Sky: Initial (Lightning)", "Spread Later");
	private final ModifiableCallout<AbilityCastStart> utopianSkyStackSafeSpot = ModifiableCallout.durationBasedCall("Utopian Sky: Safe Spot (Fire)", "Stack {safe}");
	private final ModifiableCallout<AbilityCastStart> utopianSkySpreadSafeSpot = ModifiableCallout.durationBasedCall("Utopian Sky: Safe Spot (Lightning)", "Spread {safe}");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> utopianSky = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CDA, 0x9CDB),
			(e1, s) -> {
				boolean isFire = e1.abilityIdMatches(0x9CDA);
				s.updateCall(isFire ? utopianSkyStackInitial : utopianSkySpreadInitial, e1);
				var casts = s.waitEventsQuickSuccession(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CDE));
				List<ArenaSector> safe;
				do {
					List<ArenaSector> tmpSafe = new ArrayList<>(ArenaSector.all);
					casts.stream().map(cast -> arenaPos.forCombatant(state.getLatestCombatantData(cast.getSource())))
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
				s.updateCall(isFire ? utopianSkyStackSafeSpot : utopianSkySpreadSafeSpot);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cyclonicBreak2 = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9D89, 0x9D8A), // speculated
			(e1, s) -> {
				boolean isFire = e1.abilityIdMatches(0x9D89);
				// Don't talk over the other calls
//				s.waitMs(1_000);
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

				// Prior to the proteans (i.e. need another sq), there is, in one example:
				// Turn of the Heavens (6.7s) 9CD7 - determines safe spot?
				// Guessing 9CD6 is opposite, but don't know which is blue/red
				// Burnt Strike (7.7s) 9CE3 - probably E/W big cleave
				// Burnout (9.4s) 9CE4 - probably KB?
			});

	private final ModifiableCallout<AbilityCastStart> turnNSSafe = ModifiableCallout.durationBasedCall("Turn of the Heavens: Dodge Lightning", "North/South Out");
	//	private final ModifiableCallout<AbilityCastStart> turnKB = ModifiableCallout.durationBasedCall("Turn of the Heavens: Dodge Lightning", "Get Knocked to {redSafe ? 'Red' : 'Blue'}");
	private final ModifiableCallout<AbilityCastStart> turnKB = ModifiableCallout.durationBasedCall("Turn of the Heavens: Dodge Lightning", "Get Knocked to Safe Side");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> turnOfTheHeavensSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9CD7, 0x9CD6),
			(e1, s) -> {
				// TODO: which is red vs blue safe?
				// How do we determine where red or blue is?
//				s.updateCall(TODO);
				s.waitMs(4_000);

				s.setParam("redSafe", true); // TODO

				Optional<CastTracker> lightning = casts.getActiveCastById(0x9CE3);
				s.updateCall(turnNSSafe, lightning.map(CastTracker::getCast).orElse(e1));
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CE3));
				Optional<CastTracker> burnout = casts.getActiveCastById(0x9CE4);
				s.updateCall(turnKB, burnout.map(CastTracker::getCast).orElse(e1));
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CE4));


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

	private static ModifiableCallout<FruP1TetherEvent> makeTetherDefault(String descPart, int startIndex, int endIndex, boolean isFinal) {
		// If these are the final calls, call out the one that is going off. Otherwise, call the one we just collected
		int ttsIndex = isFinal ? startIndex : endIndex;
		String base = "{events[%d].mechType} on {events[%d].target}";
		String tts = isFinal ? base.formatted(ttsIndex, ttsIndex) : "";
		String text = IntStream.rangeClosed(startIndex, endIndex).mapToObj(idx -> base.formatted(idx, idx)).collect(Collectors.joining(", "));
		return new ModifiableCallout<>("Four Tethers: %s".formatted(descPart), tts, text);
	}

	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl1 = makeTetherDefault("First Tether Out", 0, 0, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl2 = makeTetherDefault("Second Tether Out", 0, 1, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl3 = makeTetherDefault("Third Tether Out", 0, 2, false);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherColl4 = makeTetherDefault("Fourth Tether Out", 0, 3, false);

	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving1 = makeTetherDefault("First Tether Resolving", 0, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving2 = makeTetherDefault("Second Tether Resolving", 1, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving3 = makeTetherDefault("Third Tether Resolving", 2, 3, true);
	private final ModifiableCallout<FruP1TetherEvent> fourTetherResolving4 = makeTetherDefault("Fourth Tether Resolving", 3, 3, true);

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

				s.waitMs(1_000);

				// Wait for floating fetters
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9CEB));
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
			}
	);

	@NpcCastCallout(0x9CC0)
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
}



