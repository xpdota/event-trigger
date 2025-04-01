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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;

import java.time.Duration;

@CalloutRepo(name = "M5S", duty = KnownDuty.M5S)
public class M5S extends AutoChildEventHandler implements FilteredEventHandler {
	private final XivState state;

	public M5S(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M5S);
	}

	@NpcCastCallout(0xA721)
	private final ModifiableCallout<AbilityCastStart> deepCut = ModifiableCallout.<AbilityCastStart>durationBasedCall("Deep Cut", "Tankbuster with Bleed").statusIcon(0x8280);

	@NpcCastCallout(0xA780)
	private final ModifiableCallout<AbilityCastStart> stockingASide = new ModifiableCallout<>("Stocking A-Side", "A Side Stocked");
	@NpcCastCallout(0xA781)
	private final ModifiableCallout<AbilityCastStart> stockingBSide = new ModifiableCallout<>("Stocking B-Side", "B Side Stocked");

	private final ModifiableCallout<AbilityCastStart> twoSnapWestRoles = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> twoSnapWestRolesFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> twoSnapEastRoles = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> twoSnapEastRolesFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> twoSnapWestLp = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> twoSnapWestLpFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> twoSnapEastLp = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> twoSnapEastLpFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twoSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781), but the cast ID of the actual twist/drop seems to also vary, so the precursor
			 * can effectively be ignored. However, we should call it out anyway for convenience/
			 *
			 * A728 -> west -> east, facing north, light parties
			 * A729 -> west -> east, facing north, role stacks
			 * A72A -> west -> east, facing north, role stacks
			 * A72B -> east -> west, facing north, light parties
			 * A72C -> east -> west, facing north, role stacks
			 * A72D -> east -> west, facing north, role stacks
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA728, 0xA729, 0xA72A, 0xA72B, 0xA72C, 0xA72D),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				switch (id) {
					case 0xA728 -> {
						firstCall = twoSnapEastLp;
						secondCall = twoSnapEastLpFollowup;
					}
					case 0xA729, 0xA72A -> {
						firstCall = twoSnapEastRoles;
						secondCall = twoSnapEastRolesFollowup;
					}
					case 0xA72B -> {
						firstCall = twoSnapWestLp;
						secondCall = twoSnapWestLpFollowup;
					}
					case 0xA72C, 0xA72D -> {
						firstCall = twoSnapWestRoles;
						secondCall = twoSnapWestRolesFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA72E));
				s.updateCall(secondCall);
			});

	private final ModifiableCallout<AbilityCastStart> threeSnapWestRoles = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> threeSnapWestRolesFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> threeSnapEastRoles = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> threeSnapEastRolesFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> threeSnapWestLp = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> threeSnapWestLpFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> threeSnapEastLp = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> threeSnapEastLpFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> threeSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781), but the cast ID of the actual twist/drop seems to also vary, so the precursor
			 * can effectively be ignored. However, we should call it out anyway for convenience/
			 *
			 * A730 -> west -> east, facing north, role stacks
			 * A731 -> west -> east, facing north, role stacks
			 * A732 -> west -> east, facing north, light parties
			 * A733 -> east -> west, facing north, role stacks
			 * A734 -> ? east -> west, facing north, role stacks
			 * A735 -> east -> west, facing north, light parties
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA730, 0xA731, 0xA732, 0xA733, 0x7A34, 0xA735),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				switch (id) {
					case 0xA732 -> {
						firstCall = threeSnapEastLp;
						secondCall = threeSnapEastLpFollowup;
					}
					case 0xA730, 0xA731 -> {
						firstCall = threeSnapEastRoles;
						secondCall = threeSnapEastRolesFollowup;
					}
					case 0xA735 -> {
						firstCall = threeSnapWestLp;
						secondCall = threeSnapWestLpFollowup;
					}
					case 0xA733, 0xA734 -> {
						firstCall = threeSnapWestRoles;
						secondCall = threeSnapWestRolesFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA72E));
				s.updateCall(secondCall);
			});

	@NpcCastCallout(0xA723)
	private final ModifiableCallout<AbilityCastStart> celebrateGoodTimes = ModifiableCallout.durationBasedCall("Celebrate Good Times", "Raidwide");

	@NpcCastCallout(0xA723)
	private final ModifiableCallout<AbilityCastStart> discoInfernal = ModifiableCallout.durationBasedCall("Disco Infernal", "Raidwide");
	private final ModifiableCallout<BuffApplied> discoInfernalLong = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Long Timer", "Long Timer").autoIcon();
	private final ModifiableCallout<BuffApplied> discoInfernalShort = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Short Timer", "Short Timer").autoIcon();

	private final ModifiableCallout<AbilityCastStart> outsideIn = ModifiableCallout.durationBasedCall("Outside In", "In then Out");
	private final ModifiableCallout<AbilityCastStart> insideOut = ModifiableCallout.durationBasedCall("Inside Out", "Out then In");
	private final ModifiableCallout<?> outsideIn2 = ModifiableCallout.durationBasedCall("Outside In, Followup", "Out");
	private final ModifiableCallout<?> insideOut2 = ModifiableCallout.durationBasedCall("Inside Out, Followup", "In");

	private final ModifiableCallout<BuffApplied> discoInfernalSoak = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Soak Now", "Soak").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> discoInfernalSq = SqtTemplates.sq(75_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA756),
			(e1, s) -> {
				var myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x116D) && ba.getTarget().isThePlayer());
				boolean iAmLong = myBuff.getInitialDuration().toSeconds() > 28;
				RawModifiedCallout<BuffApplied> buffCall;
				if (iAmLong) {
					buffCall = s.call(discoInfernalLong, myBuff);
				}
				else {
					buffCall = s.call(discoInfernalShort, myBuff);
				}
				// A77C = Inside Out (Out then In safe)
				// A77E = Outside In (In then Out safe)
				AbilityCastStart inOutMech = s.waitEvent(AbilityCastStart.class, aue -> aue.abilityIdMatches(0xA77C, 0xA77E));
				if (inOutMech.abilityIdMatches(0xA77C)) {
					s.updateCall(insideOut, inOutMech);
				}
				else {
					s.updateCall(outsideIn, inOutMech);
				}
				// These are probably all the followups
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x93C2, 0x93C3, 0x93C4, 0x93C5));
				if (inOutMech.abilityIdMatches(0xA77C)) {
					s.updateCall(insideOut2);
				}
				else {
					s.updateCall(outsideIn2);
				}
				// Wait until our buff has 5 seconds left
				s.waitDuration(myBuff.remainingDurationPlus(Duration.ofSeconds(-5)));
				s.call(discoInfernalSoak, myBuff).setReplaces(buffCall);
			});

	@NpcCastCallout(0x9BE4)
	private final ModifiableCallout<AbilityCastStart> getDown = ModifiableCallout.durationBasedCall("Get Down!", "Out");
}
