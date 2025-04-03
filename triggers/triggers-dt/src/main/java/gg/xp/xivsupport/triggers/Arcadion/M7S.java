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

import java.util.EnumSet;
import java.util.List;

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

	@NpcCastCallout(0xA560)
	private final ModifiableCallout<AbilityCastStart> smashThere = ModifiableCallout.durationBasedCall("Smash There", "Tanks Out, Party In");
	@NpcCastCallout(0xA55F)
	private final ModifiableCallout<AbilityCastStart> smashHere = ModifiableCallout.durationBasedCall("Smash Here", "Tanks In, Party Out");

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
					THey start casting stuff
					You can interrupt winding
					Hurricane force is adds enrage

				 */
			});

	private static final ArenaPos exploPos = new ArenaPos(100, 100, 5, 5);


	private final ModifiableCallout<AbilityCastStart> quarrySwamp1 = ModifiableCallout.durationBasedCall("Quarry Swamp: First Safe Spot", "{safe} safe");
	private final ModifiableCallout<?> quarrySwampDirection = new ModifiableCallout<>("Quarry Swamp: Direction", "{clockwise ? 'Clockwise' : 'Counter-Clockwise'}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> quarrySwampSq = SqtTemplates.multiInvocation(60_000,
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
	@NpcCastCallout({0xA585, 0xA94A})
	private final ModifiableCallout<AbilityCastStart> glowerPower = ModifiableCallout.durationBasedCall("Glower Power", "Out and Spread");

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

	// TODO: There's a tether + flare thing in this

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

				// TODO: secondary loop trigger to warn you if you get too many stacks
				// TODO: call if you get marked
				// Flare on a tank, they need to get away
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
					// TODO: Can the headmarker just be its own separate trigger?
					List<AbilityCastStart> strangeSeeds = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA598));
					strangeSeeds.stream().filter(ss -> ss.getTarget().isThePlayer())
							.findAny()
							.ifPresent(ss -> s.updateCall(demolitionDeathmatchMarker, ss));

				}

				// TODO: more after this
				// As each set drops, you have to dodge
				// Not sure what the optimal call order is
				// New amrkers go out before explosion

				// Then there's stack markers at the end
			});

	@NpcCastCallout(0xA59E)
	private final ModifiableCallout<AbilityCastStart> powerSlam = ModifiableCallout.durationBasedCall("Powerslam", "Big Raidwide");


	// P3
	private final ModifiableCallout<AbilityCastStart> debrisSporePollenSafe = ModifiableCallout.<AbilityCastStart>durationBasedCall("Debris Deathmatch Pollen Safe Spots", "{safeCorners} Corners Safe")
			.extendedDescription("""
					To use inner safe spots instead of corners, use {safeInner} instead of {safeCorners}""");

	private static final ArenaPos pollenPosP3 = new ArenaPos(100, 5, 8, 8);
	private final ModifiableCallout<AbilityCastStart> debrisSporeSinisterSeedOnYou = ModifiableCallout.durationBasedCall("Sinister Seed on You", "Drop Seed");
	private final ModifiableCallout<?> debrisSporeDropPuddles = new ModifiableCallout<>("Drop Puddles", "Bait Puddles");
	private final ModifiableCallout<AbilityCastStart> debrisSporeDodgeTentril = ModifiableCallout.durationBasedCall("Dodge Tendrils", "Dodge Tendrils");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> debrisDeathmatchSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA5B0),
			(e1, s) -> {
					// TODO
				// 4 tethers from outside
				// Has the same vines as P1 with outside corners + inside intercards safe
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
				List<AbilityCastStart> markers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA59B));
				markers.stream().filter(m -> m.getTarget().isThePlayer()).findAny()
						.ifPresentOrElse(
								myMarker -> s.updateCall(debrisSporeSinisterSeedOnYou, myMarker),
								() -> s.updateCall(debrisSporeDropPuddles));
				s.waitCastFinished(casts, markers.get(0));
				AbilityCastStart tendrilCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA59C));
				s.updateCall(debrisSporeDodgeTentril, tendrilCast);
				// Same as P1
				// One stack marker
				// Proteans

			});


	// Has more jumping mechs
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
		TODO Realistically, P2 stuff should just unconditionally call glower after a jump too

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
