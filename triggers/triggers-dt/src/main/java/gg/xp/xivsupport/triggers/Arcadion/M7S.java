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
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "M7S", duty = KnownDuty.M7S)
public class M7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M7S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M7S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M7S);
	}

	// P1

	@NpcCastCallout(0xA55B)
	private final ModifiableCallout<AbilityCastStart> brutalImpact = ModifiableCallout.durationBasedCall("Brutal Impact", "Multi Hit Raidwide");

	private final ModifiableCallout<AbilityCastStart> inSmashThere = ModifiableCallout.durationBasedCall("In + Smash There", "In, Tanks Out, Party In");
	private final ModifiableCallout<AbilityCastStart> inSmashHere = ModifiableCallout.durationBasedCall("In + Smash Here", "In, Tanks In, Party Out");
	private final ModifiableCallout<AbilityCastStart> outSmashThere = ModifiableCallout.durationBasedCall("Out + Smash There", "Out, Tanks Out, Party In");
	private final ModifiableCallout<AbilityCastStart> outSmashHere = ModifiableCallout.durationBasedCall("Out + Smash Here", "Out, Tanks In, Party Out");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> smashHereThere = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA55F, 0xA560),
			(e1, s) -> {
				boolean partyIn = e1.abilityIdMatches(0xA560);
				// A561 is OUT, A562 is IN
				AbilityCastStart inOut = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xA561, 0xA562), false);
				boolean overallIn = inOut.abilityIdMatches(0xA562);

				if (overallIn) {
					s.updateCall(partyIn ? inSmashThere : inSmashHere, inOut);
				}
				else {
					s.updateCall(partyIn ? outSmashThere : outSmashHere, inOut);
				}

			});

	private static final ArenaPos pollenPos = new ArenaPos(100, 100, 8, 8);

	private final ModifiableCallout<AbilityCastStart> sporePollenSafe = ModifiableCallout.<AbilityCastStart>durationBasedCall("Pollen Safe Spots", "{safeCorners} Corners Safe")
			.extendedDescription("""
					To use inner safe spots instead of corners, use {safeInner} instead of {safeCorners}""");
	private final ModifiableCallout<AbilityCastStart> sporeSinisterSeedOnYou = ModifiableCallout.durationBasedCall("Sinister Seed on You", "Drop Seed");
	private final ModifiableCallout<?> sporeDropPuddles = new ModifiableCallout<>("Drop Puddles", "Bait Puddles");
	private final ModifiableCallout<AbilityCastStart> sporeDodgeTentril = ModifiableCallout.durationBasedCall("Dodge Tendrils", "Dodge Tendrils");

	private final ModifiableCallout<AbilityCastStart> windingWildwindsInterrupt = ModifiableCallout.durationBasedCall("Winding Wildwinds Interrupt", "Interrupt Winding");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sporeSac = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA569),
			(e1, s) -> {
				// Spore Sac casts from the actual spores
				s.waitEvents(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56A));
				// Boss casts Sinister Seeds A56D
				// Then there are some map effects:
				/*
					800375C6:20001:5 through 8
					Also 5 through 8 of 200010 but that's too late, might as well just look at pollen cast
				 */
				var pollens = s.waitEventsQuickSuccession(12, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56B));
				s.waitThenRefreshCombatants(100);
				var outerSafeCorners = EnumSet.copyOf(ArenaSector.quadrants);
				pollens.stream().map(cast -> state.getLatestCombatantData(cast.getSource()))
						.map(pollenPos::forCombatant)
						.forEach(outerSafeCorners::remove);
				if (outerSafeCorners.size() != 2) {
					log.error("bad outerSafeCorners! {}", outerSafeCorners);
				}
				else {
					s.setParam("safeCorners", outerSafeCorners);
					s.setParam("safeInner", outerSafeCorners.stream().map(c -> c.plusQuads(1)).toList());
				}
				s.updateCall(sporePollenSafe, pollens.get(0));

				List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56E));
				markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(
								myMarker -> s.updateCall(sporeSinisterSeedOnYou, myMarker),
								() -> s.updateCall(sporeDropPuddles));
				s.waitCastFinished(casts, markers.get(0));
				AbilityCastStart tendrilCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA570));
				s.updateCall(sporeDodgeTentril, tendrilCast);
				if (state.playerJobMatches(Job::caresAboutInterrupt)) {
					var winding = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA90D));
					s.updateCall(windingWildwindsInterrupt, winding);
				}
				/*
					After this:
					Get to safe spot
					Ranged rotate around to not get hit by panto puddles (4 puddles)
					Melee drop aoes
						Make trigger for these - either you got a marker or you didn't
						The headmarker also comes with A56E cast on you
					Dodge 8-ways (Tendrils of Terror)
					Adds spawn
					They start casting stuff
					You can interrupt winding
					Hurricane force is adds enrage

				 */
			});

	private static final ArenaPos exploPos = new ArenaPos(100, 100, 5, 5);

	@NpcCastCallout(0xA575)
	private final ModifiableCallout<AbilityCastStart> quarrySwampGaze = ModifiableCallout.durationBasedCall("Quarry Swamp: LoS Gaze", "Hide Behind Monster");

	private final ModifiableCallout<AbilityCastStart> quarrySwamp1 = ModifiableCallout.durationBasedCall("Quarry Swamp: First Safe Spot", "{safe} safe");
	private final ModifiableCallout<?> quarrySwampDirection = new ModifiableCallout<>("Quarry Swamp: Direction", "{clockwise ? 'Clockwise' : 'Counter-Clockwise'}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> explosionsSq = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA575),
			(e1, s) -> {
				// It's DSR gigaflares
				CastLocationDataEvent first = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA576));
				ArenaSector firstPos = exploPos.forPosition(first.getPos());
				s.setParam("safe", firstPos.opposite());
				s.updateCall(quarrySwamp1, first.originalEvent());
				CastLocationDataEvent second = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA576));
				ArenaSector secondPos = exploPos.forPosition(second.getPos());
				boolean clockwise = firstPos.eighthsTo(secondPos) > 0;
				s.setParam("clockwise", clockwise);
				s.call(quarrySwampDirection);
			});

	@NpcCastCallout(0xA577)
	private final ModifiableCallout<AbilityCastStart> pulpSmash = ModifiableCallout.durationBasedCall("Pulp Smash", "Stack into Clocks");
	@NpcCastCallout(0xA57A)
	private final ModifiableCallout<AbilityCastStart> cameFromDirt = ModifiableCallout.durationBasedCall("It Came from the Dirt", "Clocks");

	@NpcCastCallout(0xA57C)
	private final ModifiableCallout<AbilityCastStart> neoBombarianSpecial = ModifiableCallout.durationBasedCall("Neo-Bombarian Special", "Go North, Raidwide");

	// P2

	@NpcCastCallout(0xA587)
	private final ModifiableCallout<AbilityCastStart> revengeOfTheVines = ModifiableCallout.durationBasedCall("Revenge of the Vines", "Raidwide");

	private static final ArenaPos p2pos = new ArenaPos(100, 5, 10, 10);

	private final ModifiableCallout<AbilityCastStart> brutishSwingIn = ModifiableCallout.durationBasedCall("Brutish Swing: In", "In at {where}");
	private final ModifiableCallout<AbilityCastStart> brutishSwingOut = ModifiableCallout.durationBasedCall("Brutish Swing: Out", "Out from {where}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> brutishSwingSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA592, 0xA593),
			(e1, s) -> {
				/*
					Brutish Swing:
					Boss NE: A58F
					Jump to SW: A592 (already casting there)
					Out

					Boss N: A58D
					Jump to SW: A593
					In
				 */
				s.waitThenRefreshCombatants(100);
				XivCombatant bossFake = state.getLatestCombatantData(e1.getSource());
				ArenaSector where = p2pos.forCombatant(bossFake);
				s.setParam("where", where);
				if (e1.abilityIdMatches(0xA592)) {
					s.updateCall(brutishSwingOut, e1);
				}
				else {
					s.updateCall(brutishSwingIn, e1);
				}
			});

	// Glower: Line cleave from boss's face + spread
	@NpcCastCallout(0xA585)
	private final ModifiableCallout<AbilityCastStart> glowerPower = ModifiableCallout.durationBasedCall("Glower Power", "Out and Spread");

	// Thorny deathmatch

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thornyDeathmatchSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA588),
			(e1, s) -> {

			});

	// Abominable Blink is the flare, no need separate call

	private final ModifiableCallout<AbilityCastStart> sporesplosionInitial = ModifiableCallout.durationBasedCall("Sporeplosion: Initial", "Dodge 3 to 1");
	private final ModifiableCallout<?> sporesplosionAfterFirst = new ModifiableCallout<>("Sporeplosion: After First Explosion", "Move");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sporeplosionSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA58A),
			(e1, s) -> {
				// Sporeplosion: 3 to 1 dodge
				s.updateCall(sporesplosionInitial, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA58B));
				s.updateCall(sporesplosionAfterFirst);
			});


	private final ModifiableCallout<?> demolitionDeathmatchInitial = new ModifiableCallout<>("Demolition Deathmatch", "Tethers near Walls");

	private final ModifiableCallout<HeadMarkerEvent> demolitionDeathmatchFlare = new ModifiableCallout<>("Demolition Deathmatch: Flare", "Flare on you");
	private final ModifiableCallout<HeadMarkerEvent> demolitionDeathmatchNoFlare = new ModifiableCallout<>("Demolition Deathmatch: Flare", "Flare on {event.target}");
	private final ModifiableCallout<AbilityCastStart> demolitionDeathmatchMarker = ModifiableCallout.durationBasedCall("Demolition Deathmatch: Seed on You", "Seed on you");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> demolitionDeathmatchSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA596),
			(e1, s) -> {
				/*
					Demolition Deathmatch:
					Some tether shit?
				 */
				s.updateCall(demolitionDeathmatchInitial);

				// Flare on a tank, they need to get away
				// Call if you got tether too TODO
				HeadMarkerEvent flare = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -48);
				if (flare.getTarget().isThePlayer()) {
					// You have flare
					s.updateCall(demolitionDeathmatchFlare, flare);
				}
				else {
					// You don't have flare
					s.updateCall(demolitionDeathmatchNoFlare, flare);
				}
				// Strange seeds are cast on their targets
				for (int i = 1; i <= 4; i++) {
					List<AbilityCastStart> strangeSeeds = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA598));
					strangeSeeds.stream().filter(ss -> ss.getTarget().isThePlayer())
							.findAny()
							.ifPresent(ss -> s.updateCall(demolitionDeathmatchMarker, ss));
				}
				// As each set drops, you have to dodge, but that would be too much noise to call
				// Then there's stack markers at the end, handled by Killer Seeds
			});

	// Handled by a sequential since another mech needs to lock it out
	private final ModifiableCallout<AbilityCastStart> killerSeeds = ModifiableCallout.durationBasedCall("Killer Seeds", "Partners");

	@NpcCastCallout(0xA59E)
	private final ModifiableCallout<AbilityCastStart> powerSlam = ModifiableCallout.durationBasedCall("Powerslam", "Big Raidwide");

	// P3

	@NpcCastCallout(0xA5AE)
	private final ModifiableCallout<AbilityCastStart> slaminator = ModifiableCallout.durationBasedCall("Slaminator", "Tower");

	private final ModifiableCallout<AbilityCastStart> debrisSporePollenSafe = ModifiableCallout.<AbilityCastStart>durationBasedCall("Debris Deathmatch Pollen Safe Spots", "{safeCorners} Corners Safe")
			.extendedDescription("""
					To use inner safe spots instead of corners, use {safeInner} instead of {safeCorners}""");

	private static final ArenaPos pollenPosP3 = new ArenaPos(100, 5, 8, 8);
	private final ModifiableCallout<AbilityCastStart> debrisSporeSinisterSeedOnYou = ModifiableCallout.durationBasedCall("Sinister Seed on You", "Drop Seed into Partners");
	private final ModifiableCallout<?> debrisSporeDropPuddles = new ModifiableCallout<>("Drop Puddles", "Bait Puddles into Partners");
	private final ModifiableCallout<AbilityCastStart> debrisSporeDodgeTentril = ModifiableCallout.durationBasedCall("Dodge Tendrils", "Dodge Tendrils");
	private final ModifiableCallout<AbilityCastStart> debrisSporeSinisterSeedOnYou2 = ModifiableCallout.durationBasedCall("Sinister Seed on You", "Place Seed");
	private final ModifiableCallout<?> debrisSporeDropPuddles2 = new ModifiableCallout<>("Drop Puddles", "Bait Puddles then Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> debrisDeathmatchSq = SqtTemplates.sq(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA5B0),
			(e1, s) -> {
				// 4 tethers from outside
				// Has the same vines as P1 with outside corners + inside intercards safe
				// First, find pollen safe spot
				var pollens = s.waitEventsQuickSuccession(12, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56B));
				s.waitThenRefreshCombatants(100);
				var outerSafeCorners = EnumSet.copyOf(ArenaSector.quadrants);
				pollens.stream().map(cast -> state.getLatestCombatantData(cast.getSource()))
						.map(pollenPosP3::forCombatant)
						.forEach(outerSafeCorners::remove);
				if (outerSafeCorners.size() != 2) {
					log.error("bad outerSafeCorners! {}", outerSafeCorners);
				}
				else {
					s.setParam("safeCorners", outerSafeCorners);
					s.setParam("safeInner", outerSafeCorners.stream().map(c -> c.plusQuads(1)).toList());
				}
				s.updateCall(debrisSporePollenSafe, pollens.get(0));
				// Also pair stack markers go out but don't need to be resolved yet
				// Resolve stacks
				// Dodge 8-ways
				// 4 people get marked, 4 people have puddles
				// Bait puddles into partners
				{
					List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA59B));

					markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny()
							.ifPresentOrElse(
									myMarker -> s.updateCall(debrisSporeSinisterSeedOnYou, myMarker),
									() -> s.updateCall(debrisSporeDropPuddles));
					s.waitCastFinished(casts, markers.get(0));
				}
				AbilityCastStart tendrilCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA59C, 0xA59D));
				// Dodge tendrils
				s.updateCall(debrisSporeDodgeTentril, tendrilCast);
				// Hide behind monster (handled by other trigger)


				{
					List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56E));
					markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny()
							.ifPresentOrElse(
									myMarker -> s.updateCall(debrisSporeSinisterSeedOnYou2, myMarker),
									() -> s.updateCall(debrisSporeDropPuddles2));
					s.waitCastFinished(casts, markers.get(0));
				}
			});

	private final ModifiableCallout<AbilityCastStart> strangeSeed = ModifiableCallout.durationBasedCallWithOffset("P3 Strange Seed", "Drop Seed", Duration.ofMillis(4_200));


	@AutoFeed
	private final SequentialTrigger<BaseEvent> strangeSeedsSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA90A),
			(e1, s) -> {
				{
					List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA598));
					Optional<AbilityCastStart> myMarker = markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny();
					myMarker.ifPresent(m -> s.updateCall(strangeSeed, m));
				}
				{
					List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA598));
					Optional<AbilityCastStart> myMarker = markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny();
					myMarker.ifPresent(m -> s.updateCall(strangeSeed, m));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> killerSeedsSq = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA59B),
			(e1, s) -> {
				// This trigger handles the calls itself
				if (!debrisDeathmatchSq.isActive()) {
					s.updateCall(killerSeeds, e1);
				}
				// Refire suppression
				s.waitMs(200);
			});

	@NpcCastCallout(0xA5B1)
	private final ModifiableCallout<AbilityCastStart> enrage = ModifiableCallout.durationBasedCallWithOffset("Special Bombarian Special (Enrage)", "Enrage", Duration.ofMillis(4_200));

	// TODO: does the precursor also determine the lariat direction? - yes it does, so integrate that
	private final ModifiableCallout<AbilityCastStart> brutishSwingInIntoLariat = ModifiableCallout.durationBasedCall("Brutish Swing: In into Lariat", "In at {where} then Dodge");
	private final ModifiableCallout<AbilityCastStart> brutishSwingOutIntoLariat = ModifiableCallout.durationBasedCall("Brutish Swing: Out into Lariat", "Out from {where} then Dodge");
	private final ModifiableCallout<AbilityCastStart> lariatDodgeLeft = ModifiableCallout.durationBasedCall("Brutish Swing: Lariat", "Dodge Left");
	private final ModifiableCallout<AbilityCastStart> lariatDodgeRight = ModifiableCallout.durationBasedCall("Brutish Swing: Lariat", "Dodge Right");
	private final ModifiableCallout<AbilityCastStart> brutishSwingInIntoGlower = ModifiableCallout.durationBasedCall("Brutish Swing: In into Glower", "In at {where} then Out");
	private final ModifiableCallout<AbilityCastStart> brutishSwingOutIntoGlower = ModifiableCallout.durationBasedCall("Brutish Swing: Out into Glower", "Out from {where} then Out");
	private final ModifiableCallout<?> brutishSwingGlowerNow = new ModifiableCallout<>("Brutish Swing: Glower", "Out and Spread");
	private final ModifiableCallout<AbilityCastStart> brutishSwingInIntoTower = ModifiableCallout.durationBasedCall("Brutish Swing: In into Tower", "In at {where} then Tower");
	private final ModifiableCallout<AbilityCastStart> brutishSwingOutIntoTower = ModifiableCallout.durationBasedCall("Brutish Swing: Out into Tower", "Out from {where} then Tower");

	// Has more jumping mechs
	@AutoFeed
	private final SequentialTrigger<BaseEvent> p3jumps = SqtTemplates.selfManagedMultiInvocation(30_000,
			// A5A3 is out, A5A5 is in
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA5A3, 0xA5A5),
			(firstBrutish, s, index) -> {
				s.waitThenRefreshCombatants(100);
				{
					XivCombatant bossFake = state.getLatestCombatantData(firstBrutish.getSource());
					ArenaSector where = p2pos.forCombatant(bossFake);
					s.setParam("where", where);
					if (firstBrutish.abilityIdMatches(0xA5A3)) {
						s.updateCall(brutishSwingOutIntoLariat, firstBrutish);
					}
					else {
						s.updateCall(brutishSwingInIntoLariat, firstBrutish);
					}
				}
				AbilityCastStart lariat = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA5A8, 0xA5AA));
				// A5A8 hits the Boss's right, therefore we dodge right facing the boss
				if (lariat.abilityIdMatches(0xA5A8)) {
					s.updateCall(lariatDodgeRight, lariat);
				}
				else {
					s.updateCall(lariatDodgeLeft, lariat);
				}
				AbilityCastStart secondBrutish = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA5A3, 0xA5A5));
				s.waitThenRefreshCombatants(100);
				// The final one does include a glower, it goes straight into the tower
				boolean skipGlower = index == 1;
				{
					XivCombatant bossFake = state.getLatestCombatantData(secondBrutish.getSource());
					ArenaSector where = p2pos.forCombatant(bossFake);
					s.setParam("where", where);
					if (secondBrutish.abilityIdMatches(0xA5A3)) {
						s.updateCall(skipGlower ? brutishSwingOutIntoTower : brutishSwingOutIntoGlower, secondBrutish);
					}
					else {
						s.updateCall(skipGlower ? brutishSwingInIntoTower : brutishSwingInIntoGlower, secondBrutish);
					}
				}
				if (!skipGlower) {
					s.waitCastFinished(casts, secondBrutish);
					s.updateCall(brutishSwingGlowerNow);
				}
			});

	/*
		Jumpies:
		Brutish Swing again
		A5A2 on boss, A5A5 south
		IN south

		Lashing Lariat: A5A7 hitting right

		Brutish Swing
		A5AC on boss, A5A3 west
		OUT west

		Followed by Glower Power A94A
		TODO this glower call is too late to react to

		They always come in pairs - is it always lariat then glower?


	 */

	// More spore + tether stuff
	// Same corner/in safe spot as P1
	// ArenaPos is still x=100, y=5 but z=-200


	// Tanks out party in etc - is there also a chariot/dynamo along with that?
	/*
	hockey stick = dynamo
	club = chariot
	 */

}
