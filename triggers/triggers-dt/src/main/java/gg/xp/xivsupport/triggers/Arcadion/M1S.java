package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
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
import java.util.function.Predicate;

@CalloutRepo(name = "M1S", duty = KnownDuty.M1S)
public class M1S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M1S.class);

	public M1S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private XivState state;
	private StatusEffectRepository buffs;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M1S);
	}

	private static final Predicate<AbilityUsedEvent> isQuadCrossingHit = aue -> aue.abilityIdMatches(0x943F, 0x9440, 0x945B, 0x945C, 0x947D, 0x947E);
	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitial = ModifiableCallout.durationBasedCall("Quadruple Crossing - Initial", "Quadruple Crossing");
	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitialLeapingLeft = ModifiableCallout.durationBasedCall("Quadruple Crossing - Leaping Left", "Quadruple Crossing - Leaping Left");
	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitialLeapingRight = ModifiableCallout.durationBasedCall("Quadruple Crossing - Leaping Right", "Quadruple Crossing - Leaping Right");
	private final ModifiableCallout<?> quadrupleCrossingBaitSecond = new ModifiableCallout<>("Quadruple Crossing - Bait Second Wave", "Bait - In");
	private final ModifiableCallout<?> quadrupleCrossingDontBaitSecond = new ModifiableCallout<>("Quadruple Crossing - Avoid Second Wave", "Don't Bait - Out");
	private final ModifiableCallout<?> quadrupleCrossingThird = new ModifiableCallout<>("Quadruple Crossing - Dodge First", "Dodge 1");
	private final ModifiableCallout<?> quadrupleCrossingFourth = new ModifiableCallout<>("Quadruple Crossing - Dodge Second", "Dodge 2");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> quadrupleCrossing = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x943C, 0x982F, 0x9457, 0x982F),
			(e1, s) -> {
				switch ((int) e1.getAbility().getId()){
					case 0x943C -> s.updateCall(quadrupleCrossingInitial, e1);
					// There are two other IDs, 9479 and 9853, which are left/right, but those are paired with
					// Nailchipper
					case 0x9457 -> s.updateCall(quadrupleCrossingInitialLeapingLeft, e1);
					case 0x982F -> s.updateCall(quadrupleCrossingInitialLeapingRight, e1);
				}
				var hits = s.waitEventsQuickSuccession(99, AbilityUsedEvent.class, isQuadCrossingHit);
				if (hits.stream().anyMatch(hit -> hit.getTarget().isThePlayer())) {
					s.updateCall(quadrupleCrossingDontBaitSecond);
				}
				else {
					s.updateCall(quadrupleCrossingBaitSecond);
				}
				s.waitMs(250);
				s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, isQuadCrossingHit);
				s.updateCall(quadrupleCrossingThird);
				s.waitMs(250);
				s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, isQuadCrossingHit);
				s.updateCall(quadrupleCrossingFourth);
			});

	private static final Duration oneTwoDelay = Duration.ofSeconds(1);
	private static final Duration oneTwoLeapingDelay = Duration.ofMillis(1_700);
	private final ModifiableCallout<AbilityCastStart> oneTwoLeftFirst = ModifiableCallout.durationBasedCallWithOffset("One Two - Left First", "Left then Right", oneTwoDelay);
	private final ModifiableCallout<AbilityCastStart> oneTwoRightFirst = ModifiableCallout.durationBasedCallWithOffset("One Two - Right First", "Right then Left", oneTwoDelay);
	private final ModifiableCallout<AbilityCastStart> oneTwoLeapingLeftFirst = ModifiableCallout.durationBasedCallWithOffset("One Two (Leaping) - Left First", "Left then Right ({whereClone})", oneTwoLeapingDelay);
	private final ModifiableCallout<AbilityCastStart> oneTwoLeapingRightFirst = ModifiableCallout.durationBasedCallWithOffset("One Two (Leaping) - Right First", "Right then Left ({whereClone})", oneTwoLeapingDelay);
	private final ModifiableCallout<AbilityUsedEvent> oneTwoLeft = new ModifiableCallout<>("One Two - Left Second", "Left");
	private final ModifiableCallout<AbilityUsedEvent> oneTwoRight = new ModifiableCallout<>("One Two - Right Second", "Right");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> oneTwoPaw = SqtTemplates.sq(30_000,
			/*
			Normal one-two:
			Left->Right:
				9436 Boss
				9437 First hit
				9438 Second hit
			Right->Left:
				9439 boss
				943B first hit
				943A second hit
			 */
			/*
			Leaping
			944D left right (west)
			944E right left (west)
			944F left right (east)
			9450 right left (east)
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9436, 0x9439, 0x944D, 0x944E, 0x944F, 0x9450),
			(e1, s) -> {
				boolean leftFirst = e1.abilityIdMatches(0x9436, 0x944D, 0x944F);
				boolean leaping = e1.getAbility().getId() > 0x9440;
				if (leaping) {
					s.setParam("whereClone", e1.abilityIdMatches(0x944F, 0x9450) ? ArenaSector.EAST : ArenaSector.WEST);
					if (leftFirst) {
						s.updateCall(oneTwoLeapingLeftFirst, e1);
					}
					else {
						s.updateCall(oneTwoLeapingRightFirst, e1);
					}

				}
				else {
					if (leftFirst) {
						s.updateCall(oneTwoLeftFirst, e1);
					}
					else {
						s.updateCall(oneTwoRightFirst, e1);
					}

				}
				var event = s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// The boss cast is 1 second shorter
				Duration delay = leaping ? oneTwoLeapingDelay : oneTwoDelay;
				log.info("Delay: {}", delay);
				s.waitDuration(delay);
				if (leftFirst) {
					s.updateCall(oneTwoRight, event);
				}
				else {
					s.updateCall(oneTwoLeft, event);
				}
			});

	@NpcCastCallout(0x9494)
	private final ModifiableCallout<AbilityCastStart> bloodyScratch = ModifiableCallout.durationBasedCall("Bloody Scratch", "Raidwide");
	@NpcCastCallout(0x9495)
	private final ModifiableCallout<AbilityCastStart> biscuitMaker = ModifiableCallout.durationBasedCall("Biscuit Maker", "Tankbuster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> doubleSwipe = ModifiableCallout.durationBasedCall("Double Swipe", "Light Parties");
	private final ModifiableCallout<AbilityCastStart> quadSwipe = ModifiableCallout.durationBasedCall("Quad Swipe", "Partners");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> doubleQuadSwipeSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x945f, 0x9481, 0x945d, 0x947f),
			(e1, s) -> {
				if (e1.abilityIdMatches(0x945f, 0x9481)) {
					s.updateCall(doubleSwipe, e1);
				}
				else {
					s.updateCall(quadSwipe, e1);
				}
				// light parties
				// wait for thing
				// call light parties on clone
			});

	private final ModifiableCallout<AbilityCastStart> kick = ModifiableCallout.durationBasedCall("Mouser: Kick", "Kick on {hm.target} - Knock");
	private final ModifiableCallout<AbilityCastStart> punch = ModifiableCallout.durationBasedCall("Mouser: Punch", "Punch on {hm.target}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mouser = SqtTemplates.sq(120_000,
			// Initial Mouser cast
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9441),
			(e1, s) -> {
				log.info("mouser start");
				// first invocation:
				// just call out whether it's on supports or dps
				// Call out each kick
				for (int i = 0; i < 4; i++) {
					log.info("mouser {}", i);
//					var hm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 320);
					var hm = s.waitEvent(HeadMarkerEvent.class, hme -> true);
					s.setParam("hm", hm);
					var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9446, 0x9448));
					if (cast.abilityIdMatches(0x9446)) {
						s.updateCall(kick, cast);
					}
					else {
						s.updateCall(punch, cast);
					}
				}
				log.info("mouser end");
			});

	private final ModifiableCallout<AbilityCastStart> splintering = ModifiableCallout.durationBasedCall("Splintering Nails", "Role Groups");
	private final ModifiableCallout<AbilityCastStart> stack = ModifiableCallout.durationBasedCall("Stack", "Stack");

	// Extra trigger purely for stack/splintering on second mouser
	@AutoFeed
	private final SequentialTrigger<BaseEvent> mouserExtra = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9441),
			(e1, s) -> log.info("mouserExtra: ignoring first"),
			(e1, s) -> {
				log.info("mouserExtra: second start");
				for (int i = 0; i < 4; i++) {
					log.info("mouserExtra: {}", i);
					var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9497, 0x9499));
					if (cast.abilityIdMatches(0x9499)) {
						s.updateCall(splintering, cast);
					}
					else {
						s.updateCall(stack, cast);
					}
				}
				log.info("mouserExtra: second end");
			});

	private final ModifiableCallout<AbilityCastStart> shockwave = ModifiableCallout.durationBasedCall("Shockwave", "Knockback into Spread");
	private final ModifiableCallout<AbilityCastStart> shockwaveAfter = ModifiableCallout.durationBasedCall("Shockwave Spread", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> shockWave = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x944C),
			(e1, s) -> {
				s.updateCall(shockwave, e1);
				var spread = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9B84));
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				s.updateCall(shockwaveAfter, spread);
			});

	// For leaping one-two
	//Tempestuous tear 0x9483 is the light party stacks

	private final ModifiableCallout<AbilityCastStart> tempestuousTearWestInFirst = ModifiableCallout.durationBasedCall("Tempestuous Tear: West, In First", "Party Stacks - West, In First");
	private final ModifiableCallout<AbilityCastStart> tempestuousTearEastInFirst = ModifiableCallout.durationBasedCall("Tempestuous Tear: East, In First", "Party Stacks - East, In First");
	private final ModifiableCallout<AbilityCastStart> tempestuousTearWestOutFirst = ModifiableCallout.durationBasedCall("Tempestuous Tear: West, Out First", "Party Stacks - West, Out First");
	private final ModifiableCallout<AbilityCastStart> tempestuousTearEastOutFirst = ModifiableCallout.durationBasedCall("Tempestuous Tear: East, Out First", "Party Stacks - East, Out First");
	private final ModifiableCallout<?> tempestuousTearOutSecond = new ModifiableCallout<>("Tempestuous Tear: Out Second", "Out");
	private final ModifiableCallout<?> tempestuousTearInSecond = new ModifiableCallout<>("Tempestuous Tear: In Second", "In");

	/*
	946F - facing south, teleporting east, cleaving right (west) first
	9470 - facing north, teleporting west, cleaving left (west) first
	9471 - facing north, teleporting east, cleaving right (east) first
	9472 - facing south, teleporting west, cleaving left (east) first
	 */
	@AutoFeed
	private final SequentialTrigger<BaseEvent> tempestuousTearSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x946F, 0x9470, 0x9471, 0x9472),
			(e1, s) -> {
				switch ((int) e1.getAbility().getId()) {
					case 0x946F -> s.updateCall(tempestuousTearEastOutFirst, e1);
					case 0x9470 -> s.updateCall(tempestuousTearWestInFirst, e1);
					case 0x9471 -> s.updateCall(tempestuousTearEastInFirst, e1);
					case 0x9472 -> s.updateCall(tempestuousTearWestOutFirst, e1);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				s.waitMs(1700);
				s.updateCall(e1.abilityIdMatches(0x9470, 0x9471) ? tempestuousTearOutSecond : tempestuousTearInSecond);
			});

	@NpcCastCallout({0x9ABB, 0x9ABC})
	private final ModifiableCallout<AbilityCastStart> rainingCats = ModifiableCallout.durationBasedCall("Raining Cats", "Tethers and Stacks");

	private final ModifiableCallout<AbilityCastStart> nailchipperBaitFirst = ModifiableCallout.durationBasedCall("Nailchipper - Bait First Wave", "Bait Protean");
	private final ModifiableCallout<AbilityCastStart> nailchipperDontBaitFirst = ModifiableCallout.durationBasedCall("Nailchipper - Avoid First Wave", "Out");
	private final ModifiableCallout<AbilityCastStart> nailchipperBaitSecond = ModifiableCallout.durationBasedCall("Nailchipper - Bait Second Wave", "Bait Protean");
	private final ModifiableCallout<AbilityCastStart> nailchipperDontBaitSecond = ModifiableCallout.durationBasedCall("Nailchipper - Avoid Second Wave", "Out");
	private final ModifiableCallout<?> nailchipperThird = new ModifiableCallout<>("Nailchipper - Dodge First", "Dodge 1");
	private final ModifiableCallout<?> nailchipperFourth = new ModifiableCallout<>("Nailchipper - Dodge Second", "Dodge 2");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> nailchipper = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9485),
			(e1, s) -> {
				// Same as quadruple crossing (in fact, has a quad crossing cast in it),
				// but first/second set is forced due to the nailchipper mechanic.
				// These casts are 1:1 with the headmarkers
				{
					var headCasts = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9486));
					var mine = headCasts.stream().filter(acs -> acs.getTarget().isThePlayer()).findFirst().orElse(null);
					if (mine == null) {
						s.updateCall(nailchipperBaitFirst, headCasts.get(0));
					}
					else {
						s.updateCall(nailchipperDontBaitFirst, mine);
					}
				}
				// Repeat but 2nd wave
				{
					var headCasts = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9486));
					var mine = headCasts.stream().filter(acs -> acs.getTarget().isThePlayer()).findFirst().orElse(null);
					if (mine == null) {
						s.updateCall(nailchipperBaitSecond, headCasts.get(0));
					}
					else {
						s.updateCall(nailchipperDontBaitSecond, mine);
					}
				}
				s.waitMs(50);
				s.waitEvent(AbilityUsedEvent.class, isQuadCrossingHit);
				s.updateCall(nailchipperThird);
				s.waitMs(50);
				s.waitEvent(AbilityUsedEvent.class, isQuadCrossingHit);
				s.updateCall(nailchipperFourth);


			});

	private static final ArenaPos ap = new ArenaPos(100, 100, 5, 5);

	// 24410 ms delay between fake and real cast, plus the 1s extra from the fake casts being longer
	private static final Duration soulshadeOneTwoOffset = Duration.ofMillis(24_410).plusSeconds(1);
	private final ModifiableCallout<AbilityCastStart> soulshadeCardinalFirst = ModifiableCallout.durationBasedCallWithOffset("Soul Shade One-Two: Cardinal First", "Start {cardinal}-{intercard.opposite()}", soulshadeOneTwoOffset);
	private final ModifiableCallout<AbilityCastStart> soulshadeIntercardFirst = ModifiableCallout.durationBasedCallWithOffset("Soul Shade One-Two: Cardinal First", "Start {intercard}-{cardinal.opposite()}", soulshadeOneTwoOffset);
	private final ModifiableCallout<?> soulshadeMove = new ModifiableCallout<>("Soul Shade One-Two: Cross Over", "Move");

	// 9464 = hitting right first
	// 9467 = hitting left first
	// can ignore the actual damaging casts
	@AutoFeed
	private final SequentialTrigger<BaseEvent> soulshadeOneTwo = SqtTemplates.multiInvocation(50_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9436, 0x9439),
			(initialCast, s) -> {
				log.info("Soulshade one-two: start");
				var buff1 = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x891));
				var buff2 = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x891));
				s.waitThenRefreshCombatants(100);
				XivCombatant fakeCbt1 = state.getLatestCombatantData(buff1.getTarget());
				XivCombatant fakeCbt2 = state.getLatestCombatantData(buff2.getTarget());
				ArenaSector pos1 = ap.forCombatant(fakeCbt1);
				ArenaSector pos2 = ap.forCombatant(fakeCbt2);
				boolean leftSafeFirst = initialCast.abilityIdMatches(0x9436);
				log.info("Soulshade one-two: sectors {} and {}", pos1, pos2);
				ArenaSector cardinalPos;
				ArenaSector intercardPos;
				if (pos1.isCardinal()) {
					cardinalPos = pos1;
					intercardPos = pos2;
				}
				else if (pos2.isCardinal()) {
					cardinalPos = pos2;
					intercardPos = pos1;
				}
				else {
					throw new RuntimeException("Could not determine positions! %s %s".formatted(fakeCbt1.getPos(), fakeCbt2.getPos()));
				}
				s.setParam("cardinal", cardinalPos);
				s.setParam("intercard", intercardPos);
				int delta = cardinalPos.eighthsTo(intercardPos);
				if (delta == 3) {
					// e.g. S + NW
					// If hitting left first, start on cardinal side. S cleaves W, NW cleaves NE, so only S-SE is safe
					if (!leftSafeFirst) {
						s.updateCall(soulshadeCardinalFirst, initialCast);
					}
					else {
						s.updateCall(soulshadeIntercardFirst, initialCast);
					}
				}
				else if (delta == -3) {
					// e.g. S + NE
					// If hitting left first, start on intercardinal side. S cleaves W, NE cleaves SE, so only N-NW is afe.
					if (!leftSafeFirst) {
						s.updateCall(soulshadeIntercardFirst, initialCast);
					}
					else {
						s.updateCall(soulshadeCardinalFirst, initialCast);
					}
				}
				else {
					throw new RuntimeException("Could not determine delta! %s %s".formatted(fakeCbt1.getPos(), fakeCbt2.getPos()));
				}
				var cloneCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9464, 0x9467));
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == cloneCast);
				s.waitMs(1000);
				s.updateCall(soulshadeMove);
			});

	/*
	TODO:
	Trigger for boss destroying tiles?
	*/

	@NpcCastCallout(value = 0x9AD3, suppressMs = 5_000)
	private final ModifiableCallout<AbilityCastStart> predaceousPounce = ModifiableCallout.durationBasedCall("Predaceous Pounce", "Watch Pounces");
}
