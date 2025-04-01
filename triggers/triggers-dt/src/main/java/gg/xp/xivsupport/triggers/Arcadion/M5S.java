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
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "M5S", duty = KnownDuty.M5S)
public class M5S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M5S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M5S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M5S);
	}

	@NpcCastCallout(0xA721)
	private final ModifiableCallout<AbilityCastStart> deepCut = ModifiableCallout.<AbilityCastStart>durationBasedCall("Deep Cut", "Tankbuster with Bleed").statusIcon(0x8280);

	private final ModifiableCallout<AbilityCastStart> stockingASide = new ModifiableCallout<>("Stocking A-Side", "A Side Stocked");
	private final ModifiableCallout<AbilityCastStart> stockingBSide = new ModifiableCallout<>("Stocking B-Side", "B Side Stocked");

	private enum Stock {
		Roles,
		LightParty
	}

	private Stock lastStock;

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stockSq = SqtTemplates.sq(1_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA780, 0xA781),
			(e1, s) -> {
				if (e1.abilityIdMatches(0xA780)) {
					s.updateCall(stockingASide, e1);
					lastStock = Stock.Roles;
				}
				else {
					s.updateCall(stockingBSide, e1);
					lastStock = Stock.LightParty;
				}
			});

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
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781)
			 *
			 * A728 -> west -> east, facing north
			 * A729 -> west -> east, facing north
			 * A72A -> west -> east, facing north
			 * A72B -> east -> west, facing north
			 * A72C -> east -> west, facing north
			 * A72D -> east -> west, facing north
			 *
			 * A4DB -> west -> east, facing north
			 * A4DC -> east -> west, facing north
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA728, 0xA729, 0xA72A, 0xA72B, 0xA72C, 0xA72D),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA728, 0xA729, 0xA72A, 0xA4DB -> {
						firstCall = isRoles ? twoSnapEastRoles : twoSnapEastLp;
						secondCall = isRoles ? twoSnapEastRolesFollowup : twoSnapEastLpFollowup;
					}
					case 0xA72B, 0xA72C, 0xA72D, 0xA4DC -> {
						firstCall = isRoles ? twoSnapWestRoles : twoSnapWestLp;
						secondCall = isRoles ? twoSnapWestRolesFollowup : twoSnapWestLpFollowup;
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
			 * A730 -> west -> east, facing north
			 * A731 -> west -> east, facing north
			 * A732 -> west -> east, facing north
			 * A733 -> east -> west, facing north
			 * A734 -> ? east -> west, facing north
			 * A735 -> east -> west, facing north
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA730, 0xA731, 0xA732, 0xA733, 0xA734, 0xA735),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA730, 0xA731, 0xA732 -> {
						firstCall = isRoles ? threeSnapEastRoles : threeSnapEastLp;
						secondCall = isRoles ? threeSnapEastRolesFollowup : threeSnapEastLpFollowup;
					}
					case 0xA733, 0xA734, 0xA735 -> {
						firstCall = isRoles ? threeSnapWestRoles : threeSnapWestLp;
						secondCall = isRoles ? threeSnapWestRolesFollowup : threeSnapWestLpFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA737));
				s.updateCall(secondCall);
			});

	private final ModifiableCallout<AbilityCastStart> fourSnapWestRoles = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> fourSnapWestRolesFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> fourSnapEastRoles = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> fourSnapEastRolesFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> fourSnapWestLp = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> fourSnapWestLpFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> fourSnapEastLp = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> fourSnapEastLpFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fourSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781), but the cast ID of the actual twist/drop seems to also vary, so the precursor
			 * can effectively be ignored. However, we should call it out anyway for convenience/
			 *
			 * A730 -> west -> east, facing north
			 * A731 -> west -> east, facing north
			 * A732 -> west -> east, facing north
			 * A733 -> east -> west, facing north
			 * A734 -> ? east -> west, facing north
			 * A735 -> east -> west, facing north
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA739, 0xA73A, 0xA73B, 0xA73C, 0xA73D, 0xA73E),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA739, 0xA73A, 0xA73B -> {
						firstCall = isRoles ? fourSnapEastRoles : fourSnapEastLp;
						secondCall = isRoles ? fourSnapEastRolesFollowup : fourSnapEastLpFollowup;
					}
					case 0xA73C, 0xA73D, 0xA73E -> {
						firstCall = isRoles ? fourSnapWestRoles : fourSnapWestLp;
						secondCall = isRoles ? fourSnapWestRolesFollowup : fourSnapWestLpFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA741));
				s.updateCall(secondCall);
			});

	@NpcCastCallout(0xA723)
	private final ModifiableCallout<AbilityCastStart> celebrateGoodTimes = ModifiableCallout.durationBasedCall("Celebrate Good Times", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> discoInfernal = ModifiableCallout.durationBasedCall("Disco Infernal", "Raidwide");
	private final ModifiableCallout<BuffApplied> discoInfernalLong = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Long Timer", "Long Timer").autoIcon();
	private final ModifiableCallout<BuffApplied> discoInfernalShort = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Short Timer", "Short Timer").autoIcon();

	private final ModifiableCallout<AbilityCastStart> outsideIn = ModifiableCallout.durationBasedCall("Outside In", "In then Out");
	private final ModifiableCallout<AbilityCastStart> insideOut = ModifiableCallout.durationBasedCall("Inside Out", "Out then In");
	private final ModifiableCallout<?> outsideIn2 = new ModifiableCallout<>("Outside In, Followup", "Out");
	private final ModifiableCallout<?> insideOut2 = new ModifiableCallout<>("Inside Out, Followup", "In");

	private final ModifiableCallout<BuffApplied> discoInfernalSoak = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Soak Now", "Soak").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> discoInfernalSq = SqtTemplates.sq(75_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA756),
			(e1, s) -> {
				s.updateCall(discoInfernal, e1);
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

	private final ModifiableCallout<AbilityCastStart> arcadyInitial = ModifiableCallout.durationBasedCall("Arcady: Initial", "Out");

	private final ModifiableCallout<?> arcadyIn = new ModifiableCallout<>("Arcady: In", "In");
	private final ModifiableCallout<?> arcadyOut = new ModifiableCallout<>("Arcady: Out", "Out");
	private final ModifiableCallout<BuffApplied> arcadyNisi = ModifiableCallout.<BuffApplied>durationBasedCall("Arcady: Nisi", "Touch {partner}").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> arcadySq = SqtTemplates.sq(75_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9BE4),
			(e1, s) -> {
				s.updateCall(arcadyInitial, e1);
				s.waitCastFinished(casts, e1);
				// TODO: indicate when you got hit
				for (int i = 0; i < 7; i++) {
					if (i % 2 == 0) {
						s.updateCall(arcadyIn);
					}
					else {
						s.updateCall(arcadyOut);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA762, 0xA763) && aue.isFirstTarget());
				}
				s.waitMs(2_000);
				// Alpha is 0x116e, Beta is 116f

				BuffApplied myBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0x116e, 0x116f));
				if (myBuff == null) {
					log.warn("Player has no nisi!");
					return;
				}
				BuffApplied matchingBuff = buffs.findBuff(
						ba -> !ba.getTarget().isThePlayer()
						      && ba.buffIdMatches(0x116e, 0x116f)
						      // allow up to 1800ms of tolerance
						&& ba.getEstimatedRemainingDuration().minus(myBuff.getEstimatedRemainingDuration()).abs().toMillis() < 1_800) ;
				if (matchingBuff == null) {
					log.warn("Player has no partner!");
					return;
				}
				s.setParam("partnerBuff", matchingBuff);
				s.setParam("partner", matchingBuff.getTarget());
				s.waitDuration(myBuff.remainingDurationPlus(Duration.ofSeconds(-4)));
				s.updateCall(arcadyNisi, myBuff);
			});



	private final ModifiableCallout<ActorControlExtraEvent> letsDanceFirstWest = new ModifiableCallout<>("Start West");
	private final ModifiableCallout<ActorControlExtraEvent> letsDanceFirstEast = new ModifiableCallout<>("Start East");

	private final ModifiableCallout<ActorControlExtraEvent> letsDanceCrossWest = new ModifiableCallout<>("Move West");
	private final ModifiableCallout<ActorControlExtraEvent> letsDanceCrossEast = new ModifiableCallout<>("Move East");

	private final ModifiableCallout<ActorControlExtraEvent> letsDanceStayWest = new ModifiableCallout<>("Stay West");
	private final ModifiableCallout<ActorControlExtraEvent> letsDanceStayEast = new ModifiableCallout<>("Stay East");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ensembleFrogsSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9A32),
			(e1, s) -> {
				var first = s.waitEvent(ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x3f);
				var rest = s.waitEvents(7, ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x3f);

				var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA76A));
				var current = first;
				if (first.getData0() == 5) {
					s.updateCall(letsDanceFirstWest, current);
				}
				else {
					s.updateCall(letsDanceFirstEast, current);
				}
				s.waitCastFinished(casts, cast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9BDD) && aue.isFirstTarget());
				for (ActorControlExtraEvent acee : rest) {
					var prev = current;
					current = acee;
					if (acee.getData0() == 5) {
						s.updateCall(prev.getData0() == 5 ? letsDanceStayWest : letsDanceCrossWest, current);
					}
					else {
						s.updateCall(prev.getData0() == 5 ? letsDanceCrossEast : letsDanceStayEast, current);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9BDD) && aue.isFirstTarget());
				}
			});

	@NpcCastCallout(0xA75B)
	private final ModifiableCallout<AbilityCastStart> quarterBeats = ModifiableCallout.durationBasedCall("Quarter Beats", "Partners");
	@NpcCastCallout(0xA75D)
	private final ModifiableCallout<AbilityCastStart> eightBeats = ModifiableCallout.durationBasedCall("Eight Beats", "Spread");

	/*
	Quarter Beats = Partners
	Eight Beats = Spread
	Half Beats = Light Party? I haven't seen this yet



	Need to ID safe spots for frogtourage in all forms

	Let's Dance Remix:
	0xA390: Initial cast
	0xA391-4: Directional telegraphs?
	0xA395: Actual cast
	 */

}
