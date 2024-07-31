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
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitial = ModifiableCallout.durationBasedCall("Quadruple Crossing - Initial", "Quadruple Crossing");
	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitialLeaping = ModifiableCallout.durationBasedCall("Quadruple Crossing - Initial (Leaping)", "Quadruple Crossing on Clone");
	private final ModifiableCallout<?> quadrupleCrossingBaitSecond = new ModifiableCallout<>("Quadruple Crossing - Bait Second Wave", "Bait - In");
	private final ModifiableCallout<?> quadrupleCrossingDontBaitSecond = new ModifiableCallout<>("Quadruple Crossing - Avoid Second Wave", "Don't Bait - Out");
	private final ModifiableCallout<?> quadrupleCrossingThird = new ModifiableCallout<>("Quadruple Crossing - Dodge First", "Dodge 1");
	private final ModifiableCallout<?> quadrupleCrossingFourth = new ModifiableCallout<>("Quadruple Crossing - Dodge Second", "Dodge 2");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> quadrupleCrossing = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x943C, 0x982F),
			(e1, s) -> {
				s.updateCall(e1.abilityIdMatches(0x943C) ? quadrupleCrossingInitial : quadrupleCrossingInitialLeaping, e1);
				s.waitMs(50);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x943F, 0x9440, 0x945B, 0x945C));
				if (buffs.isStatusOnTarget(state.getPlayer(), 0xC3A)) {
					s.updateCall(quadrupleCrossingDontBaitSecond);
				}
				else {
					s.updateCall(quadrupleCrossingBaitSecond);
				}
				s.waitMs(50);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x943F, 0x9440, 0x945B, 0x945C));
				s.updateCall(quadrupleCrossingThird);
				s.waitMs(50);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x943F, 0x9440, 0x945B, 0x945C));
				s.updateCall(quadrupleCrossingFourth);
			});

	private final ModifiableCallout<AbilityCastStart> oneTwoLeftFirst = ModifiableCallout.durationBasedCall("One Two - Left First", "Left then Right");
	private final ModifiableCallout<AbilityCastStart> oneTwoRightFirst = ModifiableCallout.durationBasedCall("One Two - Right First", "Right then Left");
	// TODO: these are not wired
	private final ModifiableCallout<AbilityCastStart> oneTwoLeapingLeftFirst = ModifiableCallout.durationBasedCall("One Two (Leaping) - Left First", "Left then Right ({whereClone})");
	private final ModifiableCallout<AbilityCastStart> oneTwoLeapingRightFirst = ModifiableCallout.durationBasedCall("One Two (Leaping) - Right First", "Right then Left ({whereClone})");
	private final ModifiableCallout<AbilityUsedEvent> oneTwoLeft = new ModifiableCallout<>("One Two - Left Second", "Left");
	private final ModifiableCallout<AbilityUsedEvent> oneTwoRight = new ModifiableCallout<>("One Two - Right Second", "Right");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> oneTwoPaw = SqtTemplates.sq(30_000,
			// TODO: 944F missing, 9471 (with party stacks)
			/*
			Leaping
			944D left right (west)
			944E right left (west)
			944F left right (east)
			9450 right left (east)
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9438, 0x9439, 0x944E, 0x9450),
			(e1, s) -> {
				boolean leftFirst = e1.abilityIdMatches(0x9438, 0x944D, 0x944F);
				boolean leaping = e1.getAbility().getId() > 0x9440;
				if (leaping) {
					s.setParam("whereClone", e1.abilityIdMatches(0x944F, 0x9450) ? ArenaSector.EAST : ArenaSector.WEST);
					if (leftFirst) {
						s.updateCall(leaping ? oneTwoLeapingLeftFirst : oneTwoLeftFirst, e1);
					}
					else {
						s.updateCall(leaping ? oneTwoLeapingRightFirst : oneTwoRightFirst, e1);
					}

				}
				else {
					if (leftFirst) {
						s.updateCall(leaping ? oneTwoLeapingLeftFirst : oneTwoLeftFirst, e1);
					}
					else {
						s.updateCall(leaping ? oneTwoLeapingRightFirst : oneTwoRightFirst, e1);
					}

				}
				var event = s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
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
					// TODO: is offset even needed?
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
	@NpcCastCallout(0x9483)
	private final ModifiableCallout<AbilityCastStart> lightParties = ModifiableCallout.durationBasedCall("Tempestuous Tear", "Light Parties");

	@NpcCastCallout({0x9ABB, 0x9ABC})
	private final ModifiableCallout<AbilityCastStart> rainingCats = ModifiableCallout.durationBasedCall("Raining Cats", "Tethers");

	/*
	TODO:
	Nailchipper is the proteans + head markers
	Soulshade one-two paw - like e9s cleaves
	It also uses this during the light parties mechanic
	 */
}
