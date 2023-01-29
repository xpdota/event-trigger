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
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.PantoAssignments;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.ProgramLoopAssignments;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.groupmodels.PsMarkerGroups;
import gg.xp.xivsupport.models.groupmodels.TwoGroupsOfFour;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
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

@CalloutRepo(name = "TOP Triggers", duty = KnownDuty.OmegaProtocol)
public class OmegaUltimate extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(OmegaUltimate.class);

	// Looper
	private static final Duration looperOffset = Duration.ofMillis(-2650);
	private final ModifiableCallout<BuffApplied> firstInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop First: Start/Tower", "One with {buddy}, Take Tower", looperOffset).statusIcon(0xBBC);
	private final ModifiableCallout<?> firstInLineTether = new ModifiableCallout<>("Loop First: Tether", "Take Tether").statusIcon(0xBBC);
	private final ModifiableCallout<?> firstNotYou = new ModifiableCallout<>("Loop First: Not You", "1");

	private final ModifiableCallout<?> secondInLineLoop = new ModifiableCallout<>("Loop Second: Start", "Two with {buddy}").statusIcon(0xBBD);
	private final ModifiableCallout<BuffApplied> secondInLineTower = ModifiableCallout.<BuffApplied>durationBasedCallWithOffset("Loop Second: Tower", "Take Tower", looperOffset).statusIcon(0xBBD);
	private final ModifiableCallout<?> secondInLineTether = new ModifiableCallout<>("Loop Second: Tether", "Take tether").statusIcon(0xBBD);
	private final ModifiableCallout<?> secondNotYou = new ModifiableCallout<>("Loop Second: Not You", "2");

	private final ModifiableCallout<?> thirdInLineTether = new ModifiableCallout<>("Loop Third: Start/Tether", "Three with {buddy}, Take Tether").statusIcon(0xBBE);
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

	private final ModifiableCallout<AbilityCastStart> checkMfPattern = new ModifiableCallout<>("Check M/F Sword/Shield");

	private final ModifiableCallout<?> partySynergyBothIn = new ModifiableCallout<>("Party Synergy: Both In", "On Male")
			.extendedDescription("Shield and Skates: Stand on Male");
	private final ModifiableCallout<?> partySynergyBothOut = new ModifiableCallout<>("Party Synergy: Both Out", "Out of Both")
			.extendedDescription("Neither Shield nor Skates: Cross + Chariot, stand off to the sides");
	private final ModifiableCallout<?> partySynergyFoutMin = new ModifiableCallout<>("Party Synergy: F Out, M In", "Sides of Male")
			.extendedDescription("Shield, no Skates: Small safe areas to the sides of Male");
	private final ModifiableCallout<?> partySynergyFinMout = new ModifiableCallout<>("Party Synergy: F In, M Out", "On Female")
			.extendedDescription("Skates, no Shield: Stand close to Female");


	private final ModifiableCallout<BuffApplied> midGlitchO = ModifiableCallout.durationBasedCall("Mid Glitch with O Buddy", "Circle, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> midGlitchS = ModifiableCallout.durationBasedCall("Mid Glitch with □ Buddy", "Square, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> midGlitchT = ModifiableCallout.durationBasedCall("Mid Glitch with △ Buddy", "Triangle, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> midGlitchX = ModifiableCallout.durationBasedCall("Mid Glitch with X Buddy", "X, Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchO = ModifiableCallout.durationBasedCall("Remote Glitch with O Buddy", "Circle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchS = ModifiableCallout.durationBasedCall("Remote Glitch with □ Buddy", "Square, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchT = ModifiableCallout.durationBasedCall("Remote Glitch with △ Buddy", "Triangle, far from {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitchX = ModifiableCallout.durationBasedCall("Remote Glitch with X Buddy", "X, far from {tetherBuddy}");

	private final ModifiableCallout<AbilityCastStart> eyeLaserStart = ModifiableCallout.durationBasedCall("Eye Laser Starts Casting", "Eye Laser");
	private final ModifiableCallout<AbilityUsedEvent> eyeLaserDone = new ModifiableCallout<>("Eye Laser Done Casting", "Knockback Stacks");
	private final ModifiableCallout<HeadMarkerEvent> glitchStacks = new ModifiableCallout<>("Glitch Stacks", "Stacks on {stackPlayers[0]} and {stackPlayers[1]}");

	private final ModifiableCallout<AbilityCastStart> limitlessSynergy = new ModifiableCallout<>("Limitless Synergy", "Limitless Synergy");

	private final ModifiableCallout<TetherEvent> limitlessSynergyGrabTether = new ModifiableCallout<>("Limitless Synergy as Tank: Grab Tether", "Grab Tethers");
	private final ModifiableCallout<TetherEvent> limitlessSynergyGiveTether = new ModifiableCallout<>("Limitless Synergy as non-Tank: Give Away Tether", "Give Tethers to Tanks");
	private final ModifiableCallout<BuffApplied> limitlessSynergyFlare = new ModifiableCallout<>("Limitless Synergy: Flare", "Out for Flare");
	private final ModifiableCallout<BuffApplied> limitlessSynergyNoFlare = new ModifiableCallout<>("Limitless Synergy: No Flare", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyStack = new ModifiableCallout<>("Limitless Synergy: Stack", "Stack");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyDontStack = new ModifiableCallout<AbilityUsedEvent>("Limitless Synergy: Don't Stack", "Stack")
			.extendedDescription("This trigger activates if you were chosen for beyond defense, but not if you were clipped by someone else's hit.");
	private final ModifiableCallout<AbilityUsedEvent> limitlessSynergyDontStackMistake = new ModifiableCallout<AbilityUsedEvent>("Limitless Synergy: Don't Stack (Mistake)", "Stack")
			.extendedDescription("This trigger is activated instead of the one above if you were clipped by someone else's Beyond Defense hit.");

	private static final Position center = Position.of2d(100, 100);

//	private final ModifiableCallout<BuffApplied> followUpMidGlitchO = ModifiableCallout.durationBasedCall("Mid Glitch with O Buddy: Followup", "Circle, Close to {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpMidGlitchS = ModifiableCallout.durationBasedCall("Mid Glitch with □ Buddy: Followup", "Square, Close to {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpMidGlitchT = ModifiableCallout.durationBasedCall("Mid Glitch with △ Buddy: Followup", "Triangle, Close to {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpMidGlitchX = ModifiableCallout.durationBasedCall("Mid Glitch with X Buddy: Followup", "X, Close to {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpRemoteGlitchO = ModifiableCallout.durationBasedCall("Remote Glitch with O Buddy: Followup", "Circle, far from {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpRemoteGlitchS = ModifiableCallout.durationBasedCall("Remote Glitch with □ Buddy: Followup", "Square, far from {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpRemoteGlitchT = ModifiableCallout.durationBasedCall("Remote Glitch with △ Buddy: Followup", "Triangle, far from {tetherBuddy}");
//	private final ModifiableCallout<BuffApplied> followUpRemoteGlitchX = ModifiableCallout.durationBasedCall("Remote Glitch with X Buddy: Followup", "X, far from {tetherBuddy}");

	// Panto
//	@PlayerStatusCallout({0xDB3, 0xDB4, 0xDB5, 0xDB6})
//	private final ModifiableCallout<BuffApplied> waveCannonKyrios = new ModifiableCallout<BuffApplied>("Condensed Wave Cannon Kyrios", "Wave Cannon on You").autoIcon();
//	@PlayerStatusCallout({0xD60, 0xDA7, 0xDA8, 0xDA9})
//	private final ModifiableCallout<BuffApplied> guidedMissileKyrios = new ModifiableCallout<BuffApplied>("Guided Missile Kyrios", "Missile on You").autoIcon();

	// Mechanics
	@PlayerStatusCallout(0xDC8)
	private final ModifiableCallout<BuffApplied> cascadingLatentDefect = new ModifiableCallout<BuffApplied>("Cascading Latent Defect", "Pick Up Rot").autoIcon();
	@PlayerStatusCallout(0xDC5)
	private final ModifiableCallout<BuffApplied> criticalOverflowBug = new ModifiableCallout<BuffApplied>("Critical Overflow Bug", "Defamation").autoIcon();
	@PlayerStatusCallout(0xDC4)
	private final ModifiableCallout<BuffApplied> criticalSynchronizationBug = new ModifiableCallout<BuffApplied>("Critical Synchronization Bug", "Stack").autoIcon();
	@PlayerStatusCallout(0xDC6)
	private final ModifiableCallout<BuffApplied> criticalUnderflowBug = new ModifiableCallout<BuffApplied>("Critical Underflow Bug", "Rot on You").autoIcon();

	// Get hit by stuff
	@PlayerStatusCallout(0xDC7)
	private final ModifiableCallout<BuffApplied> latentDefect = new ModifiableCallout<BuffApplied>("Latent Defect", "Get Hit by Defamation").autoIcon();
	@PlayerStatusCallout(0xD6A)
	private final ModifiableCallout<BuffApplied> latentSynchronizationBug = new ModifiableCallout<BuffApplied>("Latent Synchronization Bug", "Stack").autoIcon();

	// Soon
	@PlayerStatusCallout(0xD6D)
	private final ModifiableCallout<BuffApplied> overflowCodeSmell = new ModifiableCallout<BuffApplied>("Overflow Code Smell", "Defamation Soon").autoIcon();
	@PlayerStatusCallout(0xD6C)
	private final ModifiableCallout<BuffApplied> synchronizationCodeSmell = new ModifiableCallout<BuffApplied>("Synchronization Code Smell", "Stack Soon").autoIcon();
	@PlayerStatusCallout(0xD6E)
	private final ModifiableCallout<BuffApplied> underflowCodeSmell = new ModifiableCallout<BuffApplied>("Underflow Code Smell", "Rot Soon").autoIcon();

	// Tethers
	@PlayerStatusCallout({0xD70, 0xDAF})
	private final ModifiableCallout<BuffApplied> localCodeSmell = new ModifiableCallout<BuffApplied>("Local Code Smell", "Close Tether Soon").autoIcon();
	@PlayerStatusCallout(0xDC9)
	private final ModifiableCallout<BuffApplied> localRegression = new ModifiableCallout<BuffApplied>("Local Regression", "Close Tether").autoIcon();
	@PlayerStatusCallout({0xD71, 0xDB0})
	private final ModifiableCallout<BuffApplied> remoteCodeSmell = new ModifiableCallout<BuffApplied>("Remote Code Smell", "Far Tether Soon").autoIcon();
	@PlayerStatusCallout(0xDCA)
	private final ModifiableCallout<BuffApplied> remoteRegression = new ModifiableCallout<BuffApplied>("Remote Regression", "Far Tether").autoIcon();

//	@PlayerStatusCallout(0xD62)
//	private final ModifiableCallout<BuffApplied> highPoweredSniperCannon = new ModifiableCallout<BuffApplied>("High-powered Sniper Cannon", "Super sniper soon").autoIcon();
//	@PlayerStatusCallout(0xD61)
//	private final ModifiableCallout<BuffApplied> sniperCannon = new ModifiableCallout<BuffApplied>("Sniper Cannon", "Sniper Soon").autoIcon();

	@PlayerStatusCallout(0xDAC)
	private final ModifiableCallout<BuffApplied> packetFilterF = new ModifiableCallout<BuffApplied>("Packet Filter F", "Attack M").autoIcon();
	@PlayerStatusCallout(0xDAB)
	private final ModifiableCallout<BuffApplied> packetFilterM = new ModifiableCallout<BuffApplied>("Packet Filter M", "Attack F").autoIcon();

	@NpcCastCallout(0x7B22)
	private final ModifiableCallout<AbilityCastStart> cosmoMemory = new ModifiableCallout<>("Cosmo Memory", "Raidwides");

	private final XivState state;
	private final StatusEffectRepository buffs;
	private final JobSortSetting groupPrioJobSort;
	private final MultiSlotAutomarkSetting<TwoGroupsOfFour> markSettings;
	private final MultiSlotAutomarkSetting<PsMarkerGroups> psMarkSettings;
	private final BooleanSetting looperAM;
	private final BooleanSetting pantoAmEnable;

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
		psMarkSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.ps-am-slot-settings", PsMarkerGroups.class, Map.of(
				PsMarkerGroups.GROUP1_CIRCLE, MarkerSign.ATTACK1,
				PsMarkerGroups.GROUP1_TRIANGLE, MarkerSign.ATTACK2,
				PsMarkerGroups.GROUP1_SQUARE, MarkerSign.ATTACK3,
				PsMarkerGroups.GROUP1_X, MarkerSign.ATTACK4,
				PsMarkerGroups.GROUP2_CIRCLE, MarkerSign.BIND1,
				PsMarkerGroups.GROUP2_TRIANGLE, MarkerSign.BIND2,
				PsMarkerGroups.GROUP2_SQUARE, MarkerSign.BIND3,
				PsMarkerGroups.GROUP2_X, MarkerSign.CROSS
		));
		looperAM = new BooleanSetting(pers, settingKeyBase + "looper-am.enabled", false);
		pantoAmEnable = new BooleanSetting(pers, settingKeyBase + "panto-am.enabled", false);
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

	//returns first player with line debuff
	private XivPlayerCharacter supPlayerFromLine(NumberInLine il) {
		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isTank() || p.getJob().isHealer()).toList();
		int lineId = il.lineId();

		return players.stream()
				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
				.findFirst()
				.orElse(null);
	}

	//returns first player with line debuff
	private XivPlayerCharacter dpsPlayerFromLine(NumberInLine il) {
		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isDps()).toList();
		int lineId = il.lineId();

		return players.stream()
				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
				.findFirst()
				.orElse(null);
	}

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
					case FIRST -> s.updateCall(firstInLineTower.getModified(looperDebuff, params));
					case SECOND -> s.updateCall(secondInLineLoop.getModified(params));
					case THIRD -> s.updateCall(thirdInLineTether.getModified(params));
					case FOURTH -> s.updateCall(fourthInLineLoop.getModified(params));
					case UNKNOWN -> {
						log.error("Unknown number!");
						return;
					}
				}
				log.info("Loop start: player has {}, buddy {}", num, buddy.getName());
				s.waitMs(3000);
				if (num != NumberInLine.FIRST) {
					s.accept(firstNotYou.getModified(params));
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
	private final SequentialTrigger<BaseEvent> midRemoteGlitch = SqtTemplates.sq(50_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B3F),
			(e1, s) -> {
				s.updateCall(checkMfPattern.getModified(e1));
				// TODO: tether IDs - are there multiple?
//				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer) && te.tetherIdMatches(0xDE));
				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant buddy = tether.getTargetMatching(xpc -> !xpc.isThePlayer());
				HeadMarkerEvent myHm = s.waitEvent(HeadMarkerEvent.class, hm -> hm.getTarget().isThePlayer());
				Map<String, Object> params = Map.of("tetherBuddy", buddy);

				// TODO: get all the ability IDs instead of using the buff
//				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B25, 0x7B2D));
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x68C));


				BuffApplied status = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD63, 0xD64));
				boolean mid = status.buffIdMatches(0xD63);
				s.updateCall((switch (myHm.getMarkerOffset()) {
					case 393 -> mid ? midGlitchO : remoteGlitchO;
					case 394 -> mid ? midGlitchT : remoteGlitchT;
					case 395 -> mid ? midGlitchS : remoteGlitchS;
					case 396 -> mid ? midGlitchX : remoteGlitchX;
					default -> throw new IllegalStateException("Unexpected value: " + myHm.getMarkerOffset());
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
				s.updateCall(glitchStacks.getModified(stackMarkers.get(0), params));
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
//
//	@AutoFeed
//	public SequentialTrigger<BaseEvent> pantokratorSqOther = SqtTemplates.sq(50_000, AbilityCastStart.class,
//			acs -> acs.abilityIdMatches(0x7B0B),
//			(e1, s) -> {
//				EventCollector<BuffApplied> sup = new EventCollector<>(ba -> isLineDebuff(ba) && ((XivPlayerCharacter) ba.getTarget()).getJob().isSupport());
//				EventCollector<BuffApplied> dps = new EventCollector<>(ba -> isLineDebuff(ba) && ((XivPlayerCharacter) ba.getTarget()).getJob().isDps());
//				s.collectEvents(8, 30_000, BuffApplied.class, true, List.of(sup, dps));
//
//				List<NumberInLine> supBuffs = sup.getEvents().stream().map(NumberInLine::debuffToLine).sorted(Comparator.naturalOrder()).toList();
//				List<NumberInLine> dpsBuffs = dps.getEvents().stream().map(NumberInLine::debuffToLine).sorted(Comparator.naturalOrder()).toList();
//
//				NumberInLine priorSup = null;
//				for (NumberInLine n : supBuffs) {
//					if (priorSup == n) {
//						s.accept(new SpecificAutoMarkRequest(supPlayerFromLine(n), MarkerSign.BIND2));
//						break;
//					}
//					priorSup = n;
//				}
//
//				NumberInLine priorDps = null;
//				for (NumberInLine n : dpsBuffs) {
//					if (priorDps == n) {
//						s.accept(new SpecificAutoMarkRequest(dpsPlayerFromLine(n), MarkerSign.BIND1));
//						break;
//					}
//					priorDps = n;
//				}
//			});


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

	public MultiSlotAutomarkSetting<PsMarkerGroups> getPsMarkSettings() {
		return psMarkSettings;
	}

	public BooleanSetting getLooperAM() {
		return looperAM;
	}

	public BooleanSetting getPantoAmEnable() {
		return pantoAmEnable;
	}
}
