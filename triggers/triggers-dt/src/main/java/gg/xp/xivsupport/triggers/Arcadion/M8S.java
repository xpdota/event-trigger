package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
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

	// TODO: you have to look at his nouliths to determine whether it's cardinal or intercardinal dodge
	private final ModifiableCallout<AbilityCastStart> windfangCard = ModifiableCallout.durationBasedCall("Windfang", "Buddies, In, Cardinals");
	private final ModifiableCallout<AbilityCastStart> windfangInter = ModifiableCallout.durationBasedCall("Windfang", "Buddies, In, Intercards");
	private final ModifiableCallout<AbilityCastStart> stonefangCard = ModifiableCallout.durationBasedCall("Stonefang", "Spread, Out, Cardinals");
	private final ModifiableCallout<AbilityCastStart> stonefangInter = ModifiableCallout.durationBasedCall("Stonefang", "Spread, Out, Intercards");

	private static final int TODO = 9999999;

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

	private final ModifiableCallout<AbilityCastStart> dodgeClones = ModifiableCallout.durationBasedCall("Reigns: Dodge Clones", "Dodge Clones, Out of Middle");
	private final ModifiableCallout<AbilityCastStart> eminentStacks = ModifiableCallout.durationBasedCall("Eminent Reign: Away, Stacks", "Close Stacks");
	private final ModifiableCallout<AbilityCastStart> revoStacks = ModifiableCallout.durationBasedCall("Revolutionary Reign: Away, Stacks", "Far Stacks");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> reignsSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA930, 0xA931),
			(e1, s) -> {
				/*
				Revo: A913 + A931

				Boss can jump to different clones?
				 */
				s.updateCall(dodgeClones, e1);
				boolean isRevolutionary = e1.abilityIdMatches(0xA931);
				s.waitCastFinished(casts, e1);
				if (isRevolutionary) {
					s.updateCall(revoStacks, e1);
				}
				else {
					s.updateCall(eminentStacks, e1);
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

	// TODO: Great Divide
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
				var crossCasts = s.waitEvents(2, CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA3D7));
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
						.map(CastLocationDataEvent::getPos)
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

//	private final ModifiableCallout<AbilityCastStart> tacticalPack = ModifiableCallout.durationBasedCall("Tactical Pack", "Adds");
//	private final ModifiableCallout<TetherEvent> addsTether = new ModifiableCallout<TetherEvent>("Tactical Pack: Tether", "Tethered to {tetherLocation}")
//			.extendedDescription("""
//					To call where you need to go, change to { tetherLocation.opposite() }""");
//
//	private final ModifiableCallout<BuffApplied> addsGreenBuff = ModifiableCallout.durationBasedCall("Tactical Pack: Green Debuff", "Walk to Green");
//	private final ModifiableCallout<BuffApplied> addsYellowBuff = ModifiableCallout.durationBasedCall("Tactical Pack: Green Debuff", "Walk to Yellow");
//
//	@AutoFeed
//	private final SequentialTrigger<BaseEvent> tactialPackSq = SqtTemplates.sq(180_000,
//			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA3C8),
//			(e1, s) -> {
//				s.updateCall(tacticalPack, e1);
//				List<TetherEvent> tethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> te.tetherIdMatches(0x14F, 0x150));
//				tethers.stream().filter(hm -> hm.getTarget().isThePlayer())
//						.findAny()
//						.ifPresent(t -> {
//							s.waitThenRefreshCombatants(100);
//							var where = towersAp.forCombatant(state.getLatestCombatantData(t.getTargetMatching(cbt -> !cbt.isPc())));
//							s.setParam("tetherLocation", where);
//							s.updateCall(addsTether, t);
//						});
//				List<BuffApplied> packBuffs = s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(0x1125, 0x1126));
//				packBuffs.stream().filter(buff -> buff.getTarget().isThePlayer())
//						.findAny()
//						.ifPresent(myBuff -> {
//							RawModifiedCallout<BuffApplied> call;
//							if (myBuff.buffIdMatches(0x1125)) {
//								call = s.updateCall(addsGreenBuff, myBuff);
//							}
//							else {
//								call = s.updateCall(addsYellowBuff, myBuff);
//							}
//							s.waitBuffRemoved(buffs, myBuff);
//							call.forceExpire();
//						});
//			});
//

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
	/*
	P2
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
}
