package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

@CalloutRepo(name = "M8S", duty = KnownDuty.M8S)
public class M8S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M8S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M8S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M8S);
	}

	@NpcCastCallout(0xA74F)
	private final ModifiableCallout<AbilityCastStart> extraplanarPursuit = ModifiableCallout.durationBasedCall("Extraplanar Pursuit", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> windfangCard = ModifiableCallout.durationBasedCall("Windfang", "Buddies, In, Cardinals");
	private final ModifiableCallout<AbilityCastStart> windfangInter = ModifiableCallout.durationBasedCall("Windfang", "Buddies, In, Intercards");
	private final ModifiableCallout<AbilityCastStart> stonefangCard = ModifiableCallout.durationBasedCall("Stonefang", "Spread, Out, Cardinals");
	private final ModifiableCallout<AbilityCastStart> stonefangInter = ModifiableCallout.durationBasedCall("Stonefang", "Spread, Out, Intercards");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fangSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA39F, 0xA3B0),
			(e1, s) -> {
				// Windfang A39D (Cross) + A39F (Donut) = hitting cardinals + partners
				// Stonefang A3A1 (Cross) + A3B0 (Circle) = hitting cardinals + spread
				AbilityCastStart noulithCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xA39D, 0xA39E, 0xA3A1, 0xA3A2), false);
				boolean hittingCard = noulithCast.abilityIdMatches(0xA39D, 0xA3A1);
				boolean isWindfang = e1.abilityIdMatches(0xA39F);
				if (isWindfang) {
					s.updateCall(hittingCard ? windfangInter : windfangCard, e1);
				}
				else {
					s.updateCall(hittingCard ? stonefangInter : stonefangCard, e1);
				}
			});

	private final ModifiableCallout<AbilityCastStart> eminentClones = ModifiableCallout.durationBasedCall("Eminent Reign: Dodge Clones", "Dodge Clones, Out of Middle");
	private final ModifiableCallout<AbilityCastStart> revoClones = ModifiableCallout.durationBasedCall("Revolutionary Reign: Dodge Clones", "Dodge Clones, Out of Middle");
	private final ModifiableCallout<AbilityCastStart> eminentStacks = ModifiableCallout.durationBasedCall("Eminent Reign: Away, Stacks", "Close Stacks");
	private final ModifiableCallout<AbilityCastStart> revoStacks = ModifiableCallout.durationBasedCall("Revolutionary Reign: Away, Stacks", "Far Stacks");
	private final ModifiableCallout<AbilityCastStart> eminentStacksWithLines = ModifiableCallout.durationBasedCall("Eminent Reign: Away, Stacks", "Close Stacks, Dodge Lines");
	private final ModifiableCallout<AbilityCastStart> revoStacksWithLines = ModifiableCallout.durationBasedCall("Revolutionary Reign: Away, Stacks", "Far Stacks, Dodge Lines");
	private final ModifiableCallout<AbilityCastStart> reignsDodgeHeads = ModifiableCallout.durationBasedCall("Reign: Dodge Lines", "Dodge Lines");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> reignsSq = SqtTemplates.selfManagedMultiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA930, 0xA931),
			(e1, s, index) -> {
				/*
				Revo: A913 + A931

				Boss can jump to different clones. It is technically possible to differentiate, but is there really much
				vaue?
				 */
				boolean isRevolutionary = e1.abilityIdMatches(0xA931);
				s.updateCall(isRevolutionary ? revoClones : eminentClones, e1);
				s.waitCastFinished(casts, e1);
				if (index < 2) {
					s.updateCall(isRevolutionary ? revoStacks : eminentStacks, e1);
				}
				else {
					s.updateCall(isRevolutionary ? revoStacksWithLines : eminentStacksWithLines, e1);
					// There is a pair of Weal of Stone with these. A78E
					AbilityCastStart weal = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0xA78D, 0xA78E), false);
					s.updateCall(reignsDodgeHeads, weal);
				}
			});

	// TODO: should KB be called, or would the calls be too busy?
	@NpcCastCallout(0xA3B2)
	private final ModifiableCallout<AbilityCastStart> millennialDecay = ModifiableCallout.durationBasedCall("Millennial Decay", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> millennialPantoCW = ModifiableCallout.durationBasedCall("Millennial Decay: Clockwise Panto", "Clockwise");
	private final ModifiableCallout<AbilityCastStart> millennialPantoCCW = ModifiableCallout.durationBasedCall("Millennial Decay: Counter-Clockwise Panto", "Counter-Clockwise");
	private final ModifiableCallout<HeadMarkerEvent> millennialSpread = new ModifiableCallout<>("Millennial Decay: Spread Marker", "Spread");
	private final ModifiableCallout<TetherEvent> millennialTether = new ModifiableCallout<>("Millennial Decay: Tether", "Tether");
	private final ModifiableCallout<TetherEvent> millennialNoTether = new ModifiableCallout<>("Millennial Decay: No Tether", "Soak Tower");


	@AutoFeed
	private final SequentialTrigger<BaseEvent> millennialDecaySq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3B2),
			(e1, s) -> {
				// There's only one millenial - how to determine rotation direction?
				// Breath of Decay A3B4 - the lines down the middle. use cast angle to determine CW/CCW?
				// Aero III (A3B7 and A3B8 simultaneously) is the KB?
				CastLocationDataEvent first = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3B4));
				CastLocationDataEvent second = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3B4));
				var firstPos = first.getPos();
				var secondPos = second.getPos();
				Position normalized = secondPos.normalizedTo(firstPos);
				log.info("Normalized: {} -> {} = {} : {}", firstPos, secondPos, normalized.heading(), normalized.northUpClockwiseHeading());
				boolean clockwise = normalized.heading() > 0;
				s.updateCall(clockwise ? millennialPantoCW : millennialPantoCCW, first.originalEvent());

				for (int i = 1; i <= 2; i++) {
					List<HeadMarkerEvent> spreadMarkers = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 0);
					spreadMarkers.stream().filter(hm -> hm.getTarget().isThePlayer())
							.findAny()
							.ifPresent(hm -> {
								s.updateCall(millennialSpread, hm);
							});
				}
				List<TetherEvent> tethers = s.waitEvents(4, TetherEvent.class, te -> te.tetherIdMatches(0x39));
				tethers.stream().filter(hm -> hm.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(myTether -> {
							s.updateCall(millennialTether, myTether);
						}, () -> {
							s.updateCall(millennialNoTether);
						});
			});

	@NpcCastCallout(0xA3B9)
	private final ModifiableCallout<AbilityCastStart> trackingTremors = ModifiableCallout.durationBasedCall("Tracking Tremors", "Multi Stack");

	@NpcCastCallout(0xA3D8)
	private final ModifiableCallout<AbilityCastStart> greatDivide = ModifiableCallout.durationBasedCall("Great Divide", "Tank Buster on {event.target}");

	private static final ArenaPos towersAp = new ArenaPos(100, 100, 5, 5);

	private final ModifiableCallout<AbilityCastStart> terrestrialTitansSafeSpot = ModifiableCallout.durationBasedCall("Terrestrial Titans Safe Spot", "{safe} Safe");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> terrestrialTitansSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3C4),
			(e1, s) -> {
				// The two tower casts, but it's actually 2 casts each, so 4 total
				var towerCasts = s.waitEvents(4, CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3C6));
				var crossCasts = s.waitEvents(2, ActorControlExtraEvent.class, acee -> acee.getTarget().npcIdMatches(18221));
				s.waitThenRefreshCombatants(100);
				// Assumptions:
				// Towers can only be cardinal
				// Fanged is the opposite cardinals from towers
				// Safe spot is always an intercard
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.quadrants);
				towerCasts.stream()
						.map(CastLocationDataEvent::getPos)
						// The forward amount can be excessive, it isn't important
						.map(pos -> pos.translateRelative(0, 100))
						.map(towersAp::forPosition)
						.forEach(safe::remove);

				crossCasts.stream()
						.map(ActorControlExtraEvent::getTarget)
						.map(XivCombatant::getPos)
						.forEach(pos -> {
							var at = towersAp.forPosition(pos);
							var facing = ArenaPos.combatantFacing(pos);
							// The safe spot is always near the one casting towards intercards
							// Thus, if one of them is facing cardinals, we need to remove the two potential sfe spots near it
							if (facing.isCardinal()) {
								safe.remove(at.plusEighths(1));
								safe.remove(at.plusEighths(-1));
							}
						});

				if (safe.size() == 1) {
					s.setParam("safe", safe.iterator().next());
				}
				else {
					log.error("Invalid safe spots: {}", safe);
				}
				s.updateCall(terrestrialTitansSafeSpot, towerCasts.get(0).originalEvent());
			});
	/*
	P1

		Fangs
		Windfang = Conal pair stack, in
		Stonefang = Conal spread, out
		His floating nouliths thingos determine whether it's a cardinal dodge or intercardinal dodge

		Reigns
		Revolutionary Reign = Dodge initial clones, dodge line dash, then AWAY from boss (large chariot) into T/HDD/HDD/T conal stacks
		Eminent Reign = Dodge initial clones, dodge line dash, then CLOSE to boss (frontal 150deg cone) into T/HDD/HDD/T conal stacks

		"Pantokrator" Part 1
		First dragon head spawns either N or S and subsequent heads spawn in either a CW or CCW direction, boss casts a knockback.
		For my group we always orient to the first head and make that our relative north, split light parties left/right of the head.
		Either all supports or all DPSes get marked with first puddle drops.
		These puddle drops are always placed in intercardinals.
		Those not marked for first puddle drops get knocked to the safe cardinals.
		Once first puddles are dropped, the other roles get marked for puddles, while heads are going off in a CW/CCW direction.
		Depending on orientation, the next puddles are placed in the adjacent intercardinals in the direction of travel
			(e.g. if first puddles are placed NW and SW, and the heads go in a CCW direction, second puddles are placed SW and SE).

		"Pantokrator" Part 2
		Boss summons 4 dragon heads and casts another knockback.
		Either all DPSes or all supports get tethered to a head and need to stretch the tether directly opposite,
			but also stand behind the head they are stretching the tether to, to avoid the conal cleave going out directly opposite.
			The other role soaks a tower in the free cardinals/intercardinals. We colour code the tower soaks.

		Tracking Tremors
		8x heavy hitting party stack.

		Great Divide
		Tank-stack line cleave.

		Terrestrial Titans
		Slice Is Right mechanic.
		However this time there are only two permutations - from the pillars alone, pairs of directly opposite intercardinals are safe.
		These pairs are determined by where the slice on the pillar points to.
		If, at an intercardinal, you are able to see the bright blue lines bisecting the two pillars, then you are in a potential safe spot.
		If you are unable to see the bright blue lines, then the other pair is safe.
		The safe pair of intercardinals is then narrowed down even further to only one safe spot based on the floating noulith thingos.
		If the beams point to the intercardinals near you, then the opposite side is safe.
	*/

	private final ModifiableCallout<AbilityCastStart> tacticalPack = ModifiableCallout.durationBasedCall("Tactical Pack", "Adds");
	private final ModifiableCallout<TetherEvent> addsTether = new ModifiableCallout<TetherEvent>("Tactical Pack: Tether", "Tethered to {tetherLocation}")
			.extendedDescription("""
					To call where you need to go, change to { tetherLocation.opposite() }""");

	private final ModifiableCallout<?> addsClockwise = new ModifiableCallout<>("Tactical Pack: Clockwise", "Clockwise");
	private final ModifiableCallout<?> addsCcw = new ModifiableCallout<>("Tactical Pack: Counter-Clockwise", "Counter-Clockwise");
	// TODO: some strats DO NOT stack the cleave, they just have one person point it out of the arena.
	// Thus, there is value in knowing whether the cleave (headmarker) is on you or someone else.
	private final ModifiableCallout<BuffApplied> addsGreenBuffShort = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Short Green Debuff", "Touch Green, First").autoIcon();
	private final ModifiableCallout<BuffApplied> addsYellowBuffShort = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Short Yellow Debuff", "Touch Yellow, First").autoIcon();
	private final ModifiableCallout<BuffApplied> addsGreenBuffMed = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Medium Green Debuff", "Touch Green, Second").autoIcon();
	private final ModifiableCallout<BuffApplied> addsYellowBuffMed = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Medium Yellow Debuff", "Touch Yellow, Second").autoIcon();
	private final ModifiableCallout<BuffApplied> addsGreenBuffLong = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Long Green Debuff", "Touch Green, Third").autoIcon();
	private final ModifiableCallout<BuffApplied> addsYellowBuffLong = ModifiableCallout.<BuffApplied>durationBasedCall("Tactical Pack: Long Yellow Debuff", "Touch Yellow, Third").autoIcon();
	private final ModifiableCallout<BuffApplied> addsAttackGreen = new ModifiableCallout<BuffApplied>("Tactical Pack: Attack Green", "Attack Green").autoIcon();
	private final ModifiableCallout<BuffApplied> addsAttackYellow = new ModifiableCallout<BuffApplied>("Tactical Pack: Attack Yellow", "Attack Yellow").autoIcon();

	@NpcCastCallout(value = 0xA3CC, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> packPredation = ModifiableCallout.durationBasedCall("Tactical Pack: Pack Predation", "Cleaves");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> tacticalPackSq = SqtTemplates.sq(180_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3C8),
			(e1, s) -> {
				s.updateCall(tacticalPack, e1);
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.tetherIdMatches(0x14F, 0x150));
				tethers.stream().filter(hm -> hm.eitherTargetMatches(XivCombatant::isThePlayer))
						.findAny()
						.ifPresent(t -> {
							s.waitThenRefreshCombatants(100);
							var where = towersAp.forCombatant(state.getLatestCombatantData(t.getTargetMatching(cbt -> !cbt.isPc())));
							s.setParam("tetherLocation", where);
							s.updateCall(addsTether, t);
						});

				List<BuffApplied> packBuffs = s.waitEventsQuickSuccession(6, BuffApplied.class, ba -> ba.buffIdMatches(0x1127, 0x1128));


				// Rotation - don't stop and wait for this if the callouts are disabled
				if (addsClockwise.isEnabled() || addsCcw.isEnabled()) {
					for (int i = 0; i < 20; i++) {
						s.waitThenRefreshCombatants(100);
						XivCombatant yellowOrb = state.npcById(18262);
						XivCombatant yellowHead = state.npcById(18225);
						if (yellowHead == null || yellowOrb == null) {
							continue;
						}
						// You can imagine it like the orb chases the head
						ArenaSector orbSec = towersAp.forCombatant(yellowOrb);
						ArenaSector headSec = towersAp.forCombatant(yellowHead);
						if (orbSec.isCardinal() && headSec.isCardinal()) {
							int eighths = orbSec.eighthsTo(headSec);
							if (eighths == 0 || eighths == 4) {
								continue;
							}
							if (eighths > 0) {
								s.call(addsClockwise);
							}
							else {
								s.call(addsCcw);
							}
							s.waitMs(1_200);
							break;
						}
					}
				}

				packBuffs.stream().filter(buff -> buff.getTarget().isThePlayer())
						.findAny()
						.ifPresent(myBuff -> {
							boolean isGreen = myBuff.buffIdMatches(0x1128);
							long secs = myBuff.getInitialDuration().toSeconds();
							if (secs < 30) {
								s.updateCall(isGreen ? addsGreenBuffShort : addsYellowBuffShort, myBuff);
							}
							else if (secs < 45) {
								s.updateCall(isGreen ? addsGreenBuffMed : addsYellowBuffMed, myBuff);
							}
							else {
								s.updateCall(isGreen ? addsGreenBuffLong : addsYellowBuffLong, myBuff);
							}
							s.waitBuffRemoved(buffs, myBuff);
						});
				// If you are either a tank and didn't get a timed debuff, or you already handled yours, then call out which mob to attack
				// 1125 = Green Debuff, Attack Yellow
				// 1126 = Yellow Debuff, Attack Green
				s.waitMs(100);
				BuffApplied myBuff = s.findOrWaitForBuff(buffs,
						ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0x1125, 0x1126));
				s.updateCall(myBuff.buffIdMatches(0x1125) ? addsAttackYellow : addsAttackGreen, myBuff);

				// TODO: adds enrage? A3D0 + A3D3
			});


	/*
	Adds
		Everyone gets tethered to either a green head or a yellow head.
		Stretch tethers over to the other head, i.e. if you get tethered to yellow head, stretch and you'll be hitting green head and vice versa.
		Two coloured shapes will spawn - a green orb or a yellow cube.
		Non-tanks will get a coloured, timed debuff that is cleansed by walking through the shape of the same colour.
		This also changes the 'polarity' of whoever just walked through the shape, so after walking through the shape
			they now switch target over to the head they weren't hitting earlier.
		Walking through a shape gives everyone a vuln up, so shape pops have to be staggered.

		Tanks will always want to drag their head in the direction of the differently-coloured shape - the tank pulling
			the yellow head will chase the green orb, vice versa. This can be simplified into a CW/CCW callout.
		When head is getting pulled, prey marker goes on a non-tank on each side and goes off at the same time as a line tankbuster cleave goes on the tank.
		Technically the prey marker is a single-targeted line cleave, but the rest of the non-tanks on each side can soak it together as damage is non-lethal.

		At the end of all 3 sets of shape pops, only the tanks remain on the same head from the original group.
		By this point, either or both heads are close to dying, so keep DPS up until they're dead.
		Tanks can also bring them towards each other for easier target switching once either dies.
	*/

	@NpcCastCallout(0xA749)
	private final ModifiableCallout<AbilityCastStart> ravenousSaber = ModifiableCallout.durationBasedCall("Ravenous Saber", "Multiple Raidwides");

	private final ModifiableCallout<HeadMarkerEvent> terrestrialRageSpread = new ModifiableCallout<>("Terrestrial Rage Spread", "Spread Between Lines");
	private final ModifiableCallout<?> terrestrialRageStack = new ModifiableCallout<>("Terrestrial Rage Stack", "Stack Between Lines");
	private final ModifiableCallout<?> terrestrialRageMove = new ModifiableCallout<>("Terrestrial Rage Move", "Move");
	private final ModifiableCallout<HeadMarkerEvent> shadowchaseSpread = new ModifiableCallout<>("Shadowchase Spread", "Spread Behind Adds");
	private final ModifiableCallout<?> shadowchaseStack = new ModifiableCallout<>("Shadowchase Stack", "Stack Behind Adds");
	private final ModifiableCallout<?> shadowchaseMove = new ModifiableCallout<>("Shadowchase Move", "Rotate");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> terrestrialRageShadowchaseSq = SqtTemplates.selfManagedMultiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3BE),
			(e1, s, index) -> {
				// We only care about the first one
				if (index > 0) {
					return;
				}
				// Has a fanged charge A3D6, there's some headmarkers and stuff
				// spread is HM 139 -237 and stack is HM 93 -283
				// Staggered pairs of Fanged Charge A3D6
				{
					List<HeadMarkerEvent> spreads = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -237);
					spreads.stream().filter(headMarker -> headMarker.getTarget().isThePlayer())
							.findAny()
							.ifPresentOrElse(
									myHm -> s.updateCall(terrestrialRageSpread, myHm),
									() -> s.updateCall(terrestrialRageStack));
					// Calling this immediately after the *lines* go off is too early. We should wait for the actual stack/spread markers to resolve.
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA3BF, 0xA3C0));
					s.updateCall(terrestrialRageMove);
				}
				// Shadowchase
				{
					List<HeadMarkerEvent> spreads = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -237);
					// This comes out pretty early, so delay

					spreads.stream().filter(headMarker -> headMarker.getTarget().isThePlayer())
							.findAny()
							.ifPresentOrElse(
									myHm -> s.updateCall(shadowchaseSpread, myHm),
									() -> s.updateCall(shadowchaseStack));
					// Wait for first set of Shadowchase to go off
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA3BD));
					s.updateCall(shadowchaseMove);
				}
			});

	private static final ArenaPos moonlightAp = new ArenaPos(100, 100, 5, 5);

	private static ArenaSector safeSpotForMoonlight(CastLocationDataEvent moonlightCast) {
		// e.g. a north cast is 112,90 heading -1.57 (west)
		// so if we move "forward" 12, that gives us 100,90, i.e. north
		// This is the place getting hit, so we need to instead use the opposite
		return moonlightAp.forPosition(moonlightCast.getPos().translateRelative(0, 12)).opposite();
	}

	private final ModifiableCallout<AbilityCastStart> moonlightFirst = new ModifiableCallout<>("Moonlight First", "Start {safeSpots[0]}", "{safeSpots[i..-1]}", ModifiableCallout.expiresIn(10));
	private final ModifiableCallout<AbilityCastStart> moonlightRemaining = new ModifiableCallout<>("Moonlight Remaining", "{safeSpots[i]}", "{safeSpots[i..-1]}");
	private final ModifiableCallout<?> moonlightStack = new ModifiableCallout<>("Moonlight Stack", "Stack");
	private final ModifiableCallout<?> moonlightSpread = new ModifiableCallout<>("Moonlight Spread", "Spread");
	private final ModifiableCallout<?> moonlightWealCardSafe = new ModifiableCallout<>("Moonlight: Weal, Cardinal Safe", "Cardinals");
	private final ModifiableCallout<?> moonlightWealInterSafe = new ModifiableCallout<>("Moonlight: Weal, Intercard Safe", "Intercards");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> beckonMoonlightSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3C1),
			(e1, s) -> {
				// There are actually three tells for each clone
				// First, they use either action A3E0 (cleaving right) or A3E1 (left)
				// They also get an actor control extra with 3F 6:0 (right) or 3F 7:0 (left)
				// Finally, they case Moonbeam's Bite A3C2 (right) or A3C3 (left)
				// However, the cast is proabably the most reliable because they happen to be in position already for that
				// The initial actions seem to position them
				List<AbilityCastStart> allCasts = new ArrayList<>();
				List<ArenaSector> safe = new ArrayList<>();
				s.setParam("safeSpots", safe);
				s.setParam("i", 0);
				var first = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3C2, 0xA3C3));
				ArenaSector firstAs = safeSpotForMoonlight(first);
				allCasts.add(first.originalEvent());
				safe.add(firstAs);
				s.updateCall(moonlightFirst, first.originalEvent());
				for (int i = 0; i < 3; i++) {
					var next = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3C2, 0xA3C3));
					allCasts.add(next.originalEvent());
					safe.add(safeSpotForMoonlight(next));
				}

				for (int i = 0; i < allCasts.size(); i++) {
					s.setParam("i", i);
					s.updateCall(moonlightRemaining, allCasts.get(i));
					var cast = allCasts.get(i);
					if (i < allCasts.size() - 1) {
						// If we want on the final one, then we miss the Weal events
						s.waitCastFinished(casts, cast);
					}
				}

				// There are four weals at the end, hitting cards or intercards (opposite is safe)
				ActorControlExtraEvent weal = s.waitEvent(
						ActorControlExtraEvent.class,
						acee -> acee.getCategory() == 0x197
						        && acee.getTarget().npcIdMatches(18507));
				s.waitThenRefreshCombatants(100);
				ArenaSector wealFacing = ArenaPos.combatantFacing(state.getLatestCombatantData(weal.getTarget()));
				if (wealFacing.isCardinal()) {
					s.updateCall(moonlightWealInterSafe);
				}
				else {
					s.updateCall(moonlightWealCardSafe);
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> beckonMoonlightHeadmarkSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3C1),
			(e1, s) -> {
		// TODO: is this used?
				// scream test
				if (true) return;
				// For the headmarkers, we can call the first one as soon as it comes out
				{
					List<HeadMarkerEvent> spreads = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -237);
					spreads.stream().filter(headMarker -> headMarker.getTarget().isThePlayer()).findAny().ifPresentOrElse(myHm -> s.updateCall(moonlightSpread), () -> s.updateCall(moonlightStack));
				}
				{
					List<HeadMarkerEvent> spreads = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -237);
					// We need to not talk all over one of the directional calls
					s.waitMs(1200);
					spreads.stream().filter(headMarker -> headMarker.getTarget().isThePlayer()).findAny().ifPresentOrElse(myHm -> s.updateCall(moonlightSpread), () -> s.updateCall(moonlightStack));
				}
			});

	// Enrage: it goes untargetable first, no useful callout

	/*
	Post-Adds
		Ravenous Saber
		Big raidwide, multiple hits. Mits only after both dragon heads fuse.


		Terrestrial Rage
		Two noulith beams render two 'lanes' unsafe first.
		4x spread markers and 1x stack marker appear - they are resolved in the safe spot before immediately moving over to the unsafe spot.
		Spreads are either all supports or all DPSes.

		Shadowchase
		The opposite set happens from Terrestrial Rage.
		If supports got spreads earlier, they get stacks now and vice versa.
		5 shadows spawn and point to and through each other - AoEs resemble a * shape so there is enough space for 4 spreads and 1 stack marker.
		5 dragon heads also spawn, the AoEs also resemble the same shape so players rotate to be behind a head.

		Reign afterwards, but with additional dragon heads rendering two 'lanes' unsafe.
		Dodge heads after conal stacks/busters.

		Beckon Moonlight
		Clones with half room cleaves spawn.
		Clones always resolve in spawn order.
		Check which halves of the arena the clones cleave in order.
		Same deal with 4x spread markers and 1x stack markers.
		One role will be picked for spreads on the first set, and the roles are reversed for the second.
		First set of markers resolve during the first clone's cleave, while second set of markers resolve during the last clone's cleave.

		4 dragon heads spawn immediately after, rendering two middle lanes unsafe on adjacent cardinals/intercardinals in an X shape.
		For safe spots, players return to their colour-coded assignments as it is immediately proceeded by a Stonefang (out+spread) or Windfang (in+stack).

		Tracking Tremors (8x heavy stacks) next, then another raidwide into enrage.
	*/

	// P2

	// 1129 Terrestrial Chains Debuff: unable to jump

	// HM 93 -283
	// Quake III A45A - stack?
	@NpcCastCallout(0xA45A)
	private final ModifiableCallout<AbilityCastStart> quakeIII = ModifiableCallout.durationBasedCall("Quake III", "Stacks");

	// TODO: these should probably call out whether *you* are marked or not
	private final ModifiableCallout<?> ultraviolentRayOneSupportFourDps = new ModifiableCallout<>("Ultraviolent Ray: One Support, Four DPS", "DPS Marked");
	private final ModifiableCallout<?> ultraviolentRayOneDpsFourSupport = new ModifiableCallout<>("Ultraviolent Ray: One DPS, Four Support", "Supports Marked");
	private final ModifiableCallout<?> ultraviolentRayOneOtherConfig = new ModifiableCallout<>("Ultraviolent Ray: One DPS, Four Support", "{supportCount} Supports, {dpsCount} DPS");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ultraviolentRaySq = SqtTemplates.sq(120_000,
			// Use the first marker as the start condition
			HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -362,
			(e1, s) -> {
				List<HeadMarkerEvent> extraMarkers = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -362);
				List<HeadMarkerEvent> markers = new ArrayList<>();
				markers.add(e1);
				markers.addAll(extraMarkers);
				int supportCount = 0;
				int dpsCount = 0;
				for (HeadMarkerEvent marker : markers) {
					XivPlayerCharacter target = (XivPlayerCharacter) marker.getTarget();
					Job job = target.getJob();
					if (job.isDps()) {
						dpsCount++;
					}
					else {
						supportCount++;
					}
				}
				s.setParam("supportCount", supportCount);
				s.setParam("dpsCount", dpsCount);
				// Happy path 1
				if (supportCount == 1 && dpsCount == 4) {
					s.updateCall(ultraviolentRayOneSupportFourDps);
				}
				// Happy path 2
				else if (supportCount == 4 && dpsCount == 1) {
					s.updateCall(ultraviolentRayOneDpsFourSupport);
				}
				// People dead, it got messed up, etc
				else {
					s.updateCall(ultraviolentRayOneOtherConfig);
				}
				// TODO: gleaming beam - call out left vs right on your platform I guess?
				// Casts multiple Gleaming Beam A45E - these need to be dodged left vs right, but it's unique to each platform
				// probably not enough time to check the platform since someone might move last-minute

			});
	// Casts Ultraviolent Ray A45C
	// A4CD Twinbite - tankbuster?
	@NpcCastCallout(0xA4CD)
	private final ModifiableCallout<AbilityCastStart> twinbite = ModifiableCallout.durationBasedCall("Twinbite", "Double Tankbuster");

	// TODO: these *can* actually call the cleave direction. It's either W or E.
	@NpcCastCallout(0xA463)
	private final ModifiableCallout<AbilityCastStart> fangedMaw = ModifiableCallout.durationBasedCall("Fanged Maw", "Out, Watch Cleave");
	@NpcCastCallout(0xA464)
	private final ModifiableCallout<AbilityCastStart> fangedPerimeter = ModifiableCallout.durationBasedCall("Fanged Perimeter", "In, Watch Cleave");

	// TODO: this could call the location
	private final ModifiableCallout<AbilityCastStart> moonCleaver = ModifiableCallout.durationBasedCall("Mooncleaver", "Off Platform, Cleave");
	private final ModifiableCallout<HeadMarkerEvent> moonCleaverHasMarker = new ModifiableCallout<>("Mooncleaver", "Marker on YOU");
	private final ModifiableCallout<?> moonCleaverMarkersNoMarker = new ModifiableCallout<>("Mooncleaver", "Markers on {targets}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> moonCleaverSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA466),
			(e1, s) -> {
				s.updateCall(moonCleaver, e1);
				List<HeadMarkerEvent> markers = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -353);
				List<XivCombatant> targets = markers.stream().map(HeadMarkerEvent::getTarget).toList();
				s.setParam("targets", targets);
				markers.stream()
						.filter(marker -> marker.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(myMark -> s.updateCall(moonCleaverHasMarker, myMark),
								() -> s.updateCall(moonCleaverMarkersNoMarker));
			});


	@NpcCastCallout(0xA46E)
	private final ModifiableCallout<AbilityCastStart> prowlingGale = ModifiableCallout.durationBasedCall("Prowling Gale", "Partner Towers");

	private final ModifiableCallout<TetherEvent> twofoldTempestCurrentTether = new ModifiableCallout<>("Twofold Tempest, Location Changed", "{currentTetherLocation}", "Tether on {currentTetherHolder}, {currentTetherLocation}", ModifiableCallout.expiresIn(10));
	private final ModifiableCallout<TetherEvent> twofoldTempestCurrentTetherOnlyPlayerChanged = new ModifiableCallout<TetherEvent>("Twofold Tempest, Only Player Changed", "", "Tether on {currentTetherHolder}, {currentTetherLocation}", ModifiableCallout.expiresIn(10))
			.extendedDescription("""
					The 'Location Changed' callout is used if the location of the tether changes to a different platform for any reason.
					This one will trigger if it swaps to a different player on the same platform.
					As such, it defaults to no TTS so that it does not spam.""");

	private volatile @Nullable TetherEvent twofoldLastTether;

	@HandleEvents
	public void twofoldTempestTetherHelper(TetherEvent te) {
		if (te.tetherIdMatches(0x54) && te.eitherTargetMatches(XivCombatant::isPc)) {
			log.info("New twofold tempest tether: {}", te);
			twofoldLastTether = te;
		}
	}

	private static ArenaSector getP2platform(Position pos) {
		// TODO: double check these values
		double x = pos.x();
		double y = pos.y();
		if (y < 95) {
			if (x < 100) {
				return ArenaSector.NORTHWEST;
			}
			else {
				return ArenaSector.NORTHEAST;
			}
		}
		else if (x < 90) {
			return ArenaSector.SOUTHWEST;
		}
		else if (x > 110) {
			return ArenaSector.SOUTHEAST;
		}
		else {
			return ArenaSector.SOUTH;
		}

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twofoldTempestTetherHelper = SqtTemplates.sq(120_000,
			// Start on "Rise of the Howling Wind"
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA82A),
			(e1, s) -> {
				twofoldLastTether = null;
				XivCombatant lastTetherHolder = null;
				ArenaSector lastTetherLocation = null;

				// 40 seconds * 10 checks per second
				int loops = 40 * 10;
				for (int i = 0; i < loops; i++) {
					s.waitMs(100);
					TetherEvent newTether = twofoldLastTether;
					if (newTether == null) {
						continue;
					}
					XivCombatant newTetherHolder = state.getLatestCombatantData(newTether.getTargetMatching(XivCombatant::isPc));
					ArenaSector newTetherLocation = getP2platform(newTetherHolder.getPos());
					// Check if different
					if (lastTetherLocation != newTetherLocation
					    || !Objects.equals(lastTetherHolder, newTetherHolder)) {
						s.setParam("currentTether", newTether);
						s.setParam("currentTetherHolder", newTetherHolder);
						s.setParam("currentTetherLocation", newTetherLocation);
						if (lastTetherLocation == newTetherLocation) {
							// Do the "reduced" call if only the holding player changed
							s.updateCall(twofoldTempestCurrentTetherOnlyPlayerChanged, newTether);
						}
						else {
							// Otherwise do the "full" call
							s.updateCall(twofoldTempestCurrentTether, newTether);
						}
					}
					lastTetherHolder = newTetherHolder;
					lastTetherLocation = newTetherLocation;
				}
			});


	private final ModifiableCallout<AbilityCastStart> championsCircuitCw = ModifiableCallout.durationBasedCall("Champion's Circuit: Clockwise", "Clockwise");
	private final ModifiableCallout<AbilityCastStart> championsCircuitCcw = ModifiableCallout.durationBasedCall("Champion's Circuit: Counter-Clockwise", "Counter-Clockwise");

	private final ModifiableCallout<?> championsCircuitIn = new ModifiableCallout<>("Champion's Circuit: In", "In {rightSafe ? 'Right' : 'Left'}");
	private final ModifiableCallout<?> championsCircuitOut = new ModifiableCallout<>("Champion's Circuit: OUt", "Out {rightSafe ? 'Right' : 'Left'}");
	private final ModifiableCallout<?> championsCircuitSides = new ModifiableCallout<>("Champion's Circuit: Sides", "Sides {rightSafe ? 'Right' : 'Left'}");
	private final ModifiableCallout<?> championsCircuitCenter = new ModifiableCallout<>("Champion's Circuit: Center", "Center {rightSafe ? 'Right' : 'Left'}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> championsCircuitSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA477, 0xA478),
			(e1, s) -> {
				// Individual cast IDs:
				/*
				A477,78: Initial cast
				A479,7E: 30x12 rectangle (sides)
				A47A,7F: 13y Donut
				A47B,7D,80,82 28y Donut
				A47C,81: out (cone)
				IDs from 79 to 7D are the long casts, everything above that is the short casts

				Okay, I haven't seen this before - the '28y donut' is actually a cone-donut, like the intersection of a
				donut and a cone with the tip of the cone at the center of the donut.

				One example:
				CW A477
				A47D big donut at center facing NW
				A47C out at SW
				A47B big donut at center facing S
				A47A small donut at SE
				A479 sides at NE


				In, Side, Donut, Right
				 */
				// Establish cw vs ccw
				boolean cw = e1.abilityIdMatches(0xA477);
				s.updateCall(cw ? championsCircuitCw : championsCircuitCcw, e1);
				// Wait for the five mechanics
				List<CastLocationDataEvent> individualCasts = s.waitEvents(5, CastLocationDataEvent.class, acs -> {
					long id = acs.getAbility().getId();
					return id >= 0xA479 && id <= 0xA47D;
				});
				// Figure out where each of the five mechanics is
				Map<ArenaSector, XivAbility> sectorMap = new EnumMap<>(ArenaSector.class);
				for (CastLocationDataEvent cast : individualCasts) {
					Position pos = cast.getPos();
					// Cast from center with angle
					ArenaSector sector;
					if (pos.distanceFrom2D(Position.of2d(100, 100)) < 1) {
						sector = getP2platform(pos.translateRelative(0, 117));
					}
					else {
						sector = getP2platform(pos);
					}
					sectorMap.put(sector, cast.getAbility());
				}
				// I don't like relying on entity IDs, but this is by far the easiest way of going about this
				// The Left-hand gleaming fang has a lower ID than the right-hand.
				OptionalLong firstFang = state.npcsById(18223).stream().mapToLong(XivCombatant::getId).min();
				if (firstFang.isEmpty()) {
					log.error("could not find fangs!");
				}
				long firstFangId = firstFang.orElse(-1);
				// Now go through the actual mechanics
				for (int i = 0; i < 5; i++) {
					// Wait for gleaming casts
					List<CastLocationDataEvent> gleamingBarrages = s.waitEventsQuickSuccession(5,
							CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA476));
					// Platform where the player is currently
					ArenaSector mySector = getP2platform(state.getPlayer().getPos());
					// Find the barrage on our platform
					CastLocationDataEvent myBarrage = gleamingBarrages.stream().filter(barrage -> getP2platform(barrage.getPos()) == mySector)
							.findAny()
							.orElseGet(() -> {
								log.error("No barrage found for {}", mySector);
								return null;
							});
					// Left/right safe
					if (myBarrage != null && firstFangId != -1) {
						if ((myBarrage.getSource().getId() - firstFangId) % 2 == 0) {
							// Hitting left side
							s.setParam("rightSafe", true);
						}
						else {
							s.setParam("rightSafe", false);
						}
					}
					else {
						s.setParam("rightSafe", "error");
					}
					ArenaSector checkSector = mySector;
					for (int j = 0; j < i; j++) {
						// If the mechanic is rotating CW, than that means the next mechanic we need to do is the one that was CCW of us
						do {
							checkSector = checkSector.plusEighths(cw ? -1 : 1);
						}
						// However, we potentially need to go "one more" because not every eighth has a platform
						while (!sectorMap.containsKey(checkSector));
					}
					XivAbility action = sectorMap.get(checkSector);
					ModifiableCallout<?> call = switch ((int) action.getId()) {
						case 0xA479 -> championsCircuitSides;
						case 0xA47A -> championsCircuitCenter;
						case 0xA47B, 0xA47D -> championsCircuitIn;
						case 0xA47C -> championsCircuitOut;
						default -> null;
					};
					if (call != null) {
						s.updateCall(call);
					}

				}
			});

	// Rise of the Hunter's Blade A82C


	// For towers mechanic, make one callout for each possible 2-player tower location
	// 3-tower is always fixed south, so that covers every combination

	// TODO: these didn't work

	private final ModifiableCallout<BuffApplied> wolfsLamentLongTetherToDps = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Long Tether with DPS", "Long Tether with {partner}").autoIcon();
	private final ModifiableCallout<BuffApplied> wolfsLamentShortTetherToDps = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Short Tether with DPS", "Short Tether with {partner}").autoIcon();
	private final ModifiableCallout<BuffApplied> wolfsLamentLongTetherToHealer = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Long Tether with Healer", "Long Tether with {partner}").autoIcon();
	private final ModifiableCallout<BuffApplied> wolfsLamentShortTetherToHealer = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Short Tether with Healer", "Short Tether with {partner}").autoIcon();
	private final ModifiableCallout<BuffApplied> wolfsLamentLongTetherToTank = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Long Tether with Tank", "Long Tether with {partner}").autoIcon();
	private final ModifiableCallout<BuffApplied> wolfsLamentShortTetherToTank = ModifiableCallout.<BuffApplied>durationBasedCall("Lone Wolf's Lament: Short Tether with Tank", "Short Tether with {partner}").autoIcon();
	private final ModifiableCallout<AbilityCastStart> wolfsLamentTwoTower = ModifiableCallout.durationBasedCall("Lone Wolf's Lament: Two-Player Tower Location", "2-Tower {twoTower}");

	// Rotating CW or CCW, there are lasers + in/out/donut/etc while that happens
	@AutoFeed
	private final SequentialTrigger<BaseEvent> loneWolfsLamentSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA483),
			(e1, s) -> {
				TetherEvent myTether = s.waitEvent(TetherEvent.class, te -> te.tetherIdMatches(0x13D, 0x13E) && te.eitherTargetMatches(XivCombatant::isThePlayer));
				boolean longTether = myTether.tetherIdMatches(0x13E);
				BuffApplied myBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0x112C, 0x112D));
				if (myBuff == null) {
					log.error("No buff!");
				}
				XivPlayerCharacter partner = (XivPlayerCharacter) myTether.getTargetMatching(cbt -> !cbt.isThePlayer());
				s.setParam("partner", partner);

				Job partnerJob = partner.getJob();
				if (partnerJob.isHealer()) {
					s.updateCall(longTether ? wolfsLamentLongTetherToHealer : wolfsLamentShortTetherToHealer);
				}
				else if (partnerJob.isTank()) {
					s.updateCall(longTether ? wolfsLamentLongTetherToTank : wolfsLamentShortTetherToTank);
				}
				else {
					s.updateCall(longTether ? wolfsLamentLongTetherToDps : wolfsLamentShortTetherToDps);
				}

				// Find the tower with two
				CastLocationDataEvent twoTower = s.waitEvent(CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA487));
				ArenaSector twoTowerLocation = getP2platform(twoTower.getPos());
				s.setParam("twoTower", twoTowerLocation);
				s.call(wolfsLamentTwoTower, twoTower.originalEvent());

			});

	/*
		Lone Wolf's Lament
		Tether 0x13D = short tether, 0x13E = long tether (TODO check if there is a different ID for satisfied vs unsatisfied tethers)
		Support needs to know whether they have short or long
		DPS needs to know if they have short or long and whether they are tethered to healers or tanks
	*/

	@NpcCastCallout({0xAA02, 0xA494})
	private final ModifiableCallout<AbilityCastStart> enrageTower = ModifiableCallout.durationBasedCall("Enrage Tower", "Enrage Tower");

	@NpcCastCallout(0xA49E)
	private final ModifiableCallout<AbilityCastStart> finalEnrage = ModifiableCallout.durationBasedCall("Starcleaver", "Enrage");
}
