package gg.xp.xivsupport.events.triggers.duties.ewex;

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
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CalloutRepo(name = "EX7", duty = KnownDuty.ZeromusEx)
public class EX7 extends AutoChildEventHandler implements FilteredEventHandler {

	private XivState state;
	private ActiveCastRepository acr;
	private StatusEffectRepository buffs;
	private final ArenaPos ap = new ArenaPos(100, 100, 8, 8);

	public EX7(XivState state, ActiveCastRepository acr, StatusEffectRepository buffs) {
		this.state = state;
		this.acr = acr;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.ZeromusEx);
	}

	private final ModifiableCallout<AbilityCastStart> noxInitial = ModifiableCallout.durationBasedCallWithOffset("Abyssal Nox Cast", "Heal to Full Soon", Duration.ofMillis(4900));
	private final ModifiableCallout<BuffApplied> noxDoom = ModifiableCallout.<BuffApplied>durationBasedCall("Abyssal Nox Doom", "Heal to Full").autoIcon();
	@AutoFeed
	private final SequentialTrigger<BaseEvent> abyssalNox = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B3F),
			(e1, s) -> {
				s.updateCall(noxInitial, e1);
				var doomBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x6E9));
				s.updateCall(noxDoom, doomBuff);
				// TODO: when beta stuff merged in, cancel the call when all dooms gone
			});

	private final ModifiableCallout<AbilityCastStart> abyssalEchoesInitial = ModifiableCallout.durationBasedCall("Abyssal Echoes", "{firstSafe} then {secondSafe}");
	private final ModifiableCallout<AbilityUsedEvent> abyssalEchoesFollowup = new ModifiableCallout<>("Abyssal Echoes Followup", "Move to {secondSafe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> abyssalEchoes = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B42),
			(e1, s) -> {
				s.waitMs(200);
				s.waitThenRefreshCombatants(200);
				List<CastTracker> casts = acr.getActiveCastsById(0x8B42);
				Set<ArenaSector> firstUnsafe = casts.stream()
						.map(ct -> ap.forCombatant(state.getLatestCombatantData(ct.getCast().getSource())))
						.collect(Collectors.toSet());
				// First safe spots are the opposite of unsafe
				Set<ArenaSector> firstSafe = EnumSet.of(ArenaSector.NORTHWEST, ArenaSector.NORTH, ArenaSector.NORTHEAST);
				firstSafe.removeAll(firstUnsafe);
				// Second safe spots are the initial unsafe spots
				Set<ArenaSector> secondSafe = EnumSet.of(ArenaSector.NORTHWEST, ArenaSector.NORTH, ArenaSector.NORTHEAST);
				secondSafe.retainAll(firstUnsafe);

				s.setParam("firstSafe", firstSafe.stream().toList());
				s.setParam("secondSafe", secondSafe.stream().toList());
				s.updateCall(abyssalEchoesInitial, e1);
				var followup = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8B42));
				s.updateCall(abyssalEchoesFollowup, followup);
			});

	@NpcCastCallout(0x8B38)
	private final ModifiableCallout<AbilityCastStart> sableThread = ModifiableCallout.durationBasedCall("Sable Thread", "Line Stack - Multiple Hits");

	// TODO: abyssal echoes - sq with nox

	@NpcCastCallout(0x8B83)
	private final ModifiableCallout<AbilityCastStart> darkMatter = ModifiableCallout.durationBasedCall("Dark Matter", "Buster - 3 Hits");

	private final ModifiableCallout<AbilityCastStart> visceralWhirlNW = ModifiableCallout.durationBasedCall("Visceral Whirl NW", "Northwest/Southeast Safe");
	private final ModifiableCallout<AbilityCastStart> visceralWhirlNE = ModifiableCallout.durationBasedCall("Visceral Whirl NE", "Northeast/Southwest Safe");
	private final ModifiableCallout<AbilityCastStart> visceralWhirlNWtether = ModifiableCallout.durationBasedCall("Visceral Whirl NW", "Tethered to {buddy}, Northwest/Southeast Safe");
	private final ModifiableCallout<AbilityCastStart> visceralWhirlNEtether = ModifiableCallout.durationBasedCall("Visceral Whirl NE", "Tethered to {buddy}, Northeast/Southwest Safe");
	private final ModifiableCallout<AbilityUsedEvent> visceralWhirlMove = new ModifiableCallout<>("Visceral Whirl - Move", "Move");

	private final ModifiableCallout<AbilityCastStart> flare = ModifiableCallout.durationBasedCall("Flare", "Group Stacks");
	// Not sure what to do re: prominence spine followup. Actors do not move into position in a timeline manner.
	// Maybe the cast IDs of the followup flares themselves are enough? Nope, 8B62 seems to be everything
	private final ModifiableCallout<AbilityCastStart> flare1Move = ModifiableCallout.durationBasedCall("Flare #1 Lines", "Find Safe Spots");
	private final ModifiableCallout<AbilityCastStart> flare2MoveSparking = ModifiableCallout.durationBasedCall("Flare #2 Lines - Sparking", "Corners and Spread");
	private final ModifiableCallout<AbilityCastStart> flare2MoveBranding = ModifiableCallout.durationBasedCall("Flare #2 Lines - Branding", "Corners and Buddies");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> flares = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B60),
			// Corner flares
			(e1, s) -> {
				s.updateCall(flare, e1);
				var followup = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B62));
				s.updateCall(flare1Move, followup);
			},
			// Side-by-side flares
			(e1, s) -> {
				s.updateCall(flare, e1);
				var sparkOrBrand = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B64, 0x8B65));
				var followup = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B62));
				if (sparkOrBrand.abilityIdMatches(0x8B64)) {
					s.updateCall(flare2MoveSparking, followup);
				}
				else {
					s.updateCall(flare2MoveBranding, followup);
				}
				// Chaser HM is handled below


			});

	@PlayerHeadmarker(value = -167, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> chaser = new ModifiableCallout<>("Chasing AoE", "Chasing AoE on You");


	@NpcCastCallout(0x8B66)
	private final ModifiableCallout<AbilityCastStart> voidBio = ModifiableCallout.durationBasedCall("Void Bio", "Dodge Balls");

	private final ModifiableCallout<AbilityCastStart> bigBang1 = new ModifiableCallout<>("Big Bang - Spread", "Spread, Watch Floor");
	private final ModifiableCallout<AbilityCastStart> bigBang2 = ModifiableCallout.durationBasedCall("Big Bang - Raidwide", "Raidwide");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> bigBangSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B4C),
			(e1, s) -> {
				s.updateCall(bigBang1, e1);
				s.waitDuration(e1.getEstimatedRemainingDuration().minusSeconds(5));
				s.updateCall(bigBang2, e1);
			});

	private final ModifiableCallout<AbilityCastStart> meteorsInitial = ModifiableCallout.durationBasedCall("Meteors - Stack Middle", "Stack Middle");
	private final ModifiableCallout<TetherEvent> meteorsFirstSet = new ModifiableCallout<>("Meteors - First Set Part 1", "Stretch Tether, Avoid Meteors");
	private final ModifiableCallout<?> meteorsSecondSet = new ModifiableCallout<>("Meteors - Second Set Part 1", "Avoid Tethers");
	private final ModifiableCallout<?> meteorsFirstSetSecond = new ModifiableCallout<>("Meteors - First Set Part 1", "Avoid Tethers");
	private final ModifiableCallout<?> meteorsSecondSetSecond = new ModifiableCallout<>("Meteors - Second Set Part 2", "Stretch Tether, Avoid Meteors");
	private final ModifiableCallout<AbilityCastStart> meteorsExplosion = ModifiableCallout.durationBasedCall("Meteors - Explosion", "Avoid Explosions");
	private final ModifiableCallout<HeadMarkerEvent> meteorsWaitMiddle = new ModifiableCallout<>("Meteors - Wait Middle", "Wait Middle");

	private final ModifiableCallout<BuffApplied> accelBomb = ModifiableCallout.<BuffApplied>durationBasedCall("Acceleration Bomb", "Stop Everything").autoIcon();
	private final ModifiableCallout<BuffApplied> divisiveDark = ModifiableCallout.<BuffApplied>durationBasedCall("Divisive Dark", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> forkedLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Forked Lightning", "Don't Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> beckoningDark = ModifiableCallout.<BuffApplied>durationBasedCall("Beckoning Dark", "Stack on {event.target}").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> meteors = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B57),
			(e1, s) -> {
				// stack middle
				s.updateCall(meteorsInitial, e1);
				// First/second tether sets
				// Find first four tethers
				var initialTethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.eitherTargetMatches(cbt -> cbt.npcIdMatches(16558)), Duration.ofMillis(20));
				boolean firstGroup;
				{
					var myTether = initialTethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
					firstGroup = myTether.isPresent();
					// Update call based on whether player is tethered or not
					myTether.ifPresentOrElse(
							mt -> s.updateCall(meteorsFirstSet, mt),
							() -> s.updateCall(meteorsSecondSet));
				}
				if (firstGroup) {
					// If we were in the first group, then wait for Bind debuff to go away, then call
					s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x9D6) && br.getTarget().isThePlayer());
					s.updateCall(meteorsFirstSetSecond);
				}
				else {
					s.waitDuration(Duration.ofSeconds(7));
					// Otherwise, just wait. It's possible the player is dead and thus not involved in either group, but we need the rest of the trigger to still function
					s.updateCall(meteorsSecondSetSecond);
				}
				// Avoid explosions
				var explosionCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8D34));
				s.updateCall(meteorsExplosion, explosionCast);

				// Wait middle for chains
				var hm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -38);
				s.updateCall(meteorsWaitMiddle, hm);

				var visceral = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B43, 0x8B46));
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> true, Duration.ofMillis(250));
				Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();

				// Handle the Visceral Whirl instead of the normal trigger
				myTether.ifPresentOrElse(mt -> {
					s.setParam("buddy", mt.getTargetMatching(cbt -> !cbt.isThePlayer()));
					if (visceral.abilityIdMatches(0x8B43)) {
						s.updateCall(visceralWhirlNEtether, visceral);
					}
					else {
						s.updateCall(visceralWhirlNWtether, visceral);
					}
				}, () -> {
					if (visceral.abilityIdMatches(0x8B43)) {
						s.updateCall(visceralWhirlNE, visceral);
					}
					else {
						s.updateCall(visceralWhirlNW, visceral);
					}
				});
				{
					// Acceleration bomb
					BuffApplied buff = buffs.findBuff(ba -> ba.buffIdMatches(0xA61) && ba.getTarget().isThePlayer());
					if (buff != null) {
						s.waitDuration(buff.remainingDurationPlus(Duration.ofSeconds(-4)));
						s.updateCall(accelBomb, buff);
					}
				}
				{
					// Divisive dark
					BuffApplied buff = buffs.findBuff(ba -> ba.buffIdMatches(0xEB2));
					if (buff != null) {
						s.waitDuration(buff.remainingDurationPlus(Duration.ofSeconds(-4)));
						s.updateCall(divisiveDark, buff);
					}
				}
				{
					// Stack/Lightning
					BuffApplied buff = buffs.findBuff(ba -> ba.buffIdMatches(0xED7));
					if (buff != null) {
						s.waitDuration(buff.remainingDurationPlus(Duration.ofSeconds(-4)));
						s.updateCall(forkedLightning, buff);
					}
					else if (!state.playerJobMatches(Job::isTank)) {
						// Beconing dark
						var beckoning = buffs.findBuff(ba -> ba.buffIdMatches(0xED2));
						s.updateCall(beckoningDark, beckoning);
					}
				}


			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> visceralWhirl = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B43, 0x8B46),
			(e1, s) -> {
				if (meteors.isActive()) {
					return;
				}
				if (e1.abilityIdMatches(0x8B43)) {
					s.updateCall(visceralWhirlNE, e1);
				}
				else {
					s.updateCall(visceralWhirlNW, e1);
				}
				var followup = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8b44));
				// TODO: make actual call for safe spot
				s.updateCall(visceralWhirlMove, followup);
			});

	private final ModifiableCallout<AbilityCastStart> laserWest = ModifiableCallout.durationBasedCall("Laser: West", "Laser, Start West");
	private final ModifiableCallout<AbilityCastStart> laserEast = ModifiableCallout.durationBasedCall("Laser: East", "Laser, Start East");
	private final ModifiableCallout<AbilityCastStart> laserWestWithHole = ModifiableCallout.durationBasedCall("Laser: West with Black Hole", "Black Hole, Start West");
	private final ModifiableCallout<AbilityCastStart> laserEastWithHole = ModifiableCallout.durationBasedCall("Laser: East with Black Hole", "Black Hole, Start East");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> bigLaser = SqtTemplates.sq(30_000,
			HeadMarkerEvent.class, hm -> hm.getMarkerOffset() == -34,
			(e1, s) -> {
				XivCombatant bhPlayer = e1.getTarget();
				s.setParam("blackhole", bhPlayer);
				boolean playerHasBlackHole = bhPlayer.isThePlayer();
				AbilityCastStart laser = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B3C, 0x8B3D));
				if (laser.abilityIdMatches(0x8B3C)) {
					// start east
					if (playerHasBlackHole) {
						s.updateCall(laserEastWithHole, laser);
					}
					else {
						s.updateCall(laserEast, laser);
					}
				}
				else {
					// start west
					if (playerHasBlackHole) {
						s.updateCall(laserWestWithHole, laser);
					}
					else {
						s.updateCall(laserWest, laser);
					}
				}
			});


	// P2 stuff
	@NpcCastCallout(0x8C0D)
	private final ModifiableCallout<AbilityCastStart> rendTheRift = ModifiableCallout.durationBasedCall("Rend the Rift", "Avoid Ground AoE");

	private final ModifiableCallout<AbilityCastStart> dimensionalCleaveNorth = ModifiableCallout.durationBasedCall("Dimensional Cleave - North", "North Cleave");
	private final ModifiableCallout<AbilityCastStart> dimensionalCleaveMiddle = ModifiableCallout.durationBasedCall("Dimensional Cleave - Middle", "Middle Cleave");
	private final ModifiableCallout<AbilityCastStart> dimensionalCleaveSouth = ModifiableCallout.durationBasedCall("Dimensional Cleave - South", "South Cleave");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> dimensionalSurge = SqtTemplates.sq(10_000,
			MapEffectEvent.class, mee -> mee.getFlags() == 0x20001,
			(e1, s) -> {
				var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8B82));
				switch ((int) e1.getIndex()) {
					case 2 -> s.updateCall(dimensionalCleaveNorth, cast);
					case 3 -> s.updateCall(dimensionalCleaveMiddle, cast);
					case 4 -> s.updateCall(dimensionalCleaveSouth, cast);
				}
			});
}
