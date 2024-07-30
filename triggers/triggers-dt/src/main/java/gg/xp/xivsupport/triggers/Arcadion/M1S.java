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
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;

@CalloutRepo(name = "M1S", duty = KnownDuty.M1S)
public class M1S extends AutoChildEventHandler implements FilteredEventHandler {

	public M1S(XivState state) {
		this.state = state;
	}

	private final XivState state;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M1S);
	}

//	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingInitial = ModifiableCallout.durationBasedCall("Quadruple Crossing - Initial", "Quadruple Crossing");
//	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingCardFirst = ModifiableCallout.durationBasedCall("Quadruple Crossing - Card First", "Cardinals to Intercards");
//	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingIntercardFirst = ModifiableCallout.durationBasedCall("Quadruple Crossing - Card First", "Intercards to Cards");
//	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingCardFirstWithClone = ModifiableCallout.durationBasedCall("Quadruple Crossing - Card First + Clone", "Cardinals to Intercards (Clone)");
//	private final ModifiableCallout<AbilityCastStart> quadrupleCrossingIntercardFirstWithClone = ModifiableCallout.durationBasedCall("Quadruple Crossing - Card First + Clone", "Intercards to Cards (Clone)");
//	private final ModifiableCallout<?> quadrupleCrossingCardinals = new ModifiableCallout<>("Quadruple Crossing - Cardinals After", "Cardinals");
//	private final ModifiableCallout<?> quadrupleCrossingIntercards = new ModifiableCallout<>("Quadruple Crossing - Intercards After", "Intercards");
//
//	// TODO: this is not card/inter
//	private final SequentialTrigger<BaseEvent> quadrupleCrossing = SqtTemplates.sq(30_000,
//			AbilityCastStart.class, acs -> acs.abilityIdMatches(0, 1),
//			(e1, s) -> {
//		// sequence is:
//				// one group baits
//				// other four bait
//				// dodge where first set went
//				// dodge where second set went
//			});

	private final ModifiableCallout<AbilityCastStart> oneTwoLeftFirst = ModifiableCallout.durationBasedCall("One Two - Left First", "Left then Right");
	private final ModifiableCallout<AbilityCastStart> oneTwoRightFirst = ModifiableCallout.durationBasedCall("One Two - Right First", "Right then Left");
	private final ModifiableCallout<AbilityUsedEvent> oneTwoLeft = new ModifiableCallout<>("One Two - Left Second", "Left");
	private final ModifiableCallout<AbilityUsedEvent> oneTwoRight = new ModifiableCallout<>("One Two - Right Second", "Right");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> oneTwoPaw = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9438, 0x9439),
			(e1, s) -> {
				boolean leftFirst = e1.abilityIdMatches(0x9438);
				if (leftFirst) {
					s.updateCall(oneTwoLeftFirst, e1);
				}
				else {
					s.updateCall(oneTwoRightFirst, e1);
				}
				var event = s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				if (leftFirst) {
					s.updateCall(oneTwoRight, event);
				}
				else {
					s.updateCall(oneTwoLeft, event);
				}
			});

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
				// first invocation:
				// just call out whether it's on supports or dps
				// Call out each kick
				for (int i = 0; i < 4; i++) {
					var hm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 320);
					s.setParam("hm", hm);
					var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9446, 0x9448));
					if (cast.abilityIdMatches(0x9446)) {
						s.updateCall(kick, cast);
					}
					else {
						s.updateCall(punch, cast);
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> splintering = ModifiableCallout.durationBasedCall("Splintering Nails", "Role Groups");
	private final ModifiableCallout<AbilityCastStart> stack = ModifiableCallout.durationBasedCall("Stack", "Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mouserExtra = SqtTemplates.multiInvocation(120_000,
			// Initial Mouser cast
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9441),
			(e1, s) -> {
			},
			(e1, s) -> {
				// first invocation:
				// just call out whether it's on supports or dps
				// Call out each kick
				for (int i = 0; i < 4; i++) {
					// 0x9499 = splintering (role groups)
					// 0x9497 = stack
					var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9497, 0x9499));
					if (cast.abilityIdMatches(0x9499)) {
						s.updateCall(splintering, e1);
					}
					else {
						s.updateCall(stack, e1);
					}
				}
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

}
