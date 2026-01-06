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
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@CalloutRepo(name = "M9S", duty = KnownDuty.M9S)
public class M9S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M9S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M9S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M9S);
	}

	@NpcCastCallout(0xb384)
	private final ModifiableCallout<AbilityCastStart> killerVoice = ModifiableCallout.durationBasedCall("Killer Voice", "Raidwide");
	@NpcCastCallout(value = 0xb37f, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> hardcore = ModifiableCallout.durationBasedCall("Hardcore", "Double Tankbuster");

	// TODO: is the second one different?
	@NpcCastCallout({0xb34b, 0xb374})
	private final ModifiableCallout<AbilityCastStart> vampStomp = ModifiableCallout.durationBasedCall("Vamp Stomp", "Out, Watch Bats, In");

	// multiple stacks?
	@NpcCastCallout(0xb35d)
	private final ModifiableCallout<AbilityCastStart> brutalRain = ModifiableCallout.durationBasedCall("Brutal Rain", "Stacks");

	@NpcCastCallout(0xB344)
	private final ModifiableCallout<AbilityCastStart> insatiableThirst = ModifiableCallout.durationBasedCall("Insatiable Thirst", "Raidwide");

	@NpcCastCallout(0xB333)
	private final ModifiableCallout<AbilityCastStart> sadisticScreech = ModifiableCallout.durationBasedCall("Sadistic Screech", "Raidwide - Arena Change");

	// Vamp stomp: b34A (3.8s) and b374 (4.7s) cast
	// Out
	// Applies buff 1279 beforehand to players
	// applies 7a5 to vampette fatales
	// players get hit by Blast Beat b376 in pairs of two?

	// Pulping Pulse b373 cast, applies 1279 to players
	// another vamp stomp

	// Coffinfiller: in or out lines, also safe or unsafe based on where VF is hitting
	// Dead Wake (b705 4.2s or b367 4.7s) is the moving
	@NpcCastCallout(0xb367)
	private final ModifiableCallout<AbilityCastStart> deadWake = ModifiableCallout.durationBasedCall("Dead Wake", "Move Away");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> coffinFillerSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB706),
			(e1, s) -> {
				// B706 is the generic cast, the rest are the actual hits
				// X positions are 92.5, 97.5, 102.5, 107.5
				s.waitMs(200);
				// TODO: how to call these? is it worth it?
				List<CastTracker> casts = this.casts.getActiveCastsById(0xB368, 0xB369, 0xB36A);
			});

	private final ModifiableCallout<AbilityCastStart> halfMoonStart = ModifiableCallout.durationBasedCall("Half Moon Initial", "Start {initialSafe}");
	private final ModifiableCallout<AbilityCastStart> halfMoonMove = new ModifiableCallout<>("Half Moon Move", "Move {initialSafe.opposite()}");
	private final ModifiableCallout<AbilityCastStart> halfMoonWideStart = ModifiableCallout.durationBasedCall("Half Moon (Wide) Initial", "Start {initialSafe}, Wide");
	private final ModifiableCallout<AbilityCastStart> halfMoonWideMove = new ModifiableCallout<>("Half Moon (Wide) Move", "Move {initialSafe.opposite()}, Wide");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> halfMoonSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB34E, 0xB350),
			(e1, s) -> {
				var shortCast = s.findOrWaitForCastWithLocation(casts, acs -> acs.abilityIdMatches(0xB377, 0xB379, 0xB37B, 0xB37D), false);
				var shortCastTowards = ArenaPos.combatantFacing(shortCast.getLocationInfo().getBestHeading());
				boolean wide = shortCast.abilityIdMatches(0xB37D, 0xB379);
				s.setParam("initialSafe", shortCastTowards.opposite());
				s.updateCall(wide ? halfMoonWideStart : halfMoonStart, e1);
				s.waitCastFinished(casts, shortCast);
				s.updateCall(wide ? halfMoonWideMove : halfMoonMove, e1);
			});

	// Coffinfiller has 6 variants: 3 different lengths, all 5y wide
	// Also paired with Half Moon
	// From west, south safe first: B350
	// From east, south safe first: B350 (B37B B37C)
	// From west, north safe first: B350 (B37B B37C)
	// From south: east safe first: B34E (B377 B378)

	@NpcCastCallout(0xB341)
	private final ModifiableCallout<AbilityCastStart> finaleFatale = ModifiableCallout.durationBasedCall("Finale Fatale", "Raidwide");

	// Aetherletting: the rotating angle AoEs
	// then watch AoEs
	@NpcCastCallout(value = 0xB392, onYou = true)
	private final ModifiableCallout<AbilityCastStart> aetherletting = ModifiableCallout.durationBasedCall("Aetherletting", "Marker");
	private final ModifiableCallout<AbilityCastStart> aetherlettingAoes = ModifiableCallout.durationBasedCall("Aetherletting AoEs", "Dodge Lines");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> aetherLettingAoesSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0xB393),
			(e1, s) -> {
				s.waitMs(5_000);
				s.updateCall(aetherlettingAoes, e1);
				// Extra lockout
				s.waitMs(15_000);
			});
	// TODO: aoe direction

	// Sadistic Screech 2
	// Tank towers, idk whate else
	private final ModifiableCallout<AbilityCastStart> plummetAsTank = ModifiableCallout.durationBasedCall("Plummet (as Tank)", "Tank Towers");
	private final ModifiableCallout<AbilityCastStart> plummetAsNonTank = ModifiableCallout.durationBasedCall("Plummet (non Tank)", "Avoid Towers");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> plummetSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB38B),
			(e1, s) -> {
				s.updateCall(state.playerJobMatches(Job::isTank) ? plummetAsTank : plummetAsNonTank, e1);
				// Refire lockout
				s.waitMs(2_000);
			});

	// Annoying - this has a 0.2s real cast and 4.9s extra cast time
	@NpcCastCallout(0xB33E)
	private final ModifiableCallout<AbilityCastStart> crowdKill = ModifiableCallout.durationBasedCallWithOffset("Crowd Kill", "Raidwide", Duration.ofMillis(4_900));


	// Half moon stuff:
	/*
		B34E is right to left, paired with B377 (short) and b378 (long)
		B34F is right to left paired with B379 (short) and B37A (long)
		B350 is ???, paired with B37B (short) and B37C (long)
		B351 is ???, paired with B37D? (short) and B37E? (long)

		There appear to be two versions - onw hich is a perfect 50-50 cleave, the other with a wide area of overlap
		4F and 51 I think are wide
	 */

	/*
	Hell in a cell:
	you have to kill your think
	 */

	private final ModifiableCallout<AbilityCastStart> hellInACell = ModifiableCallout.durationBasedCall("Hell in a Cell", "Towers and Baits");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hellInACellSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB395),
			(e1, s) -> {
				s.updateCall(hellInACell, e1);
			});

	@NpcCastCallout(0xB39C)
	private final ModifiableCallout<AbilityCastStart> ultrasonicSpread = ModifiableCallout.durationBasedCall("Ultrasonic Spread", "Role Groups");

	@NpcCastCallout(0xB39D)
	private final ModifiableCallout<AbilityCastStart> ultrasonicAmp = ModifiableCallout.durationBasedCall("Ultrasonic Amp", "Stack");

	@NpcCastCallout(value = 0xB3A5, suppressMs = 5_000)
	private final ModifiableCallout<AbilityCastStart> sanguineScratch = ModifiableCallout.durationBasedCall("Sanguine Scratch", "Rotate, Dodge Cleaves");

	// The tower cast (bloody bondage) is longer than the UD cast
	private final ModifiableCallout<AbilityCastStart> undeadDeathmatch = ModifiableCallout.durationBasedCallWithOffset("Undead Deathmatch", "Tower Groups", Duration.ofMillis(1_200));
	private final ModifiableCallout<?> undeadDeathmatchOut = new ModifiableCallout<>("Undead Deathmatch, Away from Tether", "Out");
	private final ModifiableCallout<?> undeadDeathmatchIn = new ModifiableCallout<>("Undead Deathmatch, In Towards Tether", "In");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> undeadDeathmatchSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xB3A0),
			(e1, s) -> {
				s.updateCall(undeadDeathmatch, e1);
				var myTether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				var myTetherNpc = myTether.getTargetMatching(cbt -> !cbt.isPc());
				for (int i = 0; i < 2; i++) {
					var slva = s.waitEvent(StatusLoopVfxApplied.class, e -> e.getTarget().equals(myTetherNpc));
					boolean out = slva.getStatusLoopVfx().getId() == 0x1062;
					s.updateCall(out ? undeadDeathmatchOut : undeadDeathmatchIn);
				}
			});


}
