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
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisDeltaAssignment;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisOmegaAssignment;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisSigmaAssignment;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.OmegaFirstSetAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.OmegaSecondSetAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.PantoAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.ProgramLoopAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.PsMarkerAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.SigmaAssignments;
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
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private final ActiveCastRepository casts;
	private final JobSortSetting groupPrioJobSort;
	private final JobSortOverrideSetting p1prio;
	private final JobSortOverrideSetting psPrio;
	private final JobSortOverrideSetting sniperPrio;
	private final JobSortOverrideSetting monitorPrio;
	private final JobSortOverrideSetting sigmaPsPrio;
	private final MultiSlotAutomarkSetting<TwoGroupsOfFour> markSettings;
	private final MultiSlotAutomarkSetting<PsMarkerGroup> psMarkSettings;
	private final MultiSlotAutomarkSetting<PsMarkerGroup> psMarkSettingsFar;
	private final MultiSlotAutomarkSetting<WrothStyleAssignment> sniperAmSettings;
	private final MultiSlotAutomarkSetting<DynamisDeltaAssignment> deltaAmSettings;
	private final MultiSlotAutomarkSetting<DynamisSigmaAssignment> sigmaAmSettings;
	private final MultiSlotAutomarkSetting<DynamisOmegaAssignment> omegaAmSettings;
	private final BooleanSetting looperAM;
	private final BooleanSetting psAmEnable;
	private final BooleanSetting pantoAmEnable;
	private final BooleanSetting sniperAmEnable;
	private final BooleanSetting monitorAmEnable;
	private final BooleanSetting deltaAmEnable;
	private final BooleanSetting sigmaAmEnable;
	private final BooleanSetting omegaAmEnable;
	private final IntSetting sigmaAmDelay;
	private final IntSetting omegaFirstSetDelay;
	private final IntSetting omegaSecondSetDelay;

	public OmegaUltimate(XivState state, StatusEffectRepository buffs, ActiveCastRepository casts, PersistenceProvider pers) {
		this.state = state;
		this.buffs = buffs;
		this.casts = casts;
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
		deltaAmSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.dynamis-am-settings", DynamisDeltaAssignment.class, Map.of(
				DynamisDeltaAssignment.NearWorld, MarkerSign.IGNORE1,
				DynamisDeltaAssignment.DistantWorld, MarkerSign.IGNORE2
		));
		sigmaAmSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.sigma-am-settings", DynamisSigmaAssignment.class, Map.of(
				DynamisSigmaAssignment.NearWorld, MarkerSign.IGNORE1,
				DynamisSigmaAssignment.DistantWorld, MarkerSign.IGNORE2,
				DynamisSigmaAssignment.OneStack1, MarkerSign.ATTACK1,
				DynamisSigmaAssignment.OneStack2, MarkerSign.ATTACK2,
				DynamisSigmaAssignment.OneStack3, MarkerSign.ATTACK3,
				DynamisSigmaAssignment.OneStack4, MarkerSign.BIND1,
				DynamisSigmaAssignment.Remaining1, MarkerSign.BIND2,
				DynamisSigmaAssignment.Remaining2, MarkerSign.BIND3
		));
		omegaAmSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.omega-am-settings", DynamisOmegaAssignment.class, Map.of(
				DynamisOmegaAssignment.NearWorld, MarkerSign.IGNORE1,
				DynamisOmegaAssignment.DistantWorld, MarkerSign.IGNORE2,
				DynamisOmegaAssignment.Baiter1, MarkerSign.ATTACK1,
				DynamisOmegaAssignment.Baiter2, MarkerSign.ATTACK2,
				DynamisOmegaAssignment.Baiter3, MarkerSign.ATTACK3,
				DynamisOmegaAssignment.Baiter4, MarkerSign.ATTACK4,
				DynamisOmegaAssignment.Remaining1, MarkerSign.BIND1,
				DynamisOmegaAssignment.Remaining2, MarkerSign.BIND2
		));
		looperAM = new BooleanSetting(pers, settingKeyBase + "looper-am.enabled", false);
		pantoAmEnable = new BooleanSetting(pers, settingKeyBase + "panto-am.enabled", false);
		sniperAmEnable = new BooleanSetting(pers, settingKeyBase + "sniper-am.enabled", false);
		psAmEnable = new BooleanSetting(pers, settingKeyBase + "ps-marker-am.enabled", false);
		monitorAmEnable = new BooleanSetting(pers, settingKeyBase + "monitor-am.enabled", false);
		deltaAmEnable = new BooleanSetting(pers, settingKeyBase + "delta-am.enabled", false);
		sigmaAmEnable = new BooleanSetting(pers, settingKeyBase + "sigma-am.enabled", false);
		omegaAmEnable = new BooleanSetting(pers, settingKeyBase + "omega-am.enabled", false);
		p1prio = new JobSortOverrideSetting(pers, settingKeyBase + "p1-prio-override", state, groupPrioJobSort);
		psPrio = new JobSortOverrideSetting(pers, settingKeyBase + "ps-prio-override", state, groupPrioJobSort);
		sniperPrio = new JobSortOverrideSetting(pers, settingKeyBase + "sniper-prio-override", state, groupPrioJobSort);
		monitorPrio = new JobSortOverrideSetting(pers, settingKeyBase + "monitor-prio-override", state, groupPrioJobSort);
		sigmaPsPrio = new JobSortOverrideSetting(pers, settingKeyBase + "sigma-ps-prio-override", state, groupPrioJobSort);
		sigmaAmDelay = new IntSetting(pers, settingKeyBase + "sigma-am-delay-seconds", 0, 0, 50);
		omegaFirstSetDelay = new IntSetting(pers, settingKeyBase + "omega-am-1-delay-seconds", 1, 0, 28);
		omegaSecondSetDelay = new IntSetting(pers, settingKeyBase + "omega-am-2-delay-seconds", 0, 0, 20);
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
			v.sort(p1prio.getComparator());
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
				s.setParam("buddy", buddy);
				switch (num) {
					case FIRST -> s.updateCall(firstInLineLoop);
					case SECOND -> s.updateCall(secondInLineLoop);
					case THIRD -> s.updateCall(thirdInLineLoop);
					case FOURTH -> s.updateCall(fourthInLineLoop);
					case UNKNOWN -> {
						log.error("Unknown number!");
						return;
					}
				}
				log.info("Loop start: player has {}, buddy {}", num, buddy.getName());
				s.waitMs(3000);
				if (num == NumberInLine.THIRD) {
					s.updateCall(thirdInLineTether);
				}
				else {
					s.waitEvent(TetherEvent.class, te -> true);
					if (num == NumberInLine.FIRST) {
						s.updateCall(firstInLineTower, looperDebuff);
					}
					else {
						s.call(firstNotYou);
					}
				}

				//First tower goes off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("First tower done");
				if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTower, looperDebuff);
				}
				else if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTether);
				}
				else {
					s.call(secondNotYou);
				}

				//Second tower goes off
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("Second tower done");
				if (num == NumberInLine.THIRD) {
					s.updateCall(thirdInLineTower, looperDebuff);
				}
				else if (num == NumberInLine.FIRST) {
					s.updateCall(firstInLineTether);
				}
				else {
					s.call(thirdNotYou);
				}

				//Third tower goes off
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				log.info("Third tower done");
				if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTower, looperDebuff);
				}
				else if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTether);
				}
				else {
					s.call(fourthNotYou);
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
				s.setParam("missile", guidedMissile);
				s.setParam("cannon", waveCannon);
				s.setParam("buddy", buddy);
				switch (number) {
					case FIRST -> s.updateCall(pantoFirstInLine, myLineBuff);
					case SECOND -> s.updateCall(pantoSecondInLine, myLineBuff);
					case THIRD -> s.updateCall(pantoThirdInLine, myLineBuff);
					case FOURTH -> s.updateCall(pantoFourthInLine, myLineBuff);
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
							case SECOND -> s.updateCall(pantoSecondInLineOut, last);
							case THIRD -> s.updateCall(pantoThirdInLineOut, last);
							case FOURTH -> s.updateCall(pantoFourthInLineOut, last);
						}
					}
					else if (number.lineNumber + 1 == i) {
						switch (number) {
							case FIRST -> s.updateCall(pantoFirstGoBackIn, last);
							case SECOND -> s.updateCall(pantoSecondGoBackIn, last);
							case THIRD -> s.updateCall(pantoThirdGoBackIn, last);
						}
					}
					else {
						switch (i) {
							case 2 -> s.call(pantoSecondNotYou);
							case 3 -> s.call(pantoThirdNotYou);
							case 4 -> s.call(pantoFourthNotYou);
						}

					}
					last = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0E) && aue.isFirstTarget());
					s.waitMs(100);
				}
				s.waitMs(1500);
				if (getState().playerJobMatches(Job::isTank)) {
					s.updateCall(pantoCleave1asTank, last);
				}
				else {
					s.updateCall(pantoCleave1asNonTank, last);
					List<HeadMarkerEvent> hme = s.waitEventsQuickSuccession(3, HeadMarkerEvent.class, he -> true, Duration.ofMillis(250));
					last = hme.get(0);
					@Nullable HeadMarkerEvent myHm = hme.stream().filter(h -> h.getTarget().isThePlayer()).findFirst().orElse(null);
					if (myHm != null) {
						s.updateCall(pantoCleave1withMarker, myHm);
					}
					else {
						s.updateCall(pantoCleave1noMarker, last);
					}
					last = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B11) && aue.isFirstTarget());
					if (myHm == null) {
						s.updateCall(pantoCleave2hadNoMarker, last);
					}
					else {
						s.updateCall(pantoCleave2hadMarker, last);
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
									.sorted(getPsPrio().getComparator())
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
				s.updateCall(checkMfPattern, e1);
				PsMarkerAssignments assignments = s.waitEvent(PsMarkerAssignments.class);

				PsMarkerGroup myAssignment = assignments.forPlayer(getState().getPlayer());
				XivCombatant buddy = assignments.getPlayerForAssignment(myAssignment.getCounterpart());

				int group;
				if (buddy == null) {
					group = 0;
				}
				else {
					group = getPsPrio().getComparator().compare(getState().getPlayer(), (XivPlayerCharacter) buddy) > 0 ? 2 : 1;
				}

				// TODO: get all the ability IDs instead of using the buff
//				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B25, 0x7B2D));
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x68C));


				BuffApplied status = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD63, 0xD64));
				boolean mid = assignments.isMid();
				s.setParam("tetherBuddy", buddy);
				s.setParam("mid", mid);
				s.setParam("group", group);
				s.updateCall((switch (myAssignment) {
					case GROUP1_CIRCLE, GROUP2_CIRCLE -> mid ? midGlitchO : remoteGlitchO;
					case GROUP1_TRIANGLE, GROUP2_TRIANGLE -> mid ? midGlitchT : remoteGlitchT;
					case GROUP1_SQUARE, GROUP2_SQUARE -> mid ? midGlitchS : remoteGlitchS;
					case GROUP1_X, GROUP2_X -> mid ? midGlitchX : remoteGlitchX;
				}), status);

				AbilityCastStart eyeLaser = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B21));
				s.updateCall(eyeLaserStart, eyeLaser);
				AbilityUsedEvent eyeLaserUsed = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B21));
				s.updateCall(eyeLaserDone, eyeLaserUsed);

				List<HeadMarkerEvent> stackMarkers = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 77);
				List<XivPlayerCharacter> stackPlayers = stackMarkers.stream()
						.map(HeadMarkerEvent::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getPsPrio().getComparator())
						.toList();
				s.setParam("stackPlayers", stackPlayers);
				XivCombatant male = getState().npcById(15713);
				ArenaSector unsafeSpot = pos.forCombatant(male);
				s.setParam("unsafe", unsafeSpot);
				s.updateCall((mid ? glitchStacksMid : glitchStacksFar), stackMarkers.get(0));
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
								s.setParam("swapper", swapper);
								s.setParam("swapee", swapee);
								s.setParam("mid", mid);
								s.updateCall(furthestFromEyeSwap, stackMarkers.get(0));
							});
				}
				else {
					s.updateCall(furthestFromEyeNoSwap, stackMarkers.get(0));
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
						s.updateCall(partySynergyBothIn);
					}
					else if (inF && !inM) {
						s.updateCall(partySynergyFinMout);
					}
					else if (!inF && inM) {
						s.updateCall(partySynergyFoutMin);
					}
					else {
						s.updateCall(partySynergyBothOut);
					}
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitlessSynergySq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B38),
			(e1, s) -> {
				log.info("Limitless Synergy Start");
				s.updateCall(limitlessSynergy, e1);
				TetherEvent tether = s.waitEvent(TetherEvent.class, t -> t.tetherIdMatches(0x54));
				if (getState().playerJobMatches(Job::isTank)) {
					s.updateCall(limitlessSynergyGrabTether, tether);
				}
				else {
					s.updateCall(limitlessSynergyGiveTether, tether);
				}
				List<BuffApplied> flares = s.waitEvents(3, BuffApplied.class, buff -> buff.buffIdMatches(0x232));
				BuffApplied myBuff = flares.stream().filter(b -> b.getTarget().isThePlayer()).findAny().orElse(null);
				if (myBuff != null) {
					s.updateCall(limitlessSynergyFlare, myBuff);
				}
				else {
					s.updateCall(limitlessSynergyNoFlare, flares.get(0));
					List<AbilityUsedEvent> peopleThatGotHit = s.collectAoeHits(aue -> aue.abilityIdMatches(0x7B28));
					AbilityUsedEvent firstEvent = peopleThatGotHit.get(0);
					if (firstEvent.getTarget().isThePlayer()) {
						s.updateCall(limitlessSynergyDontStack, firstEvent);
					}
					else {
						peopleThatGotHit.stream()
								.filter(item -> item.getTarget().isThePlayer())
								.findAny()
								.ifPresentOrElse(item -> s.updateCall(limitlessSynergyDontStackMistake, item),
										() -> s.updateCall(limitlessSynergyStack, firstEvent));
					}
				}
			});

	@SuppressWarnings("SuspiciousMethodCalls")
	@AutoFeed
	private final SequentialTrigger<BaseEvent> sniperCannonSq = SqtTemplates.sq(30_000,
			BuffApplied.class, ba -> ba.buffIdMatches(0xD61),
			(e1, s) -> {
				if (getSniperAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
				}
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
						.sorted(getSniperPrio().getComparator())
						.toList();
				List<XivPlayerCharacter> hpSniperPlayers = hpSniper.stream()
						.map(BuffApplied::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getSniperPrio().getComparator())
						.toList();
				List<XivPlayerCharacter> nothingPlayers = nothing.stream()
						.sorted(getSniperPrio().getComparator())
						.toList();
				BuffApplied playerBuff = playerBuffM.getValue();
				s.setParam("snipers", sniperPlayers);
				s.setParam("hpSnipers", hpSniperPlayers);
				s.setParam("nothings", nothingPlayers);
				if (playerBuff == null) {
					s.updateCall(noSniperCannonCall, e1);
				}
				else {
					if (playerBuff.buffIdMatches(0xD61)) {
						s.updateCall(sniperCannonCall, playerBuff);
					}
					else {
						s.updateCall(highPoweredSniperCannonCall, playerBuff);
					}
				}
				if (getSniperAmEnable().get()) {
					s.waitMs(300);
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
				s.updateCall(waveRepeaterMoveIn1, ring1);
				AbilityUsedEvent ring2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B51) && aue.isFirstTarget());
				s.updateCall(waveRepeaterMoveIn2, ring2);
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

	private final ModifiableCallout<BuffApplied> hw1a_defaBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Defa", "Defa in Blue").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Defa", "Pass Rot then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_defaBlueFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Defa, Final Cycle", "Defa in Blue").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaBlueFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Defa, Final Cycle", "Spread for Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_stackBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Stack", "Stack in Blue").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackBlue = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Stack", "Pass Rot then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_stackBlueFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Blue Rot + Stack, Final Cycle", "Stack in Blue").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackBlueFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Blue Rot + Stack, Final Cycle", "Spread for Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_defaRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Defa", "Defa in Red").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Defa", "Pass Rot then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_defaRedFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Defa, Final Cycle", "Defa in Red").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_defaRedFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Defa, Final Cycle", "Spread for Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_stackRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Stack", "Stack in Red").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackRed = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Stack", "Pass Rot then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_stackRedFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Red Rot + Stack, Final Cycle", "Red Rot, Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_stackRedFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Red Rot + Stack, Final Cycle", "Spread for Rot").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_shortTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Short Tether", "Get Defa from {blueDefa ? \"Blue\" : \"Red\"}").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_shortTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Short Tether", "Get Rot, Shrink Tether").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_shortTetherFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Short Tether, Final Cycle", "Stack between {blueDefa ? \"Red\" : \"Blue\"}").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_shortTetherFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Short Tether, Final Cycle", "Watch Rots").autoIcon();

	private final ModifiableCallout<BuffApplied> hw1a_longTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Long Tether", "Stack between {blueDefa ? \"Red\" : \"Blue\"}").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_longTether = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Long Tether", "Get Rot, Stretch Tether").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1a_longTetherFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech First Part: Long Tether, Final Cycle", "Stack between {blueDefa ? \"Red\" : \"Blue\"}").autoIcon();
	private final ModifiableCallout<BuffApplied> hw1b_longTetherFinal = ModifiableCallout.<BuffApplied>durationBasedCall("Hello World Mech Second Part: Long Tether, Final Cycle", "Stretch Tether, Watch Rots").autoIcon();


	@AutoFeed
	private final SequentialTrigger<BaseEvent> helloWorldSq = SqtTemplates.sq(120_000,
			AbilityCastStart.class, a -> a.abilityIdMatches(0x7B55),
			(e1, s) -> {
				log.info("Hello World: Start");
				s.updateCall(hwCastBar, e1);
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
					s.call(hw0_defaOnBlue);
				}
				else {
					s.call(hw0_defaOnRed);
				}
				log.info("Hello World Checkpoint 1");
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(redRot));
				for (int i = 1; i <= 4; i++) {
					boolean last = i == 4;
					s.setParam("blueDefa", defaIsOnBlue);
					s.setParam("i", i);
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
							s.updateCall((last ? hw1a_stackRedFinal : hw1a_stackRed), stackBuff);
							s.waitBuffRemoved(getBuffs(), stackBuff);
							// If the player did not have one, but we have the stack for some reason, find a different one
							if (redRotBuff == null) {
								redRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall((last ? hw1b_stackRedFinal : hw1b_stackRed), redRotBuff);
						}
						else {
							s.updateCall((last ? hw1a_stackBlueFinal : hw1a_stackBlue), stackBuff);
							s.waitBuffRemoved(getBuffs(), stackBuff);
							if (blueRotBuff == null) {
								blueRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall((last ? hw1b_stackBlueFinal : hw1b_stackBlue), blueRotBuff);
						}
					}
					else if (defaBuff != null) {
						if (defaIsOnBlue) {
							s.updateCall((last ? hw1a_defaBlueFinal : hw1a_defaBlue), defaBuff);
							s.waitBuffRemoved(getBuffs(), defaBuff);
							if (blueRotBuff == null) {
								blueRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall((last ? hw1b_defaBlueFinal : hw1b_defaBlue), blueRotBuff);
						}
						else {
							s.updateCall((last ? hw1a_defaRedFinal : hw1a_defaRed), defaBuff);
							s.waitBuffRemoved(getBuffs(), defaBuff);
							if (redRotBuff == null) {
								redRotBuff = getBuffs().findBuffById(redRot);
							}
							s.updateCall((last ? hw1b_defaRedFinal : hw1b_defaRed), redRotBuff);
						}
					}
					else if (shortTetherBuff != null && shortTetherBuff.getEstimatedRemainingDuration().toMillis() < 25_000) {
						// TODO: should all of them work like this, or just rely on the fact that we pass i in as a param?
						// Leaning towards splitting them
						s.updateCall((last ? hw1a_shortTetherFinal : hw1a_shortTether), shortTetherBuff);
						BuffApplied shortRegBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(shortReg));
						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defaReal));
						// TODO: tether follow-up calls should cancel when they are satisfied
						s.updateCall((last ? hw1b_shortTetherFinal : hw1b_shortTether), shortRegBuff);
					}
					else if (longTetherBuff != null && longTetherBuff.getEstimatedRemainingDuration().toMillis() < 25_000) {
						s.updateCall((last ? hw1a_longTetherFinal : hw1a_longTether), longTetherBuff);
						BuffApplied longRegBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(longReg));
						s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defaReal));
						s.updateCall((last ? hw1b_longTetherFinal : hw1b_longTether), longRegBuff);
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
						// D7C is right monitor, D7D is left monitor
						.filter(ba -> ba.buffIdMatches(0xD7C, 0xD7D))
						.toList();

				List<XivPlayerCharacter> monitorPlayers = buffs.stream()
						.map(BuffApplied::getTarget)
						.map(XivPlayerCharacter.class::cast)
						.sorted(getMonitorPrio().getComparator())
						.toList();

				List<XivPlayerCharacter> nonMonitorPlayers = new ArrayList<>(getState().getPartyList());
				nonMonitorPlayers.removeAll(monitorPlayers);
				nonMonitorPlayers.sort(getMonitorPrio().getComparator());
				ArenaSector bossMonitor = e1.abilityIdMatches(0x7B6B) ? ArenaSector.EAST : ArenaSector.WEST;

				s.setParam("monitorPlayers", monitorPlayers);
				s.setParam("nonMonitorPlayers", nonMonitorPlayers);
				s.setParam("bossMonitor", bossMonitor);

				buffs.stream()
						.filter(ba -> ba.getTarget().isThePlayer())
						.findFirst()
						.ifPresentOrElse(
								ba -> s.updateCall(monitorOnYou, ba),
								() -> s.updateCall(noMonitorOnYou, buffs.get(0)));
				// TODO: proper AM settings
				if (getMonitorAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
					s.waitMs(1000);
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

	private final ModifiableCallout<AbilityUsedEvent> waveCannonStacks = new ModifiableCallout<AbilityUsedEvent>("Wave Cannon 1: Stacks", "Stacks on {stacks}")
			.extendedDescription("""
					This callout calls the stacks, but the {stacks} variable is also available for the 'stacks' callouts below.
					It is sorted by the Y position of each character (North to South), so for the "southernmost stack swaps"
					strategy, you can use some logic in the callout to call a swap/no swap.
					""");
	private final ModifiableCallout<AbilityCastStart> waveCannon1Start = ModifiableCallout.durationBasedCall("Wave Cannon 1: Start", "Spread");
	private final ModifiableCallout<AbilityCastStart> waveCannon1Stacks = ModifiableCallout.durationBasedCall("Wave Cannon 1: Stacks", "Stacks");
	private final ModifiableCallout<AbilityUsedEvent> waveCannon2Start = new ModifiableCallout<>("Wave Cannon 2: Start", "Spread Outside");
	private final ModifiableCallout<AbilityCastStart> waveCannon2Stacks = ModifiableCallout.durationBasedCall("Wave Cannon 2: Stacks", "In Now then Stacks");
	private final ModifiableCallout<AbilityUsedEvent> waveCannon3Start = new ModifiableCallout<>("Wave Cannon 3: Start", "Spread Outside");
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

	private List<XivPlayerCharacter> p4wcStacks = Collections.emptyList();
	@AutoFeed
	private final SequentialTrigger<BaseEvent> p4waveCannonStackCollector = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B81),
			(e1, s) -> {
				for (int i = 0; i < 3; i++) {
					List<AbilityUsedEvent> events = s.waitEventsQuickSuccession(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x5779), Duration.ofMillis(100));
					p4wcStacks = events
							.stream()
							.map(AbilityUsedEvent::getTarget)
							.map(XivPlayerCharacter.class::cast)
							.sorted(Comparator.comparing(xpc -> xpc.getPos().y()))
							.toList();
					s.setParam("stacks", p4wcStacks);
					s.updateCall(waveCannonStacks, events.get(0));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p4waveCannonSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B81),
			(e1, s) -> {
				/*
					Summary of casts and such:
					7B81: initial cast
					7B80: fake something
					7B7E: protean damage
					7B7F: stack damage

					5779: stack markers
				 */
				// First set
				{
					s.updateCall(waveCannon1Start.getModified(e1, Map.of("stacks", p4wcStacks)));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					// This cast starts about half a second before the wave cannons go off, so wait a bit
					s.waitMs(300);
					s.updateCall(waveCannon1Stacks.getModified(stackCast, Map.of("stacks", p4wcStacks)));
				}
				// Second set
				{
					// TODO: these don't work - there is no pre-cast for these
					AbilityUsedEvent secondWaveCannonStart = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B7F) && aue.isFirstTarget());
					s.updateCall(waveCannon2Start.getModified(secondWaveCannonStart, Map.of("stacks", p4wcStacks)));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					s.updateCall(waveCannon2Stacks.getModified(stackCast, Map.of("stacks", p4wcStacks)));
				}
				// Third set
				{
					AbilityUsedEvent thirdWaveCannonStart = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B7F) && aue.isFirstTarget());
					s.updateCall(waveCannon3Start.getModified(thirdWaveCannonStart, Map.of("stacks", p4wcStacks)));
					AbilityCastStart stackCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B80));
					s.updateCall(waveCannon3Stacks.getModified(stackCast, Map.of("stacks", p4wcStacks)));
					AbilityUsedEvent center = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B4F));
					s.updateCall(waveCannon3MoveInEarly.getModified(center, Map.of("stacks", p4wcStacks)));
					AbilityUsedEvent secondRing = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B50));
					s.updateCall(waveCannon3MoveInLate.getModified(secondRing, Map.of("stacks", p4wcStacks)));
				}
			});

	@NpcCastCallout(0x7B7B)
	private final ModifiableCallout<AbilityCastStart> blueScreen = ModifiableCallout.durationBasedCall("Blue Screen", "Heavy Raidwide");


	@NpcCastCallout(0x81AC)
	private final ModifiableCallout<AbilityCastStart> solarRay = ModifiableCallout.durationBasedCall("Solar Ray", "Buster on {event.target}");

	private final ModifiableCallout<AbilityCastStart> runDynamisDelta = ModifiableCallout.durationBasedCall("Run Dynamis Delta", "Raidwide");
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteNear = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote + Near", "Near World, Tethered to {buddy}")
			.statusIcon(0xD72);
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteDistant = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote + Distant", "Distant World, Tethered to {buddy}")
			.statusIcon(0xD73);
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteNearBuddy = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote + Buddy Has Near", "Remote with {buddy}, they have Near")
			.statusIcon(0xDB0);
	private final ModifiableCallout<TetherEvent> runDynamisDeltaRemoteDistantBuddy = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Remote + Buddy Has Distant", "Remote with {buddy}, they have Distant")
			.statusIcon(0xDB0);
	private final ModifiableCallout<TetherEvent> runDynamisDeltaLocal = new ModifiableCallout<TetherEvent>("Run Dynamis Delta: Local", "Local with {buddy}")
			.statusIcon(0xD70);
	private final ModifiableCallout<BuffApplied> runDynamisDeltaBaitSpinnerThenMonitor = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: After Fist Snapshot, With Monitor", "Bait then Monitor").autoIcon();
	private final ModifiableCallout<AbilityUsedEvent> runDynamisDeltaBaitSpinnerBlue = new ModifiableCallout<>("Run Dynamis Delta: After Fist Snapshot, As Blue Tether", "Bait");
	private final ModifiableCallout<AbilityUsedEvent> runDynamisDeltaBaitSpinner = new ModifiableCallout<>("Run Dynamis Delta: After Fist Snapshot", "Bait");
	// TODO: these midpoint calls should also check whether you have tether
	// TODO: same person can get BD and monitor, calls need to account for that
	private final ModifiableCallout<BuffApplied> runDynamisDeltaAfterBaitNoMonitorBlue = new ModifiableCallout<>("Run Dynamis Delta: Spinner Bait Done, No Monitor, As Blue Tether", "Move In, No Monitor");
	private final ModifiableCallout<BuffApplied> runDynamisDeltaAfterBaitNoMonitor = new ModifiableCallout<>("Run Dynamis Delta: Spinner Bait Done, No Monitor", "Get Hit by Monitor");
	private final ModifiableCallout<BuffApplied> runDynamisDeltaAfterBaitWithMonitor = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: Spinner Bait Done, With Monitor", "{rightMonitor ? \"Right\" : \"Left\"} Monitor on You")
			.autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisDeltaAfterBaitWithMonitorAndBD = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: Spinner Bait Done, With Monitor and Beyond Defense", "Don't Stack - {rightMonitor ? \"Right\" : \"Left\"} Monitor on You")
			.autoIcon();
	private final ModifiableCallout<AbilityUsedEvent> runDynamisDeltaHitByBeyondDefense = new ModifiableCallout<>("Run Dynamis Delta: Targeted by Beyond Defense", "Don't Stack");
	private final ModifiableCallout<AbilityCastStart> runDynamisDeltaFinalNothing = new ModifiableCallout<>("Run Dynamis Delta: Final Baits, Nothing", "Nothing");
	private final ModifiableCallout<AbilityCastStart> runDynamisDeltaFinalNothingWasBlue = new ModifiableCallout<>("Run Dynamis Delta: Final Baits, Nothing, Was Blue", "Nothing");
	private final ModifiableCallout<BuffApplied> runDynamisDeltaFinalNear = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: Final Baits, Near", "Near World").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisDeltaFinalDistant = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: Final Baits, Dist", "Distant World").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisDeltaFinalTether = new ModifiableCallout<BuffApplied>("Run Dynamis Delta: Final Baits, Tether", "Tether").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> runDynamisDeltaSq = SqtTemplates.sq(120_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B88),
			(e1, s) -> {
				log.info("Dynamis Delta: Start");
				s.updateCall(runDynamisDelta, e1);
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(200, 201), Duration.ofMillis(300));
				log.info("Dynamic Delta: Tethers: {}", tethers);
				s.waitMs(100);
				BuffApplied helloNearWorld = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD72));
				BuffApplied helloDistantWorld = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD73));
				TetherEvent myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst().orElseThrow(() -> new RuntimeException("Couldn't find player's tether!"));
				s.setParam("nearWorld", helloNearWorld);
				s.setParam("distWorld", helloDistantWorld);
				s.setParam("buddy", myTether.getTargetMatching(cbt -> !cbt.isThePlayer()));
				boolean remote = myTether.tetherIdMatches(0xC9);
				if (remote) {
					boolean withNear = myTether.eitherTargetMatches(helloNearWorld.getTarget());
					if (withNear) {
						boolean iHaveNear = helloNearWorld.getTarget().isThePlayer();
						s.updateCall((iHaveNear ? runDynamisDeltaRemoteNear : runDynamisDeltaRemoteNearBuddy), myTether);
					}
					else {
						boolean iHaveDist = helloDistantWorld.getTarget().isThePlayer();
						s.updateCall((iHaveDist ? runDynamisDeltaRemoteDistant : runDynamisDeltaRemoteDistantBuddy), myTether);
					}
				}
				else {
					s.updateCall(runDynamisDeltaLocal, myTether);
				}
				MultiSlotAutoMarkHandler<DynamisDeltaAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getDeltaAmSettings());
				if (getDeltaAmEnable().get()) {
					XivPlayerCharacter nearPlayer = (XivPlayerCharacter) helloNearWorld.getTarget();
					XivPlayerCharacter distPlayer = (XivPlayerCharacter) helloDistantWorld.getTarget();
					handler.process(DynamisDeltaAssignment.NearWorld, nearPlayer);
					handler.process(DynamisDeltaAssignment.DistantWorld, distPlayer);
				}
				AbilityUsedEvent eyeLaser = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B21));
				BuffApplied monitor = s.findOrWaitForBuff(getBuffs(), ba -> ba.buffIdMatches(0xD7C, 0xD7D));
				if (monitor.getTarget().isThePlayer()) {
					s.updateCall(runDynamisDeltaBaitSpinnerThenMonitor, monitor);
				}
				else {
					if (remote) {
						s.updateCall(runDynamisDeltaBaitSpinnerBlue, eyeLaser);
					}
					else {
						s.updateCall(runDynamisDeltaBaitSpinner, eyeLaser);
					}
				}
				// 7B27 is the fake, 7B28 is real
				AbilityUsedEvent beyondDefense = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B28));
				boolean rightMonitor = monitor.buffIdMatches(0xD7C);
				s.setParam("monitor", monitor);
				s.setParam("rightMonitor", rightMonitor);
				s.setParam("beyondDefense", beyondDefense);
				if (monitor.getTarget().isThePlayer()) {
					if (beyondDefense.getTarget().isThePlayer()) {
						s.updateCall(runDynamisDeltaAfterBaitWithMonitorAndBD, monitor);
					}
					else {
						s.updateCall(runDynamisDeltaAfterBaitWithMonitor, monitor);
					}
				}
				else if (beyondDefense.getTarget().isThePlayer()) {
					s.updateCall(runDynamisDeltaHitByBeyondDefense, beyondDefense);
				}
				else {
					if (remote) {
						s.updateCall(runDynamisDeltaAfterBaitNoMonitorBlue, monitor);
					}
					else {
						s.updateCall(runDynamisDeltaAfterBaitNoMonitor, monitor);
					}
				}
				AbilityCastStart swivelCannon = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B94, 0x7B95));
				if (helloNearWorld.getTarget().isThePlayer()) {
					s.updateCall(runDynamisDeltaFinalNear, helloNearWorld);
				}
				else if (helloDistantWorld.getTarget().isThePlayer()) {
					s.updateCall(runDynamisDeltaFinalDistant, helloDistantWorld);
				}
				else {
					BuffApplied tethered = getBuffs().findBuff(ba -> ba.buffIdMatches(0x688) && ba.getTarget().isThePlayer());
					if (tethered != null) {
						s.updateCall(runDynamisDeltaFinalTether, tethered);
					}
					else {
						if (remote) {
							s.updateCall(runDynamisDeltaFinalNothingWasBlue, swivelCannon);
						}
						else {
							s.updateCall(runDynamisDeltaFinalNothing, swivelCannon);
						}
					}
				}
				if (getDeltaAmEnable().get()) {
					s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD72));
					handler.clearAll();
				}
			});

	private final ModifiableCallout<AbilityCastStart> runDynamisSigma = ModifiableCallout.durationBasedCall("Run Dynamis Sigma", "Raidwide");
	private final ModifiableCallout<BuffApplied> p5midGlitchO = ModifiableCallout.<BuffApplied>durationBasedCall("P5 Mid Glitch with O Buddy", "Circle, Close to {tetherBuddy}")
			.extendedDescription("""
					The mid/remote glitch callouts can be modified to call out swap or no swap based on priority.
					There is a variable called 'group' which will be 1 or 2 (or 0 if you had no tether buddy which shouldn't happen).
					For example, you could use the callout '{(group == 2) ? "Right, Furthest" : "Left, Closest"}' to make the call
					switch what it says based on your group.
					""");
	private final ModifiableCallout<BuffApplied> p5midGlitchS = ModifiableCallout.durationBasedCall("P5 Mid Glitch with □ Buddy", "Square, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5midGlitchT = ModifiableCallout.durationBasedCall("P5 Mid Glitch with △ Buddy", "Triangle, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5midGlitchX = ModifiableCallout.durationBasedCall("P5 Mid Glitch with X Buddy", "X, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchO = ModifiableCallout.durationBasedCall("P5 Remote Glitch with O Buddy", "Circle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchS = ModifiableCallout.durationBasedCall("P5 Remote Glitch with □ Buddy", "Square, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchT = ModifiableCallout.durationBasedCall("P5 Remote Glitch with △ Buddy", "Triangle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchX = ModifiableCallout.durationBasedCall("P5 Remote Glitch with X Buddy", "X, far from {tetherBuddy}");

	private final ModifiableCallout<HeadMarkerEvent> sigmaPreyMarkerMarkedBuddy = new ModifiableCallout<>("Run Dynamis Sigma: Prey Marker, Marked Buddy", "Marker on You and Buddy");
	private final ModifiableCallout<HeadMarkerEvent> sigmaPreyMarkerUnmarkedBuddy = new ModifiableCallout<>("Run Dynamis Sigma: Prey Marker, Unmarked Buddy", "Marker on You, Not on Buddy");
	private final ModifiableCallout<HeadMarkerEvent> sigmaNoPreyMarker = new ModifiableCallout<>("Run Dynamis Sigma: Prey Marker", "No Marker");

	private final ModifiableCallout<BuffApplied> p5midGlitchOkb = ModifiableCallout.<BuffApplied>durationBasedCall("P5 Mid Glitch with O Buddy - Knockback", "Circle, Close to {tetherBuddy}")
			.extendedDescription("""
					These callouts work the same as above, but are called when it is time for the knockback.
					""");
	private final ModifiableCallout<BuffApplied> p5midGlitchSkb = ModifiableCallout.durationBasedCall("P5 Mid Glitch with □ Buddy - Knockback", "Knockback - Square, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5midGlitchTkb = ModifiableCallout.durationBasedCall("P5 Mid Glitch with △ Buddy - Knockback", "Knockback - Triangle, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5midGlitchXkb = ModifiableCallout.durationBasedCall("P5 Mid Glitch with X Buddy - Knockback", "Knockback - X, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchOkb = ModifiableCallout.durationBasedCall("P5 Remote Glitch with O Buddy - Knockback", "Knockback - Circle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchSkb = ModifiableCallout.durationBasedCall("P5 Remote Glitch with □ Buddy - Knockback", "Knockback - Square, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchTkb = ModifiableCallout.durationBasedCall("P5 Remote Glitch with △ Buddy - Knockback", "Knockback - Triangle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> p5remoteGlitchXkb = ModifiableCallout.durationBasedCall("P5 Remote Glitch with X Buddy - Knockback", "Knockback - X, far from {tetherBuddy}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> runDynamisSigmaSq = SqtTemplates.sq(120_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8014),
			(e1, s) -> {
				s.updateCall(runDynamisSigma, e1);
				if (getSigmaAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
				}
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
									.sorted(getSigmaPsPrio().getComparator())
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
				PsMarkerAssignments assignments = new PsMarkerAssignments(headmarkers, mid);
				PsMarkerGroup myAssignment = assignments.forPlayer(getState().getPlayer());
				XivCombatant buddy = assignments.getPlayerForAssignment(myAssignment.getCounterpart());
				int group;
				if (buddy == null) {
					group = 0;
				}
				else {
					group = getSigmaPsPrio().getComparator().compare(getState().getPlayer(), (XivPlayerCharacter) buddy) > 0 ? 2 : 1;
				}
				s.setParam("tetherBuddy", buddy);
				s.setParam("mid", mid);
				s.setParam("group", group);
				BuffApplied status = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD63, 0xD64));
				s.updateCall((switch (myAssignment) {
					case GROUP1_CIRCLE, GROUP2_CIRCLE -> mid ? p5midGlitchO : p5remoteGlitchO;
					case GROUP1_TRIANGLE, GROUP2_TRIANGLE -> mid ? p5midGlitchT : p5remoteGlitchT;
					case GROUP1_SQUARE, GROUP2_SQUARE -> mid ? p5midGlitchS : p5remoteGlitchS;
					case GROUP1_X, GROUP2_X -> mid ? p5midGlitchX : p5remoteGlitchX;
				}), status);


				// Near D72, Dist D73, Dynamis 0xD74

				s.waitMs(100);
				try {
					List<XivPlayerCharacter> party = new ArrayList<>(getState().getPartyList());
					XivPlayerCharacter near = (XivPlayerCharacter) getBuffs().findBuffById(0xD72).getTarget();
					XivPlayerCharacter dist = (XivPlayerCharacter) getBuffs().findBuffById(0xD73).getTarget();
					party.remove(near);
					party.remove(dist);
					// Sort is stable, so sort by job prio first then stack count
					party.sort(getSigmaPsPrio().getComparator());
					// One stack first, then zero stack (returns -1)
					party.sort(Comparator.comparing(xpc -> -getBuffs().buffStacksOnTarget(xpc, 0xD74)));

					s.accept(new SigmaAssignments(Map.of(
							DynamisSigmaAssignment.NearWorld, near,
							DynamisSigmaAssignment.DistantWorld, dist,
							DynamisSigmaAssignment.OneStack1, party.get(0),
							DynamisSigmaAssignment.OneStack2, party.get(1),
							DynamisSigmaAssignment.OneStack3, party.get(2),
							DynamisSigmaAssignment.OneStack4, party.get(3),
							DynamisSigmaAssignment.Remaining1, party.get(4),
							DynamisSigmaAssignment.Remaining2, party.get(5)
					)));
				}
				catch (Throwable t) {
					log.error("Error calculating sigma assignments!", t);
				}


				List<HeadMarkerEvent> hm = s.waitEventsQuickSuccession(6, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 221, Duration.ofMillis(100));
				Optional<HeadMarkerEvent> myHm = hm.stream().filter(m -> m.getTarget().isThePlayer()).findFirst();
				boolean buddyHm = hm.stream().anyMatch(m -> m.getTarget().equals(buddy));
				List<XivPlayerCharacter> marked = hm.stream().map(HeadMarkerEvent::getTarget).map(XivPlayerCharacter.class::cast).toList();
				List<XivPlayerCharacter> unmarked = new ArrayList<>(getState().getPartyList());
				// TODO: this should compare which pair is more in or out
				unmarked.removeAll(marked);
				s.setParam("marked", marked);
				s.setParam("unmarked", unmarked);
				unmarked.removeAll(marked);
				myHm.ifPresentOrElse(h -> s.updateCall((buddyHm ? sigmaPreyMarkerMarkedBuddy : sigmaPreyMarkerUnmarkedBuddy), h),
						() -> s.updateCall(sigmaNoPreyMarker, hm.get(0)));
				// Part 2 is handled in the below trigger
				AbilityUsedEvent used = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B74));
				s.updateCall((switch (myAssignment) {
					case GROUP1_CIRCLE, GROUP2_CIRCLE -> mid ? p5midGlitchOkb : p5remoteGlitchOkb;
					case GROUP1_TRIANGLE, GROUP2_TRIANGLE -> mid ? p5midGlitchTkb : p5remoteGlitchTkb;
					case GROUP1_SQUARE, GROUP2_SQUARE -> mid ? p5midGlitchSkb : p5remoteGlitchSkb;
					case GROUP1_X, GROUP2_X -> mid ? p5midGlitchXkb : p5remoteGlitchXkb;
				}), status);
			});

	private final ModifiableCallout<AbilityUsedEvent> sigmaNearWorld = new ModifiableCallout<AbilityUsedEvent>("Run Dynamis Sigma: Near", "Near World").statusIcon(0xD72);
	private final ModifiableCallout<AbilityUsedEvent> sigmaDistWorld = new ModifiableCallout<AbilityUsedEvent>("Run Dynamis Sigma: Distant", "Distant World").statusIcon(0xD73);
	private final ModifiableCallout<AbilityUsedEvent> sigmaOneStack = new ModifiableCallout<AbilityUsedEvent>("Run Dynamis Sigma: One Stack", "One Stack").statusIcon(0xD74, 1);
	private final ModifiableCallout<AbilityUsedEvent> sigmaOneStackLeftover = new ModifiableCallout<AbilityUsedEvent>("Run Dynamis Sigma: One Stack, Low Priority", "One Stack")
			.extendedDescription("""
					This callout is used instead of the one above in the event that you have one stack, but were not chosen by the priority system.
					If you do not intend to use the priority system, you should configure this callout exactly as the one above.""");
	private final ModifiableCallout<AbilityUsedEvent> sigmaNoStacks = new ModifiableCallout<>("Run Dynamis Sigma: No Stacks", "No Stacks");
	private final ModifiableCallout<HeadMarkerEvent> sigmaRotationCW = new ModifiableCallout<>("Run Dynamis Sigma: Clockwise Rotation", "Clockwise");
	private final ModifiableCallout<HeadMarkerEvent> sigmaRotationCCW = new ModifiableCallout<>("Run Dynamis Sigma: CCW Rotation", "Counter-Clockwise");
	private final ModifiableCallout<AbilityCastStart> sigmaSlow = new ModifiableCallout<>("Run Dynamis Sigma: Slow/Out Pattern", "Slow Pattern");
	private final ModifiableCallout<AbilityCastStart> sigmaFast = new ModifiableCallout<>("Run Dynamis Sigma: Fast/In Pattern", "Fast then Wait");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sigmaPart2sq = SqtTemplates.sq(120_000, SigmaAssignments.class, sa -> true,
			(e1, s) -> {
				int stacks = getBuffs().buffStacksOnTarget(getState().getPlayer(), 0xD74);
				// These IDs are both for "storage violation" - but I assume one is the success while one is the failure? Or is it 1 person vs 2?
				AbilityUsedEvent start = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04, 0x7B05));
				DynamisSigmaAssignment assignment = e1.localPlayerAssignment();
				switch (assignment) {
					case NearWorld -> s.updateCall(sigmaNearWorld, start);
					case DistantWorld -> s.updateCall(sigmaDistWorld, start);
					case OneStack1 -> s.updateCall(sigmaOneStack.getModified(start, Map.of("prio", 1)));
					case OneStack2 -> s.updateCall(sigmaOneStack.getModified(start, Map.of("prio", 2)));
					case OneStack3 -> s.updateCall(sigmaOneStack.getModified(start, Map.of("prio", 3)));
					case OneStack4 -> s.updateCall(sigmaOneStack.getModified(start, Map.of("prio", 4)));
					case Remaining1 ->
							s.updateCall((stacks == 1 ? sigmaOneStackLeftover : sigmaNoStacks).getModified(start, Map.of("prio", 1)));
					case Remaining2 ->
							s.updateCall((stacks == 1 ? sigmaOneStackLeftover : sigmaNoStacks).getModified(start, Map.of("prio", 2)));
				}
				// TODO: would be nice to just have "cumulative variables" for callouts within sequential triggers
				HeadMarkerEvent rotation = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().npcIdMatches(15723));
				if (rotation.getMarkerOffset() == 133) {
					s.updateCall(sigmaRotationCW, rotation);
				}
				else if (rotation.getMarkerOffset() == 134) {
					s.updateCall(sigmaRotationCCW, rotation);
				}
				AbilityCastStart laserCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B8F));
				// Okay, this one is also weird. You're supposed to look at Omega-F, but ACT seems to be reporting the
				// name as Omega-M at that point.
				// Better plan seems to be to look for whoever has the Omega-F buff.
				try {
					short wid = getBuffs().findBuffsById(0x68B).stream().findFirst()
							.map(BuffApplied::getTarget)
							.map(cbt -> getState().getLatestCombatantData(cbt))
							.map(XivCombatant::getWeaponId)
							.orElse((short) -1);
					log.info("Sigma WID: {}", wid);
					// WID 4, cast superliminal steel: cleaves sides, safe middle (in/fast pattern)
					// WID 11, cast optimized blizzard III, cleaves line
					switch (wid) {
						case -1 -> log.info("Sigma: No weapon info");// invalid data
						case 4 -> s.updateCall(sigmaFast, laserCast);
						default -> s.updateCall(sigmaSlow, laserCast);// OUT (aka slow pattern)
					}
				}
				catch (Throwable t) {
					log.error("Sigma error", t);
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sigmaAM = SqtTemplates.sq(60_000, SigmaAssignments.class, sa -> true,
			(e1, s) -> {
				if (getSigmaAmEnable().get()) {
					MultiSlotAutoMarkHandler<DynamisSigmaAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getSigmaAmSettings());
					int delay = getSigmaAmDelay().get() * 1_000;
					s.waitMs(delay);
					s.waitMs(1000);
					handler.processMulti(e1.getAssignments());
					s.waitMs(55_000 - delay);
					handler.clearAll();
				}
			});

	private final ArenaPos omegaNS = new ArenaPos(100, 100, 50, 1);
	private final ArenaPos omegaEW = new ArenaPos(100, 100, 1, 50);

	private final ModifiableCallout<AbilityCastStart> runDynamisOmega = ModifiableCallout.<AbilityCastStart>durationBasedCall("Run Dynamis Omega", "Raidwide");

	private final ModifiableCallout<?> runDynamisOmegaDodge = new ModifiableCallout<>("Run Dynamis Omega Safe Spots", "{['Close', 'Mid', 'Far'][dist1]} {dir1} then {['Close', 'Mid', 'Far'][dist2]} {dir2}");
	private final ModifiableCallout<?> runDynamisOmegaDodgeFollowup = new ModifiableCallout<>("Run Dynamis Omega Safe Spots Second Call", "{['Close', 'Mid', 'Far'][dist2]} {dir2}");

	@SuppressWarnings({"SpellCheckingInspection"})
	@AutoFeed
	private final SequentialTrigger<BaseEvent> runDynamisOmegaSafeSpotSq = SqtTemplates.sq(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8015),
			(e1, s) -> {
				log.info("Dynamis Omega Safe Spots: Start");
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8015));
				AbilityCastStart bossCleave = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B9B, 0x7B9C));
				boolean sideSafeFirst = bossCleave.abilityIdMatches(0x7B9B);
				XivCombatant firstM;
				XivCombatant secondM;
				XivCombatant firstF;
				XivCombatant secondF;
				log.info("Dynamis Omega Safe Spots: Finding Data");
				while (true) {
					s.waitThenRefreshCombatants(250);
					List<XivCombatant> ms = getState().npcsById(15721);
					List<XivCombatant> fs = getState().npcsById(15722);
					if (ms.size() != 2 || fs.size() != 2) {
						log.error("Wrong Omega M/F data: {}; {}", ms, fs);
						continue;
					}
					// TODO: seems first set is the higher IDs, but validate this
					// They all move into position at the same time, not sure what else would serve as an indicator.
					// Unless it's finally time to support modelStatus...
					firstM = ms.get(1);
					secondM = ms.get(0);
					firstF = fs.get(1);
					secondF = fs.get(0);
					if (Stream.of(firstM, secondM, firstF, secondF).anyMatch(c -> c.getWeaponId() == -1)) {
						s.waitMs(250);
					}
					else {
						break;
					}
				}
				boolean firstMin = firstM.getWeaponId() == 4;
				boolean secondMin = secondM.getWeaponId() == 4;
				boolean firstFin = firstF.getWeaponId() == 4;
				boolean secondFin = secondF.getWeaponId() == 4;
				/*
					Logic:
					If F out M out: go M FAR
					If F in  M out: go F CLOSE
					If F out M in : go M MID
					If F in  M in : go M CLOSE
				 */
				ArenaSector firstMcard = (sideSafeFirst ? omegaEW : omegaNS).forCombatant(firstM);
				ArenaSector secondMcard = (sideSafeFirst ? omegaNS : omegaEW).forCombatant(secondM);
				// This is how far on the "male" cardinal to go
				// 0 = close, 1 = mid, 2 = far, -1 = close on F side
				int firstDist = firstFin ? (firstMin ? 0 : -1) : (firstMin ? 1 : 2);
				int secondDist = secondFin ? (secondMin ? 0 : -1) : (secondMin ? 1 : 2);
				if (firstDist < 0) {
					firstDist = 0;
					firstMcard = firstMcard.opposite();
				}
				if (secondDist < 0) {
					secondDist = 0;
					secondMcard = secondMcard.opposite();
				}
				s.setParam("dir1", firstMcard);
				s.setParam("dist1", firstDist);
				s.setParam("dir2", secondMcard);
				s.setParam("dist2", secondDist);
				s.updateCall(runDynamisOmegaDodge);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getSource().npcIdMatches(15721, 15722));
				s.updateCall(runDynamisOmegaDodgeFollowup);

			});

	private final ModifiableCallout<BuffApplied> runDynamisOmegaShortNear = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Short Near", "Short Near").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaShortDist = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Short Dist", "Short Distant").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaLongNear = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Long Near", "Long Near").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaLongDist = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Long Dist", "Long Distant").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaNothing = new ModifiableCallout<>("Run Dynamis Omega: Nothing", "Nothing - {dynamisStacks} stacks");
	private final ModifiableCallout<BuffApplied> runDynamisOmegaLongNearP2 = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Long Near Part 2", "Long Near").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaLongDistP2 = ModifiableCallout.<BuffApplied>durationBasedCall("Run Dynamis Omega: Long Dist Part 2", "Long Distant").autoIcon();
	private final ModifiableCallout<BuffApplied> runDynamisOmegaNothingP2 = new ModifiableCallout<>("Run Dynamis Omega: Nothing Part 2", "Nothing - {dynamisStacks} stacks");

	@SuppressWarnings("SuspiciousMethodCalls")
	@AutoFeed
	private final SequentialTrigger<BaseEvent> runDynamisOmegaSq = SqtTemplates.sq(120_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8015),
			(e1, s) -> {
				s.updateCall(runDynamisOmega, e1);
				if (getOmegaAmEnable().get()) {
					s.accept(new ClearAutoMarkRequest());
				}
				log.info("Dynamis Omega: Start");
				/*
					D72 (3442): Near (32s short, 50s long)
					D73 (3443): Dist (32s short, 50s long)
					D74 (3444): Dynamis
					BBC (3004): First in Line
					BBD (3005): Second in Line
					AM Behavior:
					First set:
						Initially, mark short near/dist
						Also mark up to 4 people who have 1 stack and are not first in line
						If you don't have 4, use people with 2 stacks that have neither first nor second in line
					After first set goes off, wait, then redo marks:
						Mark next near/dist, and up to 4 people who have 2 stacks and do not have first/second in line
					Third set:
						Mark people who have 3 stacks and do not have first/second in line
				 */
				s.waitEvents(2, BuffApplied.class, ba -> ba.buffIdMatches(0xBBD));
				// call this T+0
				s.waitMs(100);
				MultiSlotAutoMarkHandler<DynamisOmegaAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getOmegaAmSettings());
				BuffApplied shortNear = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD72) && ba.getInitialDuration().toSeconds() < 40);
				BuffApplied shortDist = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD73) && ba.getInitialDuration().toSeconds() < 40);
				BuffApplied longNear = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD72) && ba.getInitialDuration().toSeconds() > 40);
				BuffApplied longDist = getBuffs().findBuff(ba -> ba.buffIdMatches(0xD73) && ba.getInitialDuration().toSeconds() > 40);
				{
					s.setParam("dynamisStacks", getBuffs().buffStacksOnTarget(getState().getPlayer(), 0xD74));
					if (shortNear.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaShortNear, shortNear);
					}
					else if (shortDist.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaShortDist, shortDist);
					}
					else if (longNear.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaLongNear, longNear);
					}
					else if (longDist.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaLongDist, longDist);
					}
					else {
						s.updateCall(runDynamisOmegaNothing, shortNear);
					}
				}
				{
					List<XivPlayerCharacter> partyList = getState().getPartyList();
					List<XivPlayerCharacter> playersToMark = partyList.stream()
							.sorted(Comparator.comparing(member -> getBuffs().buffStacksOnTarget(member, 3444)))
							.filter(member -> {
								if (getBuffs().isStatusOnTarget(member, 0xBBC)) {
									return false;
								}
								else if (getBuffs().buffStacksOnTarget(member, 0xD74) == 1) {
									return true;
								}
								else if (getBuffs().buffStacksOnTarget(member, 0xD74) == 2 && !getBuffs().isStatusOnTarget(member, 0xBBD)) {
									return true;
								}
								return false;
							})
							.limit(4)
							.toList();
					List<XivPlayerCharacter> leftovers = new ArrayList<>(partyList);
					leftovers.remove(shortNear.getTarget());
					leftovers.remove(shortDist.getTarget());
					leftovers.removeAll(playersToMark);
					s.accept(new OmegaFirstSetAssignments(
							(XivPlayerCharacter) shortNear.getTarget(),
							(XivPlayerCharacter) shortDist.getTarget(),
							playersToMark,
							leftovers
					));
				}
				AbilityCastStart diffuseWaveCannon = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(31643, 31644));
				// T+14s

				// Wait for any of these, since at that point the mechanic is basically locked in
				// 7B89 - near initial
				// 8110 - dist initial
				// 7B8A - near followup
				// 8111 - dist followup
				// 7B6D - Oversampled wave cannon
				// Try to make this resilient even if something goes very wrong
				s.waitEventsUntil(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B8A) && aue.isFirstTarget(),
						AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7E76));
				s.waitMs(500);
				{
					s.setParam("dynamisStacks", getBuffs().buffStacksOnTarget(getState().getPlayer(), 0xD74));
					if (longNear.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaLongNearP2, longNear);
					}
					else if (longDist.getTarget().isThePlayer()) {
						s.updateCall(runDynamisOmegaLongDistP2, longDist);
					}
					else {
						s.updateCall(runDynamisOmegaNothingP2, shortNear);
					}
				}
				{
					if (getOmegaAmEnable().get()) {
						// Second AM set
						// Mark long near
						// Mark long dist
						// Find players to mark for tethers
						List<XivPlayerCharacter> partyList = getState().getPartyList();
						List<XivPlayerCharacter> twoStackPlayers = partyList.stream()
								.filter(member -> getBuffs().buffStacksOnTarget(member, 0xD74) == 2
								                  && !getBuffs().isStatusOnTarget(member, 0xBBC)
								                  && !getBuffs().isStatusOnTarget(member, 0xBBD))
								.limit(4)
								.toList();
						List<XivPlayerCharacter> threeStackPlayers = partyList.stream()
								.filter(member -> getBuffs().buffStacksOnTarget(member, 0xD74) == 3
								                  && !getBuffs().isStatusOnTarget(member, 0xBBC)
								                  && !getBuffs().isStatusOnTarget(member, 0xBBD))
								.limit(2)
								.toList();
						s.accept(new OmegaSecondSetAssignments(
								(XivPlayerCharacter) longNear.getTarget(),
								(XivPlayerCharacter) longDist.getTarget(),
								twoStackPlayers,
								threeStackPlayers
						));
					}
				}
				s.waitMs(15_000);
				handler.clearAll();
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> omegaFirstSetAm = SqtTemplates.sq(60_000, OmegaFirstSetAssignments.class, sa -> true,
			(e1, s) -> {
				if (getOmegaAmEnable().get()) {
					MultiSlotAutoMarkHandler<DynamisOmegaAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getOmegaAmSettings());

					int delay = getOmegaFirstSetDelay().get() * 1_000;
					s.waitMs(delay);

					handler.process(DynamisOmegaAssignment.NearWorld, e1.getNear());
					handler.process(DynamisOmegaAssignment.DistantWorld, e1.getDist());
					handler.processRange(e1.getPlayersToMark(), DynamisOmegaAssignment.Baiter1, DynamisOmegaAssignment.Baiter4);
					handler.processRange(e1.getLeftovers(), DynamisOmegaAssignment.Remaining1, DynamisOmegaAssignment.Remaining2);

					// Initial near/dist hit or wave cannon
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B89, 0x8110, 0x7B6D));
					handler.clearAllFast();
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> omegaSecondSetAm = SqtTemplates.sq(60_000, OmegaSecondSetAssignments.class, sa -> true,
			(e1, s) -> {
				if (getOmegaAmEnable().get()) {
					MultiSlotAutoMarkHandler<DynamisOmegaAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getOmegaAmSettings());

					int delay = getOmegaSecondSetDelay().get() * 1_000;
					s.waitMs(delay);

					handler.process(DynamisOmegaAssignment.NearWorld, e1.getNear());
					handler.process(DynamisOmegaAssignment.DistantWorld, e1.getDist());
					handler.processRange(e1.getPlayersToMark(), DynamisOmegaAssignment.Baiter1, DynamisOmegaAssignment.Baiter4);
					handler.processRange(e1.getLeftovers(), DynamisOmegaAssignment.Remaining1, DynamisOmegaAssignment.Remaining2);

					// Initial near/dist hit or wave cannon
					s.waitMs(15_000 - delay);
					// T+28s-ish
					handler.clearAllFast();
				}
			});

	@NpcCastCallout(0x7B87)
	private final ModifiableCallout<AbilityCastStart> blindFaith = ModifiableCallout.durationBasedCall("Blind Faith", "Raidwide and Knockback");

	@NpcCastCallout(0x7BA1)
	private final ModifiableCallout<AbilityCastStart> cosmoMemoryP6 = ModifiableCallout.durationBasedCall("Cosmo Memory (P6)", "Raidwide");


	@NpcCastCallout(0x7BA6)
	private final ModifiableCallout<AbilityCastStart> cosmoDive = ModifiableCallout.durationBasedCall("Cosmo Dive", "{state.player.job.tank ? \"Close for Buster\" : \"Party Stack\"}");

	// TODO: should be pretty easy to call starting spot
	// TODO: call puddle number like DSR meteor
	// TODO: should coordinate with Cosmo Dive call
	@NpcCastCallout(0x7BAC)
	private final ModifiableCallout<AbilityCastStart> unlimitedWaveCannon = ModifiableCallout.durationBasedCall("Unlimited Wave Cannon", "Exaflares");

	@NpcCastCallout(0x7BA2)
	private final ModifiableCallout<AbilityCastStart> cosmoArrow = ModifiableCallout.<AbilityCastStart>durationBasedCall("Cosmo Arrow", "Exasquares")
			.extendedDescription("""
					This is a simple callout. The callouts below provide more detail, but you can disable those and use this one instead if you prefer a single callout.""")
			.disabledByDefault();

	private final ModifiableCallout<?> exasquareA_1 = new ModifiableCallout<>("Exasquare A1", "Corners First then In")
			.extendedDescription("""
					The 'A' pattern is where the cardinals get hit first.
					The 'B' pattern is where the outer edges get hit first.""");
	private final ModifiableCallout<?> exasquareA_2 = new ModifiableCallout<>("Exasquare A2", "In");
	private final ModifiableCallout<?> exasquareA_3 = new ModifiableCallout<>("Exasquare A3", "Stay In");
	private final ModifiableCallout<?> exasquareA_4 = new ModifiableCallout<>("Exasquare A4", "Corners");
	private final ModifiableCallout<?> exasquareA_5 = new ModifiableCallout<>("Exasquare A5", "Stay Out");
	private final ModifiableCallout<?> exasquareA_6 = new ModifiableCallout<>("Exasquare A6", "Sides");
	private final ModifiableCallout<?> exasquareA_7 = new ModifiableCallout<>("Exasquare A7", "In");

	private final ModifiableCallout<?> exasquareB_1 = new ModifiableCallout<>("Exasquare B1", "In First then Out");
	private final ModifiableCallout<?> exasquareB_2 = new ModifiableCallout<>("Exasquare B2", "Out");
	private final ModifiableCallout<?> exasquareB_3 = new ModifiableCallout<>("Exasquare B3", "Stay Out");
	private final ModifiableCallout<?> exasquareB_4 = new ModifiableCallout<>("Exasquare B4", "Corners");
	private final ModifiableCallout<?> exasquareB_5 = new ModifiableCallout<>("Exasquare B5", "Sides");
	private final ModifiableCallout<?> exasquareB_6 = new ModifiableCallout<>("Exasquare B6", "In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> exasquareSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7BA2),
			(e1, s) -> {
				// Wait for all casts
				s.waitMs(500);
				// Easy way to differentiate patterns is 2 actors casting (out > in) or 4 (in > out)
				List<CastTracker> casts = getCasts().getAll()
						.stream()
						.filter(ct -> ct.getCast().abilityIdMatches(0x7BA3) && ct.getResult() == CastResult.IN_PROGRESS)
						.toList();

				int count = casts.size();
				if (count == 2) {
					// 2 casts = initial cross = corners > in > stay in > corners > stay out/corners > in/cardinals/sides/whatever > in
					s.updateCall(exasquareA_1);
					for (ModifiableCallout<?> call : List.of(exasquareA_2, exasquareA_3, exasquareA_4, exasquareA_5, exasquareA_6, exasquareA_7)) {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7BA3, 0x7BA4));
						s.waitMs(100);
						s.updateCall(call);
					}
				}
				else if (count == 4) {
					// 4 casts = initial square = in > out > stay out > in > out > in
					s.updateCall(exasquareB_1);
					for (ModifiableCallout<?> call : List.of(exasquareB_2, exasquareB_3, exasquareB_4, exasquareB_5, exasquareB_6)) {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7BA3, 0x7BA4));
						s.waitMs(100);
						s.updateCall(call);
					}
				}
				else {
					// What?
					log.error("Saw {} exasquare casts, don't know what to do.", count);
				}
			});

	private final ModifiableCallout<AbilityCastStart> waveCannonSpread = new ModifiableCallout<>("Wave Cannon Spread (P6)", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> waveCannonStaySpread = new ModifiableCallout<>("Wave Cannon Stay Spread (P6)", "Stay Spread");
	private final ModifiableCallout<AbilityCastStart> waveCannonP6Stack = ModifiableCallout.durationBasedCall("Wave Cannon Stack (P6)", "{state.player.job.tank ? \"Stand in Front\" : \"Stand Behind Tanks\"}");

	// TODO: second instance of this happens *during* second exasquares - should coordinate between them so callouts don't talk over each other
	@AutoFeed
	private final SequentialTrigger<BaseEvent> waveCannonSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7BA9),
			(e1, s) -> {
				s.updateCall(waveCannonSpread, e1);
				AbilityUsedEvent firstHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7BAB));
				s.updateCall(waveCannonStaySpread, firstHit);
				s.waitMs(300);
				AbilityUsedEvent secondHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7BAB));
				s.updateCall(waveCannonP6Stack, e1);
			});

	private final ModifiableCallout<AbilityCastStart> cosmoMeteorStart = ModifiableCallout.durationBasedCall("Cosmo Meteor Cast", "Bait Middle then Out");
	private final ModifiableCallout<AbilityUsedEvent> cosmoMeteorSnap = new ModifiableCallout<>("Cosmo Meteor Snap", "{state.player.job.caster ? \"Spread Outside and LB\" : \"Spread Outside\"}");
	private final ModifiableCallout<?> cosmoMeteorRangedLbReminder = new ModifiableCallout<>("Cosmo Meteor Ranged LB Reminder", "Ranged LB Next").disabledByDefault();
	private final ModifiableCallout<HeadMarkerEvent> cosmoMeteorFlare = new ModifiableCallout<>("Cosmo Meteor Flare", "Flare on You");
	private final ModifiableCallout<?> cosmoMeteorNoFlare = new ModifiableCallout<>("Cosmo Meteor Flare", "Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cosmoMeteorSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7BB0),
			(e1, s) -> {
				s.updateCall(cosmoMeteorStart, e1);
				AbilityUsedEvent snap = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7BB0));
				s.updateCall(cosmoMeteorSnap, snap);
				s.waitMs(3_000);
				s.updateCall(cosmoMeteorRangedLbReminder);
				List<HeadMarkerEvent> hms = s.waitEventsQuickSuccession(3, HeadMarkerEvent.class, hm -> hm.getMarkerOffset() == 323, Duration.ofMillis(100));
				hms.stream()
						.filter(hm -> hm.getTarget().isThePlayer())
						.findFirst()
						.ifPresentOrElse(hm -> s.updateCall(cosmoMeteorFlare, hm),
								() -> s.updateCall(cosmoMeteorNoFlare));
			});

	private final ModifiableCallout<AbilityCastStart> magicNumberStart = ModifiableCallout.durationBasedCall("Magic Number: Cast", "{state.player.job.tank ? \"Tank LB Now\" : \"Raidwide\"}");
	private final ModifiableCallout<BuffApplied> magicNumberDebuff = ModifiableCallout.durationBasedCall("Magic Number: Debuff", "{state.player.job.healer ? \"Healer LB Now\" : \"Wait for Healer LB\"}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> magicNumberSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7BB6),
			(e1, s) -> {
				s.updateCall(magicNumberStart, e1);
				BuffApplied buff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xDCC));
				s.updateCall(magicNumberDebuff, buff);
			});

	@NpcCastCallout(0x7BA0)
	private final ModifiableCallout<AbilityCastStart> p6enrage = ModifiableCallout.durationBasedCall("P6 Enrage", "Enrage");

	public JobSortSetting getGroupPrioJobSort() {
		return groupPrioJobSort;
	}

	public boolean shouldSwap(XivPlayerCharacter first, XivPlayerCharacter second) {
		int result = groupPrioJobSort.getComparator().compare(first, second);
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

	public MultiSlotAutomarkSetting<DynamisDeltaAssignment> getDeltaAmSettings() {
		return deltaAmSettings;
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

	public BooleanSetting getDeltaAmEnable() {
		return deltaAmEnable;
	}

	public BooleanSetting getSigmaAmEnable() {
		return sigmaAmEnable;
	}

	public BooleanSetting getOmegaAmEnable() {
		return omegaAmEnable;
	}

	public JobSortOverrideSetting getMonitorPrio() {
		return monitorPrio;
	}

	public JobSortOverrideSetting getPsPrio() {
		return psPrio;
	}

	public JobSortOverrideSetting getSniperPrio() {
		return sniperPrio;
	}

	public JobSortOverrideSetting getP1prio() {
		return p1prio;
	}

	public JobSortOverrideSetting getSigmaPsPrio() {
		return sigmaPsPrio;
	}

	public MultiSlotAutomarkSetting<DynamisSigmaAssignment> getSigmaAmSettings() {
		return sigmaAmSettings;
	}

	public MultiSlotAutomarkSetting<WrothStyleAssignment> getSniperAmSettings() {
		return sniperAmSettings;
	}

	public MultiSlotAutomarkSetting<DynamisOmegaAssignment> getOmegaAmSettings() {
		return omegaAmSettings;
	}

	public IntSetting getSigmaAmDelay() {
		return sigmaAmDelay;
	}

	public IntSetting getOmegaFirstSetDelay() {
		return omegaFirstSetDelay;
	}

	public IntSetting getOmegaSecondSetDelay() {
		return omegaSecondSetDelay;
	}

	private ActiveCastRepository getCasts() {
		return casts;
	}
}
