package gg.xp.xivsupport.events.triggers.duties.ewult;

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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.PantoAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.ProgramLoopAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.PsMarkerAssignments;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.MultiSlotAutoMarkHandler;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.groupmodels.PsMarkerGroup;
import gg.xp.xivsupport.models.groupmodels.TwoGroupsOfFour;
import gg.xp.xivsupport.models.groupmodels.WrothStyleAssignment;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@CalloutRepo(name = "TOP Triggers", duty = KnownDuty.OmegaProtocol)
public class OmegaUltimate extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(OmegaUltimate.class);

	// P1
	// Looper
	private static final Duration looperOffset = Duration.ofMillis(-2650);
	private final ModifiableCallout<?> firstInLineLoop = new ModifiableCallout<>("Loop First: Start", "One with {buddy}").statusIcon(0xBBC);
	private final ModifiableCallout<BuffApplied> firstInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop First: Tower", "Take Tower", looperOffset).statusIcon(0xBBC);
	private final ModifiableCallout<?> firstInLineTether = new ModifiableCallout<>("Loop First: Tether", "Take Tether").statusIcon(0xBBC);
	private final ModifiableCallout<?> firstNotYou = new ModifiableCallout<>("Loop First: Not You", "1");

	private final ModifiableCallout<?> secondInLineLoop = new ModifiableCallout<>("Loop Second: Start", "Two with {buddy}").statusIcon(0xBBD);
	private final ModifiableCallout<BuffApplied> secondInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop Second: Tower", "Take Tower", looperOffset).statusIcon(0xBBD);
	private final ModifiableCallout<?> secondInLineTether = new ModifiableCallout<>("Loop Second: Tether", "Take tether").statusIcon(0xBBD);
	private final ModifiableCallout<?> secondNotYou = new ModifiableCallout<>("Loop Second: Not You", "2");

	private final ModifiableCallout<?> thirdInLineLoop = new ModifiableCallout<>("Loop Third: Start/Tether", "Three with {buddy}, First Tethers").statusIcon(0xBBE);
	private final ModifiableCallout<?> thirdInLineTether = new ModifiableCallout<>("Loop Third: Start/Tether", "Take Tether").statusIcon(0xBBE);
	private final ModifiableCallout<BuffApplied> thirdInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop Third: Tower", "Take Tower", looperOffset).statusIcon(0xBBE);
	private final ModifiableCallout<?> thirdNotYou = new ModifiableCallout<>("Loop Third: Not You", "3");

	private final ModifiableCallout<?> fourthInLineLoop = new ModifiableCallout<>("Loop Fourth: Start", "Four with {buddy}").statusIcon(0xD7B);
	private final ModifiableCallout<BuffApplied> fourthInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop Fourth: Tower", "Take tower", looperOffset).statusIcon(0xD7B);
	private final ModifiableCallout<?> fourthInLineTether = new ModifiableCallout<>("Loop Fourth: Tether", "Take tether").statusIcon(0xD7B);
	private final ModifiableCallout<?> fourthNotYou = new ModifiableCallout<>("Loop Fourth: Not You", "4");

	//Pantokrator
	private final ModifiableCallout<BuffApplied> pantoFirstInLine = new ModifiableCallout<BuffApplied>("Panto First", "One - Missile Now", "One with {buddy} - Missile in ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBC);
	private final ModifiableCallout<BaseEvent> pantoFirstGoBackIn = new ModifiableCallout<BaseEvent>("Panto First: Back In", "Stack", "Stack", ModifiableCallout.expiresIn(15)).statusIcon(0xBBC);

	private final ModifiableCallout<BuffApplied> pantoSecondInLine = new ModifiableCallout<BuffApplied>("Panto Second", "Two", "Two with {buddy} - Missile in ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBD);
	private final ModifiableCallout<BaseEvent> pantoSecondInLineOut = new ModifiableCallout<BaseEvent>("Panto Second: Out", "Missile Now then Cannon", "Missile Now Two - Missile ({missile.estimatedRemainingDuration}) then Cannon ({cannon.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	private final ModifiableCallout<BaseEvent> pantoSecondGoBackIn = new ModifiableCallout<BaseEvent>("Panto Second: Back In", "Stack", "Stack", ModifiableCallout.expiresIn(15)).statusIcon(0xBBD);
	private final ModifiableCallout<BaseEvent> pantoSecondNotYou = new ModifiableCallout<>("Panto Second: Not You", "2 - Stay Stacked");

	private final ModifiableCallout<BuffApplied> pantoThirdInLine = new ModifiableCallout<BuffApplied>("Panto Third", "Three", "Three with {buddy} - Missile in ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBE);
	private final ModifiableCallout<BaseEvent> pantoThirdInLineOut = new ModifiableCallout<BaseEvent>("Panto Third: Out", "Missile Now", "Missile Now ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	private final ModifiableCallout<BaseEvent> pantoThirdGoBackIn = new ModifiableCallout<BaseEvent>("Panto Third: Back In", "Stack", "Stack", ModifiableCallout.expiresIn(15)).statusIcon(0xBBE);
	private final ModifiableCallout<BaseEvent> pantoThirdNotYou = new ModifiableCallout<>("Panto Third: Not You", "3 - Stay Stacked");

	private final ModifiableCallout<BuffApplied> pantoFourthInLine = new ModifiableCallout<BuffApplied>("Panto Fourth", "Four", "Four with {buddy} - Missile in ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD7B);
	private final ModifiableCallout<BaseEvent> pantoFourthInLineOut = new ModifiableCallout<BaseEvent>("Panto Fourth: Out", "Missile Now", "Missile Now ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	private final ModifiableCallout<BaseEvent> pantoFourthNotYou = new ModifiableCallout<>("Panto Fourth: Not You", "4 - Stay Stacked");

	private final ModifiableCallout<BaseEvent> pantoCleave1asTank = new ModifiableCallout<>("Panto Buster 1: Prep As Tank", "Go Far, Bait Tankbusters");
	private final ModifiableCallout<BaseEvent> pantoCleave1asNonTank = new ModifiableCallout<>("Panto Buster 1: Prep As Non-Tank", "Cleave Baits");
	private final ModifiableCallout<BaseEvent> pantoCleave1withMarker = new ModifiableCallout<>("Panto Buster 1: Marker on You", "Position for Cleave");
	private final ModifiableCallout<BaseEvent> pantoCleave1noMarker = new ModifiableCallout<>("Panto Buster 1: No Marker", "Avoid Cleaves");
	private final ModifiableCallout<BaseEvent> pantoCleave2hadMarker = new ModifiableCallout<>("Panto Buster 2: Had Marker", "Avoid Cleaves");
	private final ModifiableCallout<BaseEvent> pantoCleave2hadNoMarker = new ModifiableCallout<>("Panto Buster 2: Did Not Have Marker", "Bait Cleaves");


	// P2
	@PlayerStatusCallout(0xDAC)
	private final ModifiableCallout<BuffApplied> packetFilterF = new ModifiableCallout<BuffApplied>("Packet Filter F", "Attack M").autoIcon();
	@PlayerStatusCallout(0xDAB)
	private final ModifiableCallout<BuffApplied> packetFilterM = new ModifiableCallout<BuffApplied>("Packet Filter M", "Attack F").autoIcon();

	private final ModifiableCallout<AbilityCastStart> checkMfPattern = new ModifiableCallout<AbilityCastStart>("Check M/F Sword/Shield")
			.extendedDescription("If the callouts below do not work, make sure your OverlayPlugin is at least version 0.19.14.");

	private final ModifiableCallout<?> partySynergyBothIn = new ModifiableCallout<>("Party Synergy: Both In", "On Male")
			.extendedDescription("Shield and Skates: Stand on Male");
	private final ModifiableCallout<?> partySynergyBothOut = new ModifiableCallout<>("Party Synergy: Both Out", "Out of Both")
			.extendedDescription("Neither Shield nor Skates: Cross + Chariot, stand off to the sides");
	private final ModifiableCallout<?> partySynergyFoutMin = new ModifiableCallout<>("Party Synergy: F Out, M In", "Sides of Male")
			.extendedDescription("Shield, no Skates: Small safe areas to the sides of Male");
	private final ModifiableCallout<?> partySynergyFinMout = new ModifiableCallout<>("Party Synergy: F In, M Out", "On Female")
			.extendedDescription("Skates, no Shield: Stand close to Female");


	private final ModifiableCallout<BuffApplied> midGlitchO = ModifiableCallout.<BuffApplied>durationBasedCall("Mid Glitch with O Buddy", "Circle, Close to {tetherBuddy}")
			.extendedDescription("""
					The mid/remote glitch callouts can be modified to call out swap or no swap based on priority.
					There is a variable called 'group' which will be 1 or 2 (or 0 if you had no tether buddy which shouldn't happen).
					For example, you could use the callout '{(group == 2) ? "Right, Furthest" : "Left, Closest"}' to make the call
					switch what it says based on your group.
					""");
	private final ModifiableCallout<BuffApplied> midGlitchS = ModifiableCallout.durationBasedCall("Mid Glitch with □ Buddy", "Square, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> midGlitchT = ModifiableCallout.durationBasedCall("Mid Glitch with △ Buddy", "Triangle, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> midGlitchX = ModifiableCallout.durationBasedCall("Mid Glitch with X Buddy", "X, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchO = ModifiableCallout.durationBasedCall("Remote Glitch with O Buddy", "Circle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchS = ModifiableCallout.durationBasedCall("Remote Glitch with □ Buddy", "Square, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchT = ModifiableCallout.durationBasedCall("Remote Glitch with △ Buddy", "Triangle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchX = ModifiableCallout.durationBasedCall("Remote Glitch with X Buddy", "X, far from {tetherBuddy}");

	private final ModifiableCallout<AbilityCastStart> eyeLaserStart = ModifiableCallout.durationBasedCall("Eye Laser Starts Casting", "Eye Laser");
	private final ModifiableCallout<AbilityUsedEvent> eyeLaserDone = new ModifiableCallout<AbilityUsedEvent>("Eye Laser Done Casting", "Knockback Stacks").disabledByDefault();
	// TODO icon
	private final ModifiableCallout<HeadMarkerEvent> glitchStacksMid = new ModifiableCallout<HeadMarkerEvent>("Glitch Stacks Mid", "Close Stacks on {stackPlayers[0]} and {stackPlayers[1]}", 10_000).statusIcon(0xD63)
			.extendedDescription("""
					Example to have safe spots called out for west plant:
					{unsafe.plusQuads(-1)}, {unsafe.opposite()}
					For east plant:
					{unsafe.opposite()}, {unsafe.plusQuads(1)}
					""");
	// TODO icon
	private final ModifiableCallout<HeadMarkerEvent> glitchStacksFar = new ModifiableCallout<HeadMarkerEvent>("Glitch Stacks Far", "Far Stacks on {stackPlayers[0]} and {stackPlayers[1]}", 10_000).statusIcon(0xD64)
			.extendedDescription("""
					Example to have safe spots called out:
					{unsafe.plusQuads(-1)}, {unsafe.plusQuads(1)}
					""");
	private final ModifiableCallout<HeadMarkerEvent> furthestFromEyeSwap = new ModifiableCallout<HeadMarkerEvent>("Glitch Stack Swap", "{swapper} and {swapee} Swap", 7_000)
			.disabledByDefault()
			.extendedDescription("This call will call out the players who need to swap using the 'furthest stack marker from eye' strategy.");
	private final ModifiableCallout<HeadMarkerEvent> furthestFromEyeNoSwap = new ModifiableCallout<HeadMarkerEvent>("Glitch Stack No Swap", "No Swap", 7_000)
			.disabledByDefault()
			.extendedDescription("See above.");

	private final ModifiableCallout<AbilityCastStart> limitlessSynergy = new ModifiableCallout<>("Limitless Synergy", "Limitless Synergy");

	private final ModifiableCallout<TetherEvent> limitlessSynergyGrabTether = new ModifiableCallout<>("Limitless Synergy as Tank: Grab Tether", "Grab Tethers");
	private final ModifiableCallout<TetherEvent> limitlessSynergyGiveTether = new ModifiableCallout<>("Limitless Synergy as non-Tank: Give Away Tether", "Give Tethers to Tanks");
	private final ModifiableCallout<BuffApplied> limitlessSynergyFlare = new ModifiableCallout<>("Limitless Synergy: Flare", "Out for Flare");
	private final ModifiableCallout<BuffApplied> limitlessSynergyNoFlare = new ModifiableCallout<>("Limitless Synergy: No Flare", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyStack = new ModifiableCallout<>("Limitless Synergy: Stack", "Stack");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyDontStack = new ModifiableCallout<AbilityUsedEvent>("Limitless Synergy: Don't Stack", "Don't Stack")
			.extendedDescription("This trigger activates if you were chosen for beyond defense, but not if you were clipped by someone else's hit.");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyDontStackMistake = new ModifiableCallout<AbilityUsedEvent>("Limitless Synergy: Don't Stack (Mistake)", "Stack")
			.extendedDescription("This trigger is activated instead of the one above if you were clipped by someone else's Beyond Defense hit.");

	@NpcCastCallout(0x7B22)
	private final ModifiableCallout<AbilityCastStart> cosmoMemory = new ModifiableCallout<>("Cosmo Memory", "Raidwide");


	private final ModifiableCallout<AbilityUsedEvent> waveRepeaterMoveIn1 = new ModifiableCallout<>("Wave Repeater: First Move In", "In");
	private final ModifiableCallout<AbilityUsedEvent> waveRepeaterMoveIn2 = new ModifiableCallout<>("Wave Repeater: Second Move In", "In, Dodge Hand");

	private final ModifiableCallout<BuffApplied> sniperCannonCall = ModifiableCallout.<BuffApplied>durationBasedCall("Sniper Cannon", "Sniper Soon").statusIcon(0xD61);
	private final ModifiableCallout<BuffApplied> highPoweredSniperCannonCall = ModifiableCallout.<BuffApplied>durationBasedCall("High-powered Sniper Cannon", "High-Power Sniper - Stack on {nothings[0]} or {nothings[1]}").statusIcon(0xD62);
	private final ModifiableCallout<BuffApplied> noSniperCannonCall = ModifiableCallout.durationBasedCall("No Sniper Cannon", "Nothing - Stack on {hpSnipers[0]} or {hpSnipers[1]}");


	private static final Position center = Position.of2d(100, 100);
	private static final ArenaPos pos = new ArenaPos(100, 100, 5, 5);

	private final XivState state;
	private final StatusEffectRepository buffs;
	private final JobSortSetting groupPrioJobSort;
	private final MultiSlotAutomarkSetting<TwoGroupsOfFour> markSettings;
	private final MultiSlotAutomarkSetting<PsMarkerGroup> psMarkSettings;
	private final MultiSlotAutomarkSetting<PsMarkerGroup> psMarkSettingsFar;
	private final MultiSlotAutomarkSetting<WrothStyleAssignment> sniperAmSettings;
	private final BooleanSetting looperAM;
	private final BooleanSetting psAmEnable;
	private final BooleanSetting pantoAmEnable;
	private final BooleanSetting sniperAmEnable;
	private final BooleanSetting monitorAmEnable;

	public OmegaUltimate(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
		this.state = state;
		this.buffs = buffs;
		String settingKeyBase = "triggers.omega-ultimate.";
		groupPrioJobSort = new JobSortSetting(pers, settingKeyBase + "groupsPrio", state);
		markSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.am-slot-settings", TwoGroupsOfFour.class, Map.of(
				TwoGroupsOfFour.GROUP1_NUM1, MarkerSign.ATTACK1,
				TwoGroupsOfFour.GROUP1_NUM2, MarkerSign.ATTACK2,
				TwoGroupsOfFour.GROUP1_NUM3, MarkerSign.ATTACK3,
				TwoGroupsOfFour.GROUP1_NUM4, MarkerSign.ATTACK4,
				TwoGroupsOfFour.GROUP2_NUM1, MarkerSign.BIND1,
				TwoGroupsOfFour.GROUP2_NUM2, MarkerSign.BIND2,
				TwoGroupsOfFour.GROUP2_NUM3, MarkerSign.BIND3,
				TwoGroupsOfFour.GROUP2_NUM4, MarkerSign.CROSS
		));
		psMarkSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.ps-am-slot-settings", PsMarkerGroup.class, Map.of(
				PsMarkerGroup.GROUP1_CIRCLE, MarkerSign.ATTACK1,
				PsMarkerGroup.GROUP1_TRIANGLE, MarkerSign.ATTACK2,
				PsMarkerGroup.GROUP1_SQUARE, MarkerSign.ATTACK3,
				PsMarkerGroup.GROUP1_X, MarkerSign.ATTACK4,
				PsMarkerGroup.GROUP2_CIRCLE, MarkerSign.BIND1,
				PsMarkerGroup.GROUP2_TRIANGLE, MarkerSign.BIND2,
				PsMarkerGroup.GROUP2_SQUARE, MarkerSign.BIND3,
				PsMarkerGroup.GROUP2_X, MarkerSign.CROSS
		));
		psMarkSettingsFar = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.ps-far-am-slot-settings", PsMarkerGroup.class, Map.of(
				PsMarkerGroup.GROUP1_CIRCLE, MarkerSign.ATTACK1,
				PsMarkerGroup.GROUP1_TRIANGLE, MarkerSign.ATTACK2,
				PsMarkerGroup.GROUP1_SQUARE, MarkerSign.ATTACK3,
				PsMarkerGroup.GROUP1_X, MarkerSign.ATTACK4,
				PsMarkerGroup.GROUP2_CIRCLE, MarkerSign.BIND1,
				PsMarkerGroup.GROUP2_TRIANGLE, MarkerSign.BIND2,
				PsMarkerGroup.GROUP2_SQUARE, MarkerSign.BIND3,
				PsMarkerGroup.GROUP2_X, MarkerSign.CROSS
		));
		psMarkSettingsFar.copyDefaultsFrom(psMarkSettings);
		sniperAmSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.sniper-am-settings", WrothStyleAssignment.class, Map.of(
				WrothStyleAssignment.SPREAD_1, MarkerSign.ATTACK1,
				WrothStyleAssignment.SPREAD_2, MarkerSign.ATTACK2,
				WrothStyleAssignment.SPREAD_3, MarkerSign.ATTACK3,
				WrothStyleAssignment.SPREAD_4, MarkerSign.ATTACK4,
				WrothStyleAssignment.STACK_1, MarkerSign.BIND1,
				WrothStyleAssignment.STACK_2, MarkerSign.IGNORE1,
				WrothStyleAssignment.NOTHING_1, MarkerSign.BIND2,
				WrothStyleAssignment.NOTHING_2, MarkerSign.IGNORE2
		));
		looperAM = new BooleanSetting(pers, settingKeyBase + "looper-am.enabled", false);
		pantoAmEnable = new BooleanSetting(pers, settingKeyBase + "panto-am.enabled", false);
		sniperAmEnable = new BooleanSetting(pers, settingKeyBase + "sniper-am.enabled", false);
		psAmEnable = new BooleanSetting(pers, settingKeyBase + "ps-marker-am.enabled", false);
		monitorAmEnable = new BooleanSetting(pers, settingKeyBase + "monitor-am.enabled", false);
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.OmegaProtocol);
	}

	private static boolean isLineDebuff(BuffApplied ba) {
		return NumberInLine.debuffToLine(ba) != NumberInLine.UNKNOWN;
	}

	private enum NumberInLine {
		FIRST(1, 0xBBC),
		SECOND(2, 0xBBD),
		THIRD(3, 0xBBE),
		FOURTH(4, 0xD7B),
		UNKNOWN;

		private final int id;
		private final int lineNumber;

		NumberInLine(int lineNumber, int id) {
			this.id = id;
			this.lineNumber = lineNumber;
		}

		NumberInLine() {
			this.id = -1;
			this.lineNumber = -1;
		}

		int lineId() {
			return id;
		}

		static NumberInLine debuffToLine(BuffApplied ba) {
			int id = (int) ba.getBuff().getId();
			for (NumberInLine value : values()) {
				if (id == value.id) {
					return value;
				}
			}
			return UNKNOWN;
		}

		static NumberInLine groupToLine(TwoGroupsOfFour group) {
			return forNumber(group.getNumber());
		}

		static NumberInLine forNumber(int number) {
			if (number < 1 || number > 4) {
				throw new IllegalArgumentException(String.valueOf(number));
			}
			return values()[number - 1];
		}
	}

	private @Nullable XivPlayerCharacter findLineBuddy() {
		return findLineBuddy(state.getPlayer());
	}

	private @Nullable XivPlayerCharacter findLineBuddy(XivPlayerCharacter xpc) {
		BuffApplied ba = buffs.findStatusOnTarget(xpc, OmegaUltimate::isLineDebuff);
		if (ba == null) {
			log.warn("No buff!");
			return null;
		}
		return buffs.getBuffs().stream()
				.filter(buff -> buff.buffIdMatches(ba.getBuff().getId()))
				.map(buff -> (XivPlayerCharacter) buff.getTarget())
				.filter(target -> !target.equals(xpc))
				.findFirst()
				.orElse(null);
	}
//
//	//returns first player with line debuff
//	private XivPlayerCharacter supPlayerFromLine(NumberInLine il) {
//		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isTank() || p.getJob().isHealer()).toList();
//		int lineId = il.lineId();
//
//		return players.stream()
//				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
//				.findFirst()
//				.orElse(null);
//	}
//
//	//returns first player with line debuff
//	private XivPlayerCharacter dpsPlayerFromLine(NumberInLine il) {
//		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isDps()).toList();
//		int lineId = il.lineId();
//
//		return players.stream()
//				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
//				.findFirst()
//				.orElse(null);
//	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	private Map<TwoGroupsOfFour, XivPlayerCharacter> getLineGroups() {
		Map<NumberInLine, List<XivPlayerCharacter>> groups = new EnumMap<>(NumberInLine.class);
		buffs.getBuffs().forEach(item -> {
			NumberInLine num = NumberInLine.debuffToLine(item);
			if (num != null && num != NumberInLine.UNKNOWN) {
				XivCombatant target = item.getTarget();
				if (target instanceof XivPlayerCharacter xpc) {
					groups.computeIfAbsent(num, unused -> new ArrayList<>()).add(xpc);
				}
			}
		});
		Map<TwoGroupsOfFour, XivPlayerCharacter> finalMap = new EnumMap<>(TwoGroupsOfFour.class);
		groups.forEach((k, v) -> {
			v.sort(groupPrioJobSort.getPlayerJailSortComparator());
			finalMap.put(TwoGroupsOfFour.forNumbers(1, k.lineNumber), v.get(0));
			finalMap.put(TwoGroupsOfFour.forNumbers(2, k.lineNumber), v.get(1));
		});
		return finalMap;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> programLoopGather = SqtTemplates.sq(50_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x7B03),
			(e1, s) -> {
				log.info("Program Loop: Start");
				s.waitEvents(8, BuffApplied.class, OmegaUltimate::isLineDebuff);
				s.waitMs(50);
				s.accept(new ProgramLoopAssignments(getLineGroups()));
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> programLoopExecute = SqtTemplates.sq(50_000, ProgramLoopAssignments.class, unused -> true,
			(e1, s) -> {
				BuffApplied looperDebuff = getBuffs().findStatusOnTarget(getState().getPlayer(), 0xD80);
				TwoGroupsOfFour myGroup = e1.forPlayer(getState().getPlayer());
				NumberInLine num = NumberInLine.forNumber(myGroup.getNumber());
				XivPlayerCharacter buddy = e1.getAssignments().get(myGroup.getCounterpart());
				Map<String, Object> params = Map.of("buddy", buddy == null ? "error" : buddy);
				switch (num) {
					case FIRST -> s.updateCall(firstInLineLoop.getModified(params));
					case SECOND -> s.updateCall(secondInLineLoop.getModified(params));
					case THIRD -> s.updateCall(thirdInLineLoop.getModified(params));
					case FOURTH -> s.updateCall(fourthInLineLoop.getModified(params));
					case UNKNOWN -> {
						log.error("Unknown number!");
						return;
					}
				}
				log.info("Loop start: player has {}, buddy {}", num, buddy.getName());
				s.waitMs(3000);
				if (num == NumberInLine.THIRD) {
					s.updateCall(thirdInLineTether.getModified(params));
				}
				else {
					s.waitEvent(TetherEvent.class, te -> true);
					if (num == NumberInLine.FIRST) {
						s.updateCall(firstInLineTower.getModified(looperDebuff, params));
					}
					else {
						s.accept(firstNotYou.getModified(params));
					}
				}

				//First tower goes off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("First tower done");
				if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTower.getModified(looperDebuff, params));
				}
				else if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTether.getModified(params));
				}
				else {
					s.accept(secondNotYou.getModified(params));
				}

				//Second tower goes off
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("Second tower done");
				if (num == NumberInLine.THIRD) {
					s.updateCall(thirdInLineTower.getModified(looperDebuff, params));
				}
				else if (num == NumberInLine.FIRST) {
					s.updateCall(firstInLineTether.getModified(params));
				}
				else {
					s.accept(thirdNotYou.getModified(params));
				}

				//Third tower goes off
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("Third tower done");
				if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTower.getModified(looperDebuff, params));
				}
				else if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTether.getModified(params));
				}
				else {
					s.accept(fourthNotYou.getModified(params));
				}
			});

	private boolean isLooperAmEnabled() {
		return looperAM.get();
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> programLoopAM = SqtTemplates.sq(50_000, ProgramLoopAssignments.class, unused -> true,
			(e1, s) -> {

				if (isLooperAmEnabled()) {
					e1.getAssignments().forEach((assignment, player) -> {
						MarkerSign marker = getMarkSettings().getMarkerFor(assignment);
						if (marker != null) {
							s.accept(new SpecificAutoMarkRequest(player, marker));
						}
					});
					s.waitMs(35_000);
					s.accept(new ClearAutoMarkRequest());
				}
			});

	// TODO: make this centralized somewhere
	private volatile boolean amActive;

	@HandleEvents
	public void checkAm(EventContext context, SpecificAutoMarkRequest samr) {
		amActive = true;
	}

	@HandleEvents
	public void clearedAm(EventContext context, ClearAutoMarkRequest samr) {
		amActive = false;
	}

	@HandleEvents
	public void clearAm(EventContext context, DutyRecommenceEvent event) {
		if (amActive) {
			context.accept(new ClearAutoMarkRequest());
			amActive = false;
		}
	}

	@SuppressWarnings("ReuseOfLocalVariable")
	@AutoFeed
	private final SequentialTrigger<BaseEvent> pantokratorSq = SqtTemplates.sq(50_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x7B0B),
			(e1, s) -> {
				log.info("Program Loop: Start");
				s.waitEvents(8, BuffApplied.class, OmegaUltimate::isLineDebuff);
				s.waitMs(50);
				PantoAssignments assignments = new PantoAssignments(getLineGroups());
				s.accept(assignments);
				BuffApplied myLineBuff = getBuffs().findStatusOnTarget(getState().getPlayer(), OmegaUltimate::isLineDebuff);
				NumberInLine number = NumberInLine.debuffToLine(myLineBuff);
				s.waitMs(100);
				BuffApplied guidedMissile = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD60, 0xDA7, 0xDA8, 0xDA9));
				BuffApplied waveCannon = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xDB3, 0xDB4, 0xDB5, 0xDB6));
				if (guidedMissile == null) {
					log.error("No guided missile!");
					return;
				}
				else if (waveCannon == null) {
					log.error("No wave cannon!");
					return;
				}

				// Guided missile is LONGER
				// First group is 12 missile, 24

				XivPlayerCharacter buddy = findLineBuddy();
				Map<String, Object> params = Map.of("missile", guidedMissile, "cannon", waveCannon, "buddy", buddy == null ? "error" : buddy);
				switch (number) {
					case FIRST -> s.updateCall(pantoFirstInLine.getModified(myLineBuff, params));
					case SECOND -> s.updateCall(pantoSecondInLine.getModified(myLineBuff, params));
					case THIRD -> s.updateCall(pantoThirdInLine.getModified(myLineBuff, params));
					case FOURTH -> s.updateCall(pantoFourthInLine.getModified(myLineBuff, params));
					case UNKNOWN -> {
						log.error("Unknown number in line!");
						return;
					}
				}

				BaseEvent last = myLineBuff;

				for (int i = 1; i <= 4; i++) {
					// TODO doesn't stick on screen

					log.info("Iteration: {} vs {}", number.lineNumber, i);
					if (number.lineNumber == i) {
						switch (number) {
							case SECOND -> s.updateCall(pantoSecondInLineOut.getModified(last, params));
							case THIRD -> s.updateCall(pantoThirdInLineOut.getModified(last, params));
							case FOURTH -> s.updateCall(pantoFourthInLineOut.getModified(last, params));
						}
					}
					else if (number.lineNumber + 1 == i) {
						switch (number) {
							case FIRST -> s.updateCall(pantoFirstGoBackIn.getModified(last, params));
							case SECOND -> s.updateCall(pantoSecondGoBackIn.getModified(last, params));
							case THIRD -> s.updateCall(pantoThirdGoBackIn.getModified(last, params));
						}
					}
					else {
						switch (i) {
							case 2 -> s.accept(pantoSecondNotYou.getModified(params));
							case 3 -> s.accept(pantoThirdNotYou.getModified(params));
							case 4 -> s.accept(pantoFourthNotYou.getModified(params));
						}

					}
					last = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0E) && aue.isFirstTarget());
					s.waitMs(100);
				}
				s.waitMs(1500);
				if (getState().playerJobMatches(Job::isTank)) {
					s.updateCall(pantoCleave1asTank.getModified(last));
				}
				else {
					s.updateCall(pantoCleave1asNonTank.getModified(last));
					List<HeadMarkerEvent> hme = s.waitEventsQuickSuccession(3, HeadMarkerEvent.class, he -> true, Duration.ofMillis(250));
					last = hme.get(0);
					@Nullable HeadMarkerEvent myHm = hme.stream().filter(h -> h.getTarget().isThePlayer()).findFirst().orElse(null);
					if (myHm != null) {
						s.updateCall(pantoCleave1withMarker.getModified(myHm, params));
					}
					else {
						s.updateCall(pantoCleave1noMarker.getModified(last, params));
					}
					last = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B11) && aue.isFirstTarget());
					if (myHm == null) {
						s.updateCall(pantoCleave2hadNoMarker.getModified(last, params));
					}
					else {
						s.updateCall(pantoCleave2hadMarker.getModified(last, params));
					}
				}
			});

	private boolean isPantoAmEnabled() {
		return pantoAmEnable.get();
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> pantoAm = SqtTemplates.sq(50_000, PantoAssignments.class, unused -> true,
			(e1, s) -> {
				if (isPantoAmEnabled()) {
					e1.getAssignments().forEach((assignment, player) -> {
						MarkerSign marker = getMarkSettings().getMarkerFor(assignment);
						if (marker != null) {
							s.accept(new SpecificAutoMarkRequest(player, marker));
						}
					});
					s.waitMs(35_000);
					s.accept(new ClearAutoMarkRequest());
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> psCollectorSq = SqtTemplates.sq(40_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B3F),
			(e1, s) -> {
				Map<PsMarkerGroup, XivPlayerCharacter> headmarkers = new EnumMap<>(PsMarkerGroup.class);
				// We now have a mapping from the headmarker offset to the players with that ID
				s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hm -> hm.getTarget().isPc(), Duration.ofSeconds(1))
						.stream()
						// Group by marker offset
						.collect(Collectors.groupingBy(HeadMarkerEvent::getMarkerOffset))
						.forEach((key, value) -> {
							List<XivPlayerCharacter> sorted = value
									.stream()
									.map(HeadMarkerEvent::getTarget)
									.map(XivPlayerCharacter.class::cast)
									.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
									.limit(2)
									.toList();
							PsMarkerGroup firstAssignment = (switch (key) {
								case 393 -> PsMarkerGroup.GROUP1_CIRCLE;
								case 394 -> PsMarkerGroup.GROUP1_TRIANGLE;
								case 395 -> PsMarkerGroup.GROUP1_SQUARE;
								case 396 -> PsMarkerGroup.GROUP1_X;
								default -> throw new IllegalArgumentException("Unknown marker");
							});
							// Doing it like this so that it doesn't break if you're missing a player
							for (int i = 0; i < sorted.size(); i++) {
								XivPlayerCharacter player = sorted.get(i);
								if (i == 0) {
									headmarkers.put(firstAssignment, player);
								}
								else {
									PsMarkerGroup secondAssignment = firstAssignment.getCounterpart();
									headmarkers.put(secondAssignment, player);
								}
							}
						});
				log.info("Headmarkers map: {}", headmarkers);
				boolean mid = getBuffs().getBuffs().stream().filter(ba -> ba.buffIdMatches(0xD63, 0xD64)).findFirst().map(ba -> ba.buffIdMatches(0xD63)).stream().findFirst().orElse(false);
				s.accept(new PsMarkerAssignments(headmarkers, mid));
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> psMarkerAm = SqtTemplates.sq(50_000, PsMarkerAssignments.class, e -> true,
			(e1, s) -> {
				if (!getPsAmEnable().get()) {
					return;
				}
				boolean mid = e1.isMid();
				e1.getAssignments().forEach((assignment, player) -> {
					MarkerSign marker = (mid ? getPsMarkSettings() : getPsMarkSettingsFarGlitch()).getMarkerFor(assignment);
					log.info("PS Marker: {} on {}", marker, player);
					if (marker != null) {
						s.accept(new SpecificAutoMarkRequest(player, marker));
					}
				});
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B30));
				s.accept(new ClearAutoMarkRequest());
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> midRemoteGlitch = SqtTemplates.sq(50_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B3F),
			(e1, s) -> {
				s.updateCall(checkMfPattern.getModified(e1));
				PsMarkerAssignments assignments = s.waitEvent(PsMarkerAssignments.class);

				PsMarkerGroup myAssignment = assignments.forPlayer(getState().getPlayer());
				XivCombatant buddy = assignments.getPlayerForAssignment(myAssignment.getCounterpart());

				int group;
				if (buddy == null) {
					group = 0;
				}
				else {
					group = shouldSwap(getState().getPlayer(), (XivPlayerCharacter) buddy) ? 2 : 1;
				}

				// TODO: get all the ability IDs instead of using the buff
//				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B25, 0x7B2D));
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x68C));


				BuffApplied status = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD63, 0xD64));
				boolean mid = assignments.isMid();
				Map<String, Object> params = buddy == null ? Map.of() : Map.of("tetherBuddy", buddy, "mid", mid, "group", group);
				s.updateCall((switch (myAssignment) {
					case GROUP1_CIRCLE, GROUP2_CIRCLE -> mid ? midGlitchO : remoteGlitchO;
					case GROUP1_TRIANGLE, GROUP2_TRIANGLE -> mid ? midGlitchT : remoteGlitchT;
					case GROUP1_SQUARE, GROUP2_SQUARE -> mid ? midGlitchS : remoteGlitchS;
					case GROUP1_X, GROUP2_X -> mid ? midGlitchX : remoteGlitchX;
				}).getModified(status, params));

				AbilityCastStart eyeLaser = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B21));
				s.updateCall(eyeLaserStart.getModified(eyeLaser, params));
				AbilityUsedEvent eyeLaserUsed = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B21));
				s.updateCall(eyeLaserDone.getModified(eyeLaserUsed, params));

				List<HeadMarkerEvent> stackMarkers = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 77);
				List<XivPlayerCharacter> stackPlayers = stackMarkers.stream()
						.map(HeadMarkerEvent::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
						.toList();
				params = new HashMap<>(params);
				params.put("stackPlayers", stackPlayers);
				XivCombatant male = getState().npcById(15713);
				ArenaSector unsafeSpot = pos.forCombatant(male);
				params.put("unsafe", unsafeSpot);
				s.updateCall((mid ? glitchStacksMid : glitchStacksFar).getModified(stackMarkers.get(0), params));
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> psStackLocationSwap = SqtTemplates.sq(50_000, PsMarkerAssignments.class, e -> true,
			(e1, s) -> {
				boolean mid = e1.isMid();
				List<HeadMarkerEvent> stackMarkers = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 77);
				s.waitThenRefreshCombatants(100);
				List<XivPlayerCharacter> stackPlayers = stackMarkers.stream()
						.map(HeadMarkerEvent::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.toList();
				// Find optical unit
				XivCombatant eye = getState().npcById(15716);
				Position eyePos = eye.getPos();
				// This gives us basically the "eye's view" - that is, all positions are normalized to the eye
				Map<XivPlayerCharacter, Position> normalized = stackPlayers.stream().collect(Collectors.toMap(
						Function.identity(),
						player -> player.getPos().normalizedTo(eyePos)));
				// Check if both on one side
				s.waitMs(1000);
				if (normalized.values().stream().allMatch(p -> p.x() < 0)
				    || normalized.values().stream().allMatch(p -> p.x() > 0)) {
					normalized.entrySet().stream().max(Comparator.comparing(e -> e.getValue().distanceFrom2D(eyePos)))
							.ifPresent(furthest -> {
								XivPlayerCharacter swapper = furthest.getKey();
								XivPlayerCharacter swapee = e1.getPlayerForAssignment(e1.forPlayer(swapper).getCounterpart());
								Map<String, Object> params = Map.of("swapper", swapper, "swapee", swapee, "mid", mid);
								s.updateCall(furthestFromEyeSwap.getModified(stackMarkers.get(0), params));
							});
				}
				else {
					s.updateCall(furthestFromEyeNoSwap.getModified(stackMarkers.get(0)));
				}
			});

	@SuppressWarnings("ConstantValue") // clarity
	@AutoFeed
	private final SequentialTrigger<BaseEvent> partySynergySafeSpot = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B3F),
			(e1, s) -> {
				// Wait for bosses to go untargetable
				s.waitEvent(TargetabilityUpdate.class, tu -> tu.getTarget().equals(e1.getSource()) && !tu.isTargetable());
				log.info("Party Synergy Safe Spot: Waiting for boss data...");
				s.waitMs(2000);
				XivCombatant omegaF;
				XivCombatant omegaM;
				do {
					s.waitThenRefreshCombatants(50);
					omegaF = getState().npcById(15715);
					// TODO: is it more reliable to use the ID ordering, or positions?
					omegaM = getState().npcById(15714);
					// Validate date
				} while (omegaF == null || omegaM == null
				         || omegaF.getPos() == null || omegaM.getPos() == null
				         || (omegaF.getPos().distanceFrom2D(center) < 9) || omegaM.getPos().distanceFrom2D(center) < 9
				         || omegaF.getWeaponId() == -1 || omegaM.getWeaponId() == -1);
				log.info("Party Synergy Safe Spot: Sleeping");
				s.waitMs(200);
				s.waitThenRefreshCombatants(200);
				log.info("Party Synergy Safe Spot: Done Sleeping");
				{
					omegaF = getState().getLatestCombatantData(omegaF);
					omegaM = getState().getLatestCombatantData(omegaM);
					log.info("F: {}", omegaF);
					log.info("M: {}", omegaM);
					short fw = omegaF.getWeaponId();
					short mw = omegaM.getWeaponId();
					log.info("F weapon: {}; M weapon: {}", fw, mw);
					boolean inF = fw == 4;
					boolean inM = mw == 4;
					if (inF && inM) {
						s.updateCall(partySynergyBothIn.getModified());
					}
					else if (inF && !inM) {
						s.updateCall(partySynergyFinMout.getModified());
					}
					else if (!inF && inM) {
						s.updateCall(partySynergyFoutMin.getModified());
					}
					else {
						s.updateCall(partySynergyBothOut.getModified());
					}
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitlessSynergySq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B38),
			(e1, s) -> {
				log.info("Limitless Synergy Start");
				s.updateCall(limitlessSynergy.getModified(e1));
				TetherEvent tether = s.waitEvent(TetherEvent.class, t -> t.tetherIdMatches(0x54));
				if (getState().playerJobMatches(Job::isTank)) {
					s.updateCall(limitlessSynergyGrabTether.getModified(tether));
				}
				else {
					s.updateCall(limitlessSynergyGiveTether.getModified(tether));
				}
				List<BuffApplied> flares = s.waitEvents(3, BuffApplied.class, buff -> buff.buffIdMatches(0x232));
				BuffApplied myBuff = flares.stream().filter(b -> b.getTarget().isThePlayer()).findAny().orElse(null);
				if (myBuff != null) {
					s.updateCall(limitlessSynergyFlare.getModified(myBuff));
				}
				else {
					s.updateCall(limitlessSynergyNoFlare.getModified(flares.get(0)));
					List<AbilityUsedEvent> peopleThatGotHit = s.collectAoeHits(aue -> aue.abilityIdMatches(0x7B28));
					AbilityUsedEvent firstEvent = peopleThatGotHit.get(0);
					if (firstEvent.getTarget().isThePlayer()) {
						s.updateCall(limitlessSynergyDontStack.getModified(firstEvent));
					}
					else {
						peopleThatGotHit.stream()
								.filter(item -> item.getTarget().isThePlayer())
								.findAny()
								.ifPresentOrElse(item -> s.updateCall(limitlessSynergyDontStackMistake.getModified(item)),
										() -> s.updateCall(limitlessSynergyStack.getModified(firstEvent)));
					}
				}
			});

	@SuppressWarnings("SuspiciousMethodCalls")
	@AutoFeed
	private final SequentialTrigger<BaseEvent> sniperCannonSq = SqtTemplates.sq(30_000,
			BuffApplied.class, ba -> ba.buffIdMatches(0xD61),
			(e1, s) -> {
				s.waitMs(100);
				// The buffs have already gone out, there just wasn't a good pre-tell in the log
				List<BuffApplied> sniper = new ArrayList<>(4);
				List<BuffApplied> hpSniper = new ArrayList<>(2);
				List<XivPlayerCharacter> nothing = new ArrayList<>(getState().getPartyList());
				Mutable<BuffApplied> playerBuffM = new MutableObject<>();
				getBuffs().getBuffs().forEach(ba -> {
					if (ba.buffIdMatches(0xD61)) {
						sniper.add(ba);
						nothing.remove(ba.getTarget());
					}
					else if (ba.buffIdMatches(0xD62)) {
						hpSniper.add(ba);
						nothing.remove(ba.getTarget());
					}
					else {
						return;
					}
					if (ba.getTarget().isThePlayer()) {
						playerBuffM.setValue(ba);
					}
				});
				List<XivPlayerCharacter> sniperPlayers = sniper.stream()
						.map(BuffApplied::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
						.toList();
				List<XivPlayerCharacter> hpSniperPlayers = hpSniper.stream()
						.map(BuffApplied::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
						.toList();
				List<XivPlayerCharacter> nothingPlayers = nothing.stream()
						.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
						.toList();
				BuffApplied playerBuff = playerBuffM.getValue();
				Map<String, Object> params = Map.of("snipers", sniperPlayers, "hpSnipers", hpSniperPlayers, "nothings", nothingPlayers);
				if (playerBuff == null) {
					s.updateCall(noSniperCannonCall.getModified(e1, params));
				}
				else {
					if (playerBuff.buffIdMatches(0xD61)) {
						s.updateCall(sniperCannonCall.getModified(playerBuff, params));
					}
					else {
						s.updateCall(highPoweredSniperCannonCall.getModified(playerBuff, params));
					}
				}
				if (getSniperAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
					s.waitMs(100);
					MultiSlotAutoMarkHandler<WrothStyleAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getSniperAmSettings());
					handler.processRange(sniperPlayers, WrothStyleAssignment.SPREAD_1, WrothStyleAssignment.SPREAD_4);
					handler.processRange(hpSniperPlayers, WrothStyleAssignment.STACK_1, WrothStyleAssignment.STACK_2);
					handler.processRange(nothingPlayers, WrothStyleAssignment.NOTHING_1, WrothStyleAssignment.NOTHING_2);
				}
				// Wait for third ring to go off
				// 7B4F = initial cast (inner circle)
				// 7B50 = second circle
				// 7B51 = third circle
				// 7B52 = outermost circle
				AbilityUsedEvent ring1 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B51) && aue.isFirstTarget());
				s.updateCall(waveRepeaterMoveIn1.getModified(ring1));
				AbilityUsedEvent ring2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B51) && aue.isFirstTarget());
				s.updateCall(waveRepeaterMoveIn2.getModified(ring2));
				if (getSniperAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
				}
			});

	private static final int grayDefa = 0xD6D;
	private static final int grayBlue = 0xD6F;
	private static final int grayRed = 0xD6E;
	private static final int defaReal = 0xDC5;
	private static final int stack = 0xDC4;
	private static final int redRot = 0xDC6;
	private static final int blueRot = 0xD65;
	private static final int shortTether = 0xDAF;
	private static final int longTether = 0xD71;
	private static final int shortReg = 0xDC9;
	private static final int longReg = 0xDCa;


	private final ModifiableCallout<AbilityCastStart> hwCastBar = ModifiableCallout.durationBasedCall("Hello World: Initial Cast", "Raidwide");
	private final ModifiableCallout<?> hw0_defaOnBlue = new ModifiableCallout<>("Hello World Start: Defa on Blue", "Blue has Defa");
	private final ModifiableCallout<?> hw0_defaOnRed = new ModifiableCallout<>("Hello World Start: Defa on Red", "Red has Defa");

	private final ModifiableCallout<BuffApplied> hw1a_defaBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Defa", "Blue Rot with Defa").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Defa", "Pass Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_stackBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Stack", "Blue Rot, Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Stack", "Pass Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_defaRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Defa", "Red Rot with Defa").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Defa", "Pass Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_stackRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Stack", "Red Rot, Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Stack", "Pass Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_shortTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Short Tether", "Get Defa").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_shortTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Short Tether", "Get Rot, Shrink Tether").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_shortTetherFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Short Tether, Final Cycle", "Stretch Tether, Get Defa").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_longTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Long Tether", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_longTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Long Tether", "Get Rot, Stretch Tether").autoIcon();


	@AutoFeed
	private final SequentialTrigger<BaseEvent> helloWorldSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, a -> a.abilityIdMatches(0x7B55),
			(e1, s) -> {
				log.info("Hello World: Start");
				s.updateCall(hwCastBar.getModified(e1));
				// https://ff14.toolboxgaming.space/?id=073180945764761&preview=1
				s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(0xDAF), Duration.ofMillis(100));
				log.info("Hello World: Got Initial");
				s.waitMs(100);
				// Should have gray debuffs at this point
				boolean defaIsOnBlue = getBuffs().getBuffs().stream().filter(ba -> ba.buffIdMatches(grayDefa))
						.map(BuffApplied::getTarget)
						.map(target -> {
									if (getBuffs().findStatusOnTarget(target, grayBlue) != null) {
										return true;
									}
									else if (getBuffs().findStatusOnTarget(target, grayRed) != null) {
										return false;
									}
									else {
										return null;
									}
								}
						)
						.filter(Objects::nonNull)
						.findFirst()
						.orElseGet(() -> {
							log.error("Could not determine defamation color!");
							return false;
						});
				if (defaIsOnBlue) {
					s.accept(hw0_defaOnBlue.getModified());
				}
				else {
					s.accept(hw0_defaOnRed.getModified());
				}
				log.info("Hello World Checkpoint 1");
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(redRot));
				for (int i = 1; i <= 4; i++) {
					Map<String, Object> params = Map.of("blueDefa", defaIsOnBlue, "i", i);
					log.info("Hello World Iteration {}", i);
					s.waitMs(200);
					// Should have real debuffs now
					XivPlayerCharacter player = getState().getPlayer();
					BuffApplied defaBuff = getBuffs().findStatusOnTarget(player, defaReal);
					BuffApplied stackBuff = getBuffs().findStatusOnTarget(player, stack);
					BuffApplied blueRotBuff = getBuffs().findStatusOnTarget(player, blueRot);
					BuffApplied redRotBuff = getBuffs().findStatusOnTarget(player, redRot);
					BuffApplied shortTetherBuff = getBuffs().findStatusOnTarget(player, shortTether);
					BuffApplied longTetherBuff = getBuffs().findStatusOnTarget(player, longTether);
					log.info("Found buffs: {} {} {} {} {} {}", defaBuff, stackBuff, blueRotBuff, redRotBuff, shortTetherBuff, longTetherBuff);
					if (stackBuff != null) {
						// Defa on blue = red stack
						if (defaIsOnBlue) {
							s.updateCall(hw1a_stackRed.getModified(stackBuff, params));
							s.waitBuffRemoved(getBuffs(), stackBuff);
							// If the player did not have one, but we have the stack for some reason, find a different one
							if (redRotBuff == null) {
								redRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall(hw1b_stackRed.getModified(redRotBuff, params));
						}
						else {
							s.updateCall(hw1a_stackBlue.getModified(stackBuff, params));
							s.waitBuffRemoved(getBuffs(), stackBuff);
							if (blueRotBuff == null) {
								blueRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall(hw1b_stackBlue.getModified(blueRotBuff, params));
						}
					}
					else if (defaBuff != null) {
						if (defaIsOnBlue) {
							s.updateCall(hw1a_defaBlue.getModified(defaBuff, params));
							s.waitBuffRemoved(getBuffs(), defaBuff);
							if (blueRotBuff == null) {
								blueRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall(hw1b_defaBlue.getModified(blueRotBuff, params));
						}
						else {
							s.updateCall(hw1a_defaRed.getModified(defaBuff, params));
							s.waitBuffRemoved(getBuffs(), defaBuff);
							if (redRotBuff == null) {
								redRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall(hw1b_defaRed.getModified(redRotBuff, params));
						}
					}
					else if (shortTetherBuff != null && shortTetherBuff.getEstimatedRemainingDuration().toMillis() < 25_000) {
						// TODO: should all of them work like this, or just rely on the fact that we pass i in as a param?
						// Leaning towards splitting them
						if (i == 4) {
							s.updateCall(hw1a_shortTetherFinal.getModified(shortTetherBuff, params));
						}
						else {
							s.updateCall(hw1a_shortTether.getModified(shortTetherBuff, params));
						}
						BuffApplied shortRegBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(shortReg));
						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defaReal));
						// TODO: tether follow-up calls should cancel when they are satisfied
						s.updateCall(hw1b_shortTether.getModified(shortRegBuff, params));
					}
					else if (longTetherBuff != null && longTetherBuff.getEstimatedRemainingDuration().toMillis() < 25_000) {
						s.updateCall(hw1a_longTether.getModified(longTetherBuff, params));
						BuffApplied longRegBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(longReg));
						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defaReal));
						s.updateCall(hw1b_longTether.getModified(longRegBuff, params));
					}
					else {
						log.error("I have no idea what debuff you have!");
					}
					// TODO: is this the most reliable place to do this?
					// Wait for "Performance Debugger"
					log.info("Hello World: Waiting for next cycle");
					s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xD69));
				}
			});

	@NpcCastCallout(0x7B64)
	private final ModifiableCallout<AbilityCastStart> criticalError = ModifiableCallout.durationBasedCall("Critical Error", "Raidwide");

	private final ModifiableCallout<BuffApplied> monitorOnYou = new ModifiableCallout<BuffApplied>("Oversampled Wave Cannon on You", "Monitor, Boss Cleaving {bossMonitor}").autoIcon()
			.extendedDescription("""
					In this callout and the one below, the variables `monitorPlayers` and `nonMonitorPlayers` are available.
					To have the callout indicate which monitor you are, use the syntax `{monitorPlayers.indexOf(state.player) + 1}`.
					This uses the prio list on the "Group Swap Priority" tab.
					The parameter "bossMonitor" will indicate where the boss's monitor is cleaving.
					""");
	private final ModifiableCallout<BuffApplied> noMonitorOnYou = new ModifiableCallout<BuffApplied>("Oversampled Wave Cannon on You", "No Monitor, Boss Cleaving {bossMonitor}")
			.extendedDescription("""
					In this callout and the one below, the variables `monitorPlayers` and `nonMonitorPlayers` are available.
					To have the callout indicate which non-monitor you are, use the syntax `{nonMonitorPlayers.indexOf(state.player) + 1}`.
					This uses the prio list on the "Group Swap Priority" tab.
					The parameter "bossMonitor" will indicate where the boss's monitor is cleaving.
					""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> monitorsSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B6B, 0x7B6C),
			(e1, s) -> {
				List<BuffApplied> buffs = getBuffs().getBuffs().stream()
						// TODO: is one of these left, one right?
						.filter(ba -> ba.buffIdMatches(0xD7C, 0xD7D))
						.toList();

				List<XivPlayerCharacter> monitorPlayers = buffs.stream()
						.map(BuffApplied::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getGroupPrioJobSort().getPlayerJailSortComparator())
						.toList();

				List<XivPlayerCharacter> nonMonitorPlayers = new ArrayList<>(getState().getPartyList());
				nonMonitorPlayers.removeAll(monitorPlayers);
				nonMonitorPlayers.sort(getGroupPrioJobSort().getPlayerJailSortComparator());
				ArenaSector bossMonitor = e1.abilityIdMatches(0x7B6B) ? ArenaSector.EAST : ArenaSector.WEST;

				Map<String, Object> params = Map.of("monitorPlayers", monitorPlayers, "nonMonitorPlayers", nonMonitorPlayers, "bossMonitor", bossMonitor);

				buffs.stream()
						.filter(ba -> ba.getTarget().isThePlayer())
						.findFirst()
						.ifPresentOrElse(
								ba -> s.updateCall(monitorOnYou.getModified(ba, params)),
								() -> s.updateCall(noMonitorOnYou.getModified(buffs.get(0), params)));
				// TODO: proper AM settings
				if (getMonitorAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
					s.waitMs(100);
					for (XivPlayerCharacter mp : monitorPlayers) {
						s.accept(new SpecificAutoMarkRequest(mp, MarkerSign.BIND_NEXT));
					}
					for (XivPlayerCharacter nmp : nonMonitorPlayers) {
						s.accept(new SpecificAutoMarkRequest(nmp, MarkerSign.ATTACK_NEXT));
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B6B, 0x7B6C));
					s.accept(new ClearAutoMarkRequest());
				}
			});

	private final ModifiableCallout<AbilityCastStart> waveCannon1Start = ModifiableCallout.durationBasedCall("Wave Cannon 1: Start", "Spread");
	private final ModifiableCallout<AbilityCastStart> waveCannon1Stacks = ModifiableCallout.durationBasedCall("Wave Cannon 1: Stacks", "Stacks");
	private final ModifiableCallout<AbilityCastStart> waveCannon2Start = ModifiableCallout.durationBasedCall("Wave Cannon 2: Start", "Spread Outside");
	private final ModifiableCallout<AbilityCastStart> waveCannon2Stacks = ModifiableCallout.durationBasedCall("Wave Cannon 2: Stacks", "In Now then Stacks");
	private final ModifiableCallout<AbilityCastStart> waveCannon3Start = ModifiableCallout.durationBasedCall("Wave Cannon 3: Start", "Spread Outside");
	private final ModifiableCallout<AbilityCastStart> waveCannon3Stacks = ModifiableCallout.durationBasedCall("Wave Cannon 3: Stacks", "Stacks Outside");
	private final ModifiableCallout<AbilityUsedEvent> waveCannon3MoveInEarly = new ModifiableCallout<AbilityUsedEvent>("Wave Cannon 3: Move In (Center)", "Move In")
			.disabledByDefault()
			.extendedDescription("""
					This calls out when the center goes off.
					If your group is dodging into the second ring instead, disable this call and enable the one below.
					""");
	private final ModifiableCallout<AbilityUsedEvent> waveCannon3MoveInLate = new ModifiableCallout<AbilityUsedEvent>("Wave Cannon 3: Move In (Ring)", "Move In")
			.extendedDescription("""
					This calls out when the second ring goes off (the one just outside the center).
					If your group is dodging into the center circle instead, disable this call and enable the one above.
					""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p4waveCannonSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B81),
			(e1, s) -> {
				// TODO: stack markers? doesn't look like a HM. I see two people targeted with 0x5779 on each wave, could that be it?
				//  - Yes, that is it. The challenge is that it needs to be called out at a reasonable time.
				//    Idea: Secondary optional callout that calls out stacks as soon as they are selected. Primary callout also mentions stack buddies, but this is ~3s later.
				// First set
				{
					s.updateCall(waveCannon1Start.getModified(e1));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					// This cast starts about half a second before the wave cannons go off, so wait a bit
					s.waitMs(300);
					s.updateCall(waveCannon1Stacks.getModified(stackCast));
				}
				// Second set
				{
					AbilityCastStart secondWaveCannonStart = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B81));
					s.updateCall(waveCannon2Start.getModified(secondWaveCannonStart));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					s.waitMs(300);
					s.updateCall(waveCannon2Stacks.getModified(stackCast));
				}
				// Third set
				{
					AbilityCastStart thirdWaveCannonStart = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B81));
					s.updateCall(waveCannon3Start.getModified(thirdWaveCannonStart));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					s.updateCall(waveCannon3Stacks.getModified(stackCast));
					AbilityUsedEvent center = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B4F));
					s.updateCall(waveCannon3MoveInEarly.getModified(center));
					AbilityUsedEvent secondRing = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B50));
					s.updateCall(waveCannon3MoveInLate.getModified(secondRing));
				}
			});

	@NpcCastCallout(0x81AC)
	private final ModifiableCallout<AbilityCastStart> solarRay = ModifiableCallout.durationBasedCall("Solar Ray", "Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> runDynamisDelta = ModifiableCallout.durationBasedCall("Run Dynamis Delta", "Run Dynamis Delta");
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteNear = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote, Near", "Remote with {buddy}, Near World on {nearWorld.target}")
			.statusIcon(0xDB0)
			.extendedDescription("""
					This callout is used when you have a remote tether and either you or your buddy has 'Hello, Near World'.
					You can make the callout say different things based on whether you or your buddy has it using this syntax: {nearWorld.isThePlayer() ? "On You" : "On Buddy"}""");
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteDistant = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote, Distant", "Remote with {buddy}, Distant World on {distWorld.target}")
			.statusIcon(0xDB0)
			.extendedDescription("""
					This callout is used when you have a remote tether and either you or your buddy has 'Hello, Distant World'.
					You can make the callout say different things based on whether you or your buddy has it using this syntax: {distWorld.isThePlayer() ? "On You" : "On Buddy"}""");
	private final ModifiableCallout<TetherEvent> runDynamisDeltaLocal = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Local", "Local with {buddy}")
			.statusIcon(0xD70);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> runDynamisDeltaSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B88),
			(e1, s) -> {
				log.info("Dynamis Delta: Start");
				s.updateCall(runDynamisDelta.getModified(e1));
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(200, 201), Duration.ofMillis(300));
				log.info("Dynamic Delta: Tethers: {}", tethers);
				s.waitMs(100);
				BuffApplied helloNearWorld = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD72));
				BuffApplied helloDistantWorld = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD73));
				TetherEvent myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst().orElseThrow(() -> new RuntimeException("Couldn't find player's tether!"));
				Map<String, Object> params = Map.of("nearWorld", helloNearWorld, "distWorld", helloDistantWorld, "buddy", myTether.getTargetMatching(cbt -> !cbt.isThePlayer()));
				boolean remote = myTether.tetherIdMatches(0xDB0);
				if (remote) {
					boolean withNear = myTether.eitherTargetMatches(helloNearWorld.getTarget());
					if (withNear) {
						runDynamisDeltaRemoteNear.getModified(myTether, params);
					}
					else {
						runDynamisDeltaRemoteDistant.getModified(myTether, params);
					}
				}
				else {
					s.updateCall(runDynamisDeltaLocal.getModified(myTether, params));
				}
			});


	public JobSortSetting getGroupPrioJobSort() {
		return groupPrioJobSort;
	}

	public boolean shouldSwap(XivPlayerCharacter first, XivPlayerCharacter second) {
		int result = groupPrioJobSort.getPlayerJailSortComparator().compare(first, second);
		if (result == 0) {
			log.warn("Players had the same job and name! Falling back to ID.");
			result = Long.compare(first.getId(), second.getId());
			if (result == 0) {
				throw new IllegalArgumentException(String.format("Both entities had the same ID! (%s) vs (%s)", first, second));
			}
		}
		return result > 0;
	}

	@SuppressWarnings("unused")
	public List<XivPlayerCharacter> sortAccordingToJobPrio(XivPlayerCharacter first, XivPlayerCharacter
			second) {
		if (shouldSwap(first, second)) {
			return List.of(first, second);
		}
		else {
			return List.of(second, first);
		}
	}

	public MultiSlotAutomarkSetting<TwoGroupsOfFour> getMarkSettings() {
		return markSettings;
	}

	public MultiSlotAutomarkSetting<PsMarkerGroup> getPsMarkSettings() {
		return psMarkSettings;
	}

	public MultiSlotAutomarkSetting<PsMarkerGroup> getPsMarkSettingsFarGlitch() {
		return psMarkSettingsFar;
	}

	public BooleanSetting getLooperAM() {
		return looperAM;
	}

	public BooleanSetting getPantoAmEnable() {
		return pantoAmEnable;
	}

	public BooleanSetting getSniperAmEnable() {
		return sniperAmEnable;
	}

	public BooleanSetting getPsAmEnable() {
		return psAmEnable;
	}

	public BooleanSetting getMonitorAmEnable() {
		return monitorAmEnable;
	}

	public MultiSlotAutomarkSetting<WrothStyleAssignment> getSniperAmSettings() {
		return sniperAmSettings;
	}
}
