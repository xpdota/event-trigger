package gg.xp.xivsupport.triggers.dtex;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.Duration;

@CalloutRepo(name = "EX2", duty = KnownDuty.DtEx2)
public class DTEx2 extends AutoChildEventHandler implements FilteredEventHandler {

	private XivState state;

	public DTEx2(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.DtEx2);
	}

	@NpcCastCallout(0x9398)
	private final ModifiableCallout<AbilityCastStart> actualize = ModifiableCallout.durationBasedCall("Actualize", "Raidwide");

	@NpcCastCallout(0x93a2)
	private final ModifiableCallout<AbilityCastStart> multidirectionalDivie = ModifiableCallout.durationBasedCall("Multidirectional Divide", "Cross");

	@NpcCastCallout(0x993b)
	private final ModifiableCallout<AbilityCastStart> regicidalRage = ModifiableCallout.durationBasedCall("Regicidal Rage", "Tank Tethers");

	@NpcCastCallout(0x993e)
	private final ModifiableCallout<AbilityCastStart> bitterWhirlwind = ModifiableCallout.durationBasedCall("Bitter Whirlwind", "Buster on {event.target}");

	@PlayerHeadmarker(value = 0, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> yellowMarker = new ModifiableCallout<>("Yellow Marker", "Spread on Tiles");

	@NpcCastCallout(0x9374)
	private final ModifiableCallout<AbilityCastStart> dutysEdge = ModifiableCallout.durationBasedCall("Duty's Edge", "Stack - Multiple Hits");

	@NpcCastCallout(0x9397)
	private final ModifiableCallout<AbilityCastStart> dawnOfAnAge = ModifiableCallout.durationBasedCall("Dawn of an Age", "Raidwide");

	@NpcCastCallout(value = 0x938a, suppressMs = 15_000)
	private final ModifiableCallout<AbilityCastStart> projectionOfTriumph = ModifiableCallout.durationBasedCall("Projection of Triumph", "Avoid Balls, Follow Donuts");

	@NpcCastCallout(0x9357)
	private final ModifiableCallout<AbilityCastStart> vollok = ModifiableCallout.durationBasedCall("Vollok (Five Platforms)", "Watch Swords");

	@NpcCastCallout(0x9392)
	private final ModifiableCallout<AbilityCastStart> vollokTwoPlatforms = ModifiableCallout.durationBasedCall("Vollok (Two Platforms)", "Watch Swords");

	@NpcCastCallout(0x9359)
	private final ModifiableCallout<AbilityCastStart> sync = ModifiableCallout.durationBasedCall("Sync", "Swords Mirroring");

	@NpcCastCallout(0x9381)
	private final ModifiableCallout<AbilityCastStart> greaterGateway = ModifiableCallout.durationBasedCall("Greater Gateway", "Watch Tracks");

	@NpcCastCallout(value = 0x939C, suppressMs = 5000)
	private final ModifiableCallout<AbilityCastStart> forgedTrack = ModifiableCallout.durationBasedCall("Forged Track", "Watch Swords and Tracks");

	@NpcCastCallout(0x9a88)
	private final ModifiableCallout<AbilityCastStart> projectionOfTurmoil = ModifiableCallout.durationBasedCall("Projection of Turmoil", "Take Stacks Sequentially");

	private static final Duration cleaveOffset = Duration.ofMillis(1_200);

	private final ModifiableCallout<AbilityCastStart> leftHalfFull = ModifiableCallout.durationBasedCallWithOffset("Half Full (Left Safe)", "Left/{safe}", cleaveOffset);
	private final ModifiableCallout<AbilityCastStart> rightHalfFull = ModifiableCallout.durationBasedCallWithOffset("Half Full (Right Safe)", "Right/{safe}", cleaveOffset);

	// TODO: do these exist as standalone casts?
//	private final ModifiableCallout<AbilityCastStart> backwardEdge = ModifiableCallout.durationBasedCall("Backward Edge (Cleaving Front)", "Rear/{safe}");
//	private final ModifiableCallout<AbilityCastStart> forwardEdge = ModifiableCallout.durationBasedCall("Forward Edge (Cleaving Rear)", "Front/{safe}");

	private final ModifiableCallout<AbilityCastStart> backwardHalfLeft = ModifiableCallout.durationBasedCallWithOffset("Backward Half (Back Left Safe)", "Back Left/{safe}", cleaveOffset);
	private final ModifiableCallout<AbilityCastStart> backwardHalfRight = ModifiableCallout.durationBasedCallWithOffset("Backward Half (Back Right Safe)", "Back Right/{safe}", cleaveOffset);

	private final ModifiableCallout<AbilityCastStart> forwardHalfLeft = ModifiableCallout.durationBasedCallWithOffset("Forward Half (Front Left Safe)", "Front Left/{safe}", cleaveOffset);
	private final ModifiableCallout<AbilityCastStart> forwardHalfRight = ModifiableCallout.durationBasedCallWithOffset("Forward Half (Front Right Safe)", "Front Right/{safe}", cleaveOffset);

	/*
	Potentially relevant:
	0x9368 - half full (6.0) - left safe
	0x9369 - half full (6.0) - right safe
	0x936A - half full (6.3, actual aoe)
	0x937B - forward half (8.0) - front right safe, plus some distance
	0x937C - forward half (8.0) - front left safe, plus some distance
	0x937D - backward half (8.0) - rear left safe, plus some distance
	0x937E - backward half (8.0) - right right safe? plus some distance
	0x9380 - half full (1.0, actual aoe)
	0x939E - half full (6.3, actual aoe)
	0x9972 - backward edge (1.0, actual aoe)
	0x999A - forward half (9.0) - front right safe, no extra distance
	0x999B - forward half (9.0) - front left safe? no extra distance
	0x999C - backward half (9.0) - back left safe
	0x999D - backward half (9.0) - back right safe
	 */

	@AutoFeed
	private final SequentialTrigger<BaseEvent> bossCleaves = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> {
				long id = acs.getAbility().getId();
				return id == 0x9368 || id == 0x9369
				       || (id >= 0x937B && id <= 0x937E)
				       || (id >= 0x999A && id <= 0x999D);
			},
			(e1, s) -> {
				s.waitMs(100);
				var locationInfo = e1.getLocationInfo();
				ArenaSector bossFacing;
				if (locationInfo == null) {
					s.waitThenRefreshCombatants(100);
					bossFacing = ArenaPos.combatantFacing(e1.getSource());
				}
				else {
					bossFacing = ArenaPos.combatantFacing(locationInfo.getBestHeading());
				}
				switch ((int) e1.getAbility().getId()) {
					case 0x9368 -> {
						s.setParam("safe", bossFacing.plusEighths(-2));
						s.updateCall(leftHalfFull, e1);
					}
					case 0x9369 -> {
						// left safe
						s.setParam("safe", bossFacing.plusEighths(2));
						s.updateCall(rightHalfFull, e1);
					}
					case 0x937B, 0x999A -> {
						// forward half, front right safe
						s.setParam("safe", bossFacing.plusEighths(1));
						s.updateCall(forwardHalfRight, e1);
					}
					case 0x937C, 0x999B -> {
						// forward half, front left safe
						s.setParam("safe", bossFacing.plusEighths(-1));
						s.updateCall(forwardHalfLeft, e1);
					}
					case 0x937D, 0x999C -> {
						// backward half, back left safe
						s.setParam("safe", bossFacing.plusEighths(-3));
						s.updateCall(backwardHalfLeft, e1);
					}
					case 0x937E, 0x999D -> {
						// backward half, back right safe
						s.setParam("safe", bossFacing.plusEighths(3));
						s.updateCall(backwardHalfRight, e1);
					}
				}
			});

	/*
	Half Circuit is a half (west or east) cleave with either an in or out
	Cast location info from the rectangle cast is unreliable. Use the boss's cast heading.
	936B (real) + 93A0 (donut)  + 939F (rectangle) = left in
	936B (real) + 93A1 (circle) + 939F (rectangle) = left out
	936C (real) + 93A1 (circle) + 939F (rectangle) = right out
	 */

	private final ModifiableCallout<AbilityCastStart> halfCircuitLeftOut = ModifiableCallout.durationBasedCall("Half Circuit: Left+Out", "Left/{safe} and Out");
	private final ModifiableCallout<AbilityCastStart> halfCircuitLeftIn = ModifiableCallout.durationBasedCall("Half Circuit: Left+In", "Left/{safe} and In");
	private final ModifiableCallout<AbilityCastStart> halfCircuitRightOut = ModifiableCallout.durationBasedCall("Half Circuit: Right+Out", "Right/{safe} and Out");
	private final ModifiableCallout<AbilityCastStart> halfCircuitRightIn = ModifiableCallout.durationBasedCall("Half Circuit: Right+In", "Right/{safe} and In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> halfCircuit = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x936B, 0x936C),
			(e1, s) -> {
				boolean rightSafe = e1.abilityIdMatches(0x936C);
				var inOut = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x93A0, 0x93A1));
				boolean out = inOut.abilityIdMatches(0x93A1);
				ArenaSector heading;
				var locationInfo = e1.getLocationInfo();
				if (locationInfo == null) {
					s.waitThenRefreshCombatants(100);
					XivCombatant source = state.getLatestCombatantData(e1.getSource());
					heading = ArenaPos.combatantFacing(source);
				}
				else {
					heading = ArenaPos.combatantFacing(locationInfo.getBestHeading());
				}
				if (rightSafe) {
					s.setParam("safe", heading.plusQuads(1));
				}
				else {
					s.setParam("safe", heading.plusQuads(-1));
				}
				if (rightSafe) {
					if (out) {
						s.updateCall(halfCircuitRightOut, e1);
					}
					else {
						s.updateCall(halfCircuitRightIn, e1);
					}
				}
				else {
					if (out) {
						s.updateCall(halfCircuitLeftOut, e1);
					}
					else {
						s.updateCall(halfCircuitLeftIn, e1);
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> bumpOnYou = ModifiableCallout.durationBasedCall("Drum of Vollok: Knockback on You", "Knock Buddy Back");
	private final ModifiableCallout<AbilityCastStart> getBumped = ModifiableCallout.durationBasedCall("Drum of Vollok: Knockback not on You", "Get Knocked Back");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> drumOfVollokSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x938E),
			(e1, s) -> {
				var bumps = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x938F));
				AbilityCastStart myBump = bumps.stream().filter(bump -> bump.getTarget().isThePlayer()).findFirst().orElse(null);
				if (myBump != null) {
					s.updateCall(bumpOnYou, myBump);
				}
				else {
					s.updateCall(getBumped, bumps.isEmpty() ? e1 : bumps.get(0));
				}
			});

	private final ModifiableCallout<AbilityCastStart> burningChainsInitial = new ModifiableCallout<>("Burning Chains Initial", "Stack");

	private final ModifiableCallout<BuffApplied> burningChains = ModifiableCallout.<BuffApplied>durationBasedCall("Burning Chains", "Break Chains (with {buddy})")
			.statusIcon(0x301);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> burningChainSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9395),
			(e1, s) -> {
				s.updateCall(burningChainsInitial, e1);
				var buff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x301) && ba.getTarget().isThePlayer());
				var tether = s.waitEvent(TetherEvent.class, te -> te.tetherIdMatches(0x80) && te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant buddy = tether.getTargetMatching(cbt -> !cbt.isThePlayer());
				s.setParam("buddy", buddy);
				s.updateCall(burningChains, buff);
			});

}
