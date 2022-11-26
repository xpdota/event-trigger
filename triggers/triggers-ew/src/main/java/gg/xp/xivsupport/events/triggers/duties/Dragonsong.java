package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.gui.tables.renderers.RefreshingHpBar;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@CalloutRepo(name = "Dragonsong's Reprise", duty = KnownDuty.Dragonsong)
public class Dragonsong extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(Dragonsong.class);

	// Doorboss
	private final ModifiableCallout<HeadMarkerEvent> p1_firstCleaveMarker = new ModifiableCallout<>("Quad Marker (1st set)", "Marker, First Set");
	private final ModifiableCallout<HeadMarkerEvent> p1_secondCleaveMarker = new ModifiableCallout<>("Quad Marker (2nd set)", "Second Set");
	private final ModifiableCallout<AbilityCastStart> p1_holiestOfHoly = ModifiableCallout.durationBasedCall("Holiest of Holy", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> p1_emptyDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "In");
	private final ModifiableCallout<AbilityCastStart> p1_fullDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Out");
	private final ModifiableCallout<AbilityCastStart> p1_heavensblaze = ModifiableCallout.durationBasedCall("Heavensblaze", "Stack on {event.target}");
	private final ModifiableCallout<AbilityCastStart> p1_holiestHallowing = ModifiableCallout.durationBasedCall("Holiest Hallowing", "Interrupt {event.source}");
	private final ModifiableCallout<BuffApplied> p1_brightwing = ModifiableCallout.durationBasedCall("Brightwing", "Pair Cleaves");

//	private final ModifiableCallout<TetherEvent> p1_genericTether = new ModifiableCallout<>("P1 Generic Tethers", "Tether on you", "Tether on you {event.id}", Collections.emptyList());

	private final ModifiableCallout<BuffApplied> p1_puddleBait = ModifiableCallout.<BuffApplied>durationBasedCall("Puddle (Place)", "Puddle on you").autoIcon();
	private final ModifiableCallout<BuffRemoved> p1_puddleBaitAfter = new ModifiableCallout<BuffRemoved>("Puddle (Move)", "Move").autoIcon();

	private final ModifiableCallout<HeadMarkerEvent> circle = new ModifiableCallout<>("Circle", "Red Circle with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> triangle = new ModifiableCallout<>("Triangle", "Green Triangle with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> square = new ModifiableCallout<>("Square", "Purple Square with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> cross = new ModifiableCallout<>("Cross", "Blue Cross with {partner}");

	// Thordan
	private final ModifiableCallout<AbilityCastStart> thordan_cleaveBait = ModifiableCallout.durationBasedCall("Ascalon's Mercy", "Cleave Bait");
	private final ModifiableCallout<AbilityCastStart> thordan_quaga = ModifiableCallout.durationBasedCall("Ancient Quaga", "Raidwide");

	private final ModifiableCallout<?> nsSafe = new ModifiableCallout<>("Trio 1 N/S Safe", "North/South Safe", "North South Safe");
	private final ModifiableCallout<?> neSwSafe = new ModifiableCallout<>("Trio 1 NE/SW Safe", "Northeast/Southwest Safe", "Northeast Southwest Safe");
	private final ModifiableCallout<?> ewSafe = new ModifiableCallout<>("Trio 1 E/W Safe", "East/West Safe", "East West Safe");
	private final ModifiableCallout<?> seNwSafe = new ModifiableCallout<>("Trio 1 SE/NW Safe", "Southeast/Northwest Safe", "Southeast Northwest Safe");

	private final ModifiableCallout<AbilityCastStart> thordan_heavenlyHeel = ModifiableCallout.durationBasedCall("Heavenly Heel", "Buster on {event.target}, then 3 hits");

	private final ModifiableCallout<?> thordan_trio1_nothing = new ModifiableCallout<>("First Trio: Nothing", "Nothing");
	private final ModifiableCallout<HeadMarkerEvent> thordan_trio1_blueMarker = new ModifiableCallout<>("First Trio: Blue Marker", "Blue Marker");
	private final ModifiableCallout<?> thordan_trio1_tank = new ModifiableCallout<>("First Trio: Tank", "Take Tether");
	private final ModifiableCallout<?> thordan_trio1_wheresThordan = new ModifiableCallout<>("First Trio: Where is Thordan", "Thordan {wheresThordan}");

	private final ModifiableCallout<?> thordan_trio2_swordMark = new ModifiableCallout<>("Second Trio: Swords", "{sword1} and {sword2}");
	private final ModifiableCallout<AbilityCastStart> thordan_trio2_gaze = ModifiableCallout.durationBasedCall("Second Trio: Gaze", "Look away");
	private final ModifiableCallout<?> thordan_trio2_cw = new ModifiableCallout<>("Second Trio: Clockwise", "Clockwise");
	private final ModifiableCallout<?> thordan_trio2_ccw = new ModifiableCallout<>("Second Trio: Counter-clockwise", "Counterclockwise");

	private final ModifiableCallout<?> thordan_trio2_meteorMark = new ModifiableCallout<>("Second Trio: Meteors", "Meteor on you").autoIcon();
	private final ModifiableCallout<?> thordan_trio2_meteorRoleMark = new ModifiableCallout<>("Second Trio: Meteors", "Meteor role");
	private final ModifiableCallout<?> thordan_trio2_nonMeteorRole = new ModifiableCallout<>("Second Trio: Meteors", "Non-meteor role");

	private final ModifiableCallout<?> thordan_trio2_firstTower = new ModifiableCallout<>("Second Trio: Tower 1", "Soak First Tower");
	//	private final ModifiableCallout<?> thordan_trio2_secondTower = new ModifiableCallout<>("Second Trio: Tower 2", "Soak Second Tower");
	private final ModifiableCallout<?> thordan_trio2_kbImmune = new ModifiableCallout<>("Second Trio: Knockback Immune", "Knockback Immune in Tower");
	private final ModifiableCallout<?> thordan_trio2_getKnockedBack = new ModifiableCallout<>("Second Trio: Knockback Immune", "Take knockback into tower");
	private final ModifiableCallout<BaseEvent> meteorDrop = new ModifiableCallout<>("Drop Meteors", "Drop Meteors", "Drop Meteor #{num}", ModifiableCallout.expiresIn(Duration.ofSeconds(11)));

	private final ModifiableCallout<AbilityCastStart> thordan_broadSwingL = ModifiableCallout.durationBasedCall("Broad Swing Left", "Back then Left");
	private final ModifiableCallout<AbilityCastStart> thordan_broadSwingR = ModifiableCallout.durationBasedCall("Broad Swing Right", "Back then Right");

	// Nidhogg
	private final ModifiableCallout<BuffApplied> estinhog_headmark1 = new ModifiableCallout<BuffApplied>("Estinhog: First in Line", "One").autoIcon();
	private final ModifiableCallout<BuffApplied> estinhog_headmark2 = new ModifiableCallout<BuffApplied>("Estinhog: Second in Line", "Two").autoIcon();
	private final ModifiableCallout<BuffApplied> estinhog_headmark3 = new ModifiableCallout<BuffApplied>("Estinhog: Third in Line", "Three").autoIcon();

	private final ModifiableCallout<BuffApplied> estinhog_highJumpOnlyYou = ModifiableCallout.<BuffApplied>durationBasedCall("Estinhog: High Jump", "Middle").autoIcon();
	private final ModifiableCallout<BuffApplied> estinhog_highJumpAll = ModifiableCallout.<BuffApplied>durationBasedCall("Estinhog: High Jump", "Pick Spots").autoIcon();
	private final ModifiableCallout<BuffApplied> estinhog_spineshatter = ModifiableCallout.<BuffApplied>durationBasedCall("Estinhog: Spineshatter", "West and Face In").autoIcon();
	private final ModifiableCallout<BuffApplied> estinhog_elusiveJump = ModifiableCallout.<BuffApplied>durationBasedCall("Estinhog: Elusive Jump", "East and Face Out").autoIcon();


	private final ModifiableCallout<BuffRemoved> estinhog_baitGeir = new ModifiableCallout<>("Estinhog: Bait Geirskogul", "Bait Geirskogul");

//	private final ModifiableCallout<?> wyrmhole_number = new ModifiableCallout<>("Wyrmhole: Number Only", "Number {number}");

	private final ModifiableCallout<?> wyrmhole_place1 = new ModifiableCallout<>("Wyrmhole: Place #1", "Place Tower {where}, then {first} then {second}", 8_000);
	private final ModifiableCallout<?> wyrmhole_soak1 = new ModifiableCallout<>("Wyrmhole: Soak #1", "Stack, {first}, {second}, then soak tower", 8_000);
	private final ModifiableCallout<?> wyrmhole_nothing1 = new ModifiableCallout<>("Wyrmhole: Nothing #1", "Stack, {first}, {second}", 8_000);

	private final ModifiableCallout<?> wyrmhole_place2 = new ModifiableCallout<>("Wyrmhole: Place #2", "Place Tower {where}");
	// Actually comes out at the same time as the #3 calls since it gives the whole sequence
	private final ModifiableCallout<?> wyrmhole_soak2_firstPart = new ModifiableCallout<>("Wyrmhole: Soak #2", "Soak tower, bait, then stack");
	private final ModifiableCallout<?> wyrmhole_soak2_secondPart = new ModifiableCallout<>("Wyrmhole: Soak #2 Gnash/Lash", "{first} then {second}");

	private final ModifiableCallout<?> wyrmhole_place3 = new ModifiableCallout<>("Wyrmhole: Place #3", "Place Tower {where}, then {first} then {second}", 8_000);
	private final ModifiableCallout<?> wyrmhole_soak3_as1 = new ModifiableCallout<>("Wyrmhole: Soak #3 (As #1)", "Stack, {first}, {second}, then soak tower", 8_000);
	private final ModifiableCallout<?> wyrmhole_soak3_as2 = new ModifiableCallout<>("Wyrmhole: Soak #3 (As #2)", "Stack, {first}, {second}, then soak tower", 8_000);


	private final ModifiableCallout<?> estinhog_gnash = new ModifiableCallout<>("Estinhog: Gnash", "Out");
	private final ModifiableCallout<?> estinhog_lash = new ModifiableCallout<>("Estinhog: Lash", "In");

	private final ModifiableCallout<AbilityCastStart> estinhog_drachenlance = ModifiableCallout.durationBasedCall("Estinhog: Drachenlance", "Out of front");

	// Eyes
	private final ModifiableCallout<BuffApplied> redTether = new ModifiableCallout<BuffApplied>("Red Tether", "Red").autoIcon();
	private final ModifiableCallout<BuffApplied> blueTether = new ModifiableCallout<BuffApplied>("Blue Tether", "Blue").autoIcon();

	// Haurchefant
	private final ModifiableCallout<HaurchefantHpTracker> haurch_hp = new ModifiableCallout<>("Haurchefant HP bar", "", "HP", HaurchefantHpTracker::isExpired)
			.guiProvider(HaurchefantHpTracker::getComponent);

	// Thordan II
	private final ModifiableCallout<AbilityUsedEvent> twister = new ModifiableCallout<>("Thordan II: Twister", "Twister");
	private final ModifiableCallout<BuffApplied> thordan2_trio1_lightningOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("Wrath of the Heavens: Lightning on you", "Lightning").autoIcon();

	private final ModifiableCallout<HeadMarkerEvent> thordan2_trio1_blueMark = new ModifiableCallout<>("Wrath of the Heavens: Blue Marker", "Blue Marker");
	private final ModifiableCallout<TetherEvent> thordan2_trio1_tether = new ModifiableCallout<>("Wrath of the Heavens: Tether", "Tether");
	private final ModifiableCallout<?> thordan2_trio1_neither = new ModifiableCallout<>("Wrath of the Heavens: Nothing", "Nothing");
	private final ModifiableCallout<HeadMarkerEvent> thordan2_trio1_greenMark = new ModifiableCallout<>("Wrath of the Heavens: Blue Marker", "Green Marker");

	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_protean = ModifiableCallout.durationBasedCall("Wrath of the Heavens: Protean", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> thordan2_liquidHeavenOnYou = new ModifiableCallout<>("Liquid Heaven on you", "Liquid Heaven", "Liquid Heaven #{num}/5", ModifiableCallout.expiresIn(5));

	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_in = ModifiableCallout.durationBasedCall("Wrath of the Heavens: In", "In");
	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_inLightning = ModifiableCallout.durationBasedCall("Wrath of the Heavens: In with Lightning", "In with Lightning");

	private final ModifiableCallout<BuffApplied> doom = ModifiableCallout.<BuffApplied>durationBasedCall("Thordan II: Doom", "Doom").autoIcon();
	private final ModifiableCallout<?> noDoom = new ModifiableCallout<>("Thordan II: Non-Doom", "Puddle");
	private final ModifiableCallout<HeadMarkerEvent> t2_circleDoom = new ModifiableCallout<>("Circle (Doom)", "Red Circle (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_triangleNoDoom = new ModifiableCallout<>("Triangle (No Doom)", "Green Triangle");
	private final ModifiableCallout<HeadMarkerEvent> t2_triangleDoom = new ModifiableCallout<>("Triangle (Doom)", "Green Triangle (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_squareNoDoom = new ModifiableCallout<>("Square (No Doom)", "Purple Square");
	private final ModifiableCallout<HeadMarkerEvent> t2_squareDoom = new ModifiableCallout<>("Square (Doom)", "Purple Square (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_crossNoDoom = new ModifiableCallout<>("Cross (No Doom)", "Blue Cross");

	// Double Dragons
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_stack = ModifiableCallout.durationBasedCall("P6 Tankbuster (Stack)", "Shared Buster");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_niddBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Nidhogg Buster)", "Nidd Buster, Hraes Cleave");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_hraesBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Hraes Buster)", "Hraes Buster, Nidd Cleave");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_bothBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Both Buster)", "Busters");

	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_leftIn = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Left, near Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_rightIn = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Right, near Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_leftOut = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Left, away from Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_rightOut = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Right, away from Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> akhAfah = ModifiableCallout.durationBasedCall("Akh Afah", "Light Party Stacks");
	private final ModifiableCallout<AbilityCastStart> akhAfahHpCheckCall = new ModifiableCallout<AbilityCastStart>("Akh Afah HP Check", "", "HP Check: {hpcheck}", ModifiableCallout.durationExpiry());

	private final ModifiableCallout<BuffApplied> p6_spread = ModifiableCallout.<BuffApplied>durationBasedCall("Dragons: Spread", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> p6_stackWithDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Dragons: Stack Debuff", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> p6_stackWithoutDebuff = ModifiableCallout.durationBasedCall("Dragons: No Debuff", "Nothing");

	private final ModifiableCallout<AbilityCastStart> p6_hotTail = ModifiableCallout.durationBasedCall("Dragons: Hot Tail", "Out");
	private final ModifiableCallout<AbilityCastStart> p6_hotWing = ModifiableCallout.durationBasedCall("Dragons: Hot Wing", "In");

	private final ModifiableCallout<BuffApplied> hotDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Boiling", "Get hit by Hraesvelgr").autoIcon();
	private final ModifiableCallout<BuffApplied> coldDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Freezing", "Get hit by Nidhogg").autoIcon();
	private final ModifiableCallout<BuffApplied> pyretic_pre = ModifiableCallout.<BuffApplied>durationBasedCall("Pyretic (Pre-Call)", "Stop Moving Soon").statusIcon(0x3C0);
	private final ModifiableCallout<BuffApplied> pyretic = new ModifiableCallout<BuffApplied>("Pyretic", "Stop Moving", "Stop Moving", buff -> !getBuffs().originalStatusActive(buff)).statusIcon(0x3C0);


	private final BooleanSetting p6_useAutoMarks;
	private final BooleanSetting p6_altMarkMode;
	private final BooleanSetting p6_rotPrioHigh;
	private final BooleanSetting p6_reverseSort;
	private final JobSortSetting sortSetting;

	private final XivState state;
	private final StatusEffectRepository buffs;

	public Dragonsong(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
		this.state = state;
		this.buffs = buffs;
		p6_useAutoMarks = new BooleanSetting(pers, "triggers.dragonsong.use-auto-marks", false);
		p6_altMarkMode = new BooleanSetting(pers, "triggers.dragonsong.alt-mark-mode", true);
		p6_rotPrioHigh = new BooleanSetting(pers, "triggers.dragonsong.rot-highest-prio", true);
		p6_reverseSort = new BooleanSetting(pers, "triggers.dragonsong.rot-reverse-sort", true);
		sortSetting = new JobSortSetting(pers, "triggers.dragonsong.job-prio", state);
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x3C8);
	}

	@HandleEvents
	public void abilityCast(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		final ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x62D4 -> call = p1_holiestOfHoly;
			case 0x62DA -> {
				if (!isSecondPhase) {
					call = p1_emptyDimension;
				}
				else {
					return;
				}
			}
			case 0x62DB -> call = p1_fullDimension;
			case 0x62DD -> call = p1_heavensblaze;
			case 0x62D0 -> {
				//noinspection ConstantConditions
				if (state.getPlayerJob().caresAboutInterrupt()) {
					call = p1_holiestHallowing;
				}
				else {
					return;
				}
			}
			case 0x63C8 -> call = thordan_cleaveBait;
			case 0x63C6 -> call = thordan_quaga;
			case 0x63C1 -> call = thordan_broadSwingL;
			case 0x63C0 -> call = thordan_broadSwingR;
			case 0x63C7 -> call = thordan_heavenlyHeel;
			case 0x63D0 -> call = thordan_trio2_gaze;
			case 0x670B -> call = estinhog_drachenlance;
			// TODO: what should this call actually be?
//			case 0x62D6 -> call = p1_hyper;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@HandleEvents
	public void buffApplied(EventContext context, BuffApplied event) {
		// Brightwing
		if (event.getBuff().getId() == 0x6316) {
			context.accept(p1_brightwing.getModified(event));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p1_puddleBaitSeq = new SequentialTrigger<>(10_000, BaseEvent.class,
			e -> e instanceof BuffApplied ba && ba.getBuff().getId() == 0xA65 && ba.getTarget().isThePlayer(),
			(e1, s) -> {
				s.updateCall(p1_puddleBait.getModified((BuffApplied) e1));
				BuffRemoved removed = s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xA65 && br.getTarget().isThePlayer());
				s.updateCall(p1_puddleBaitAfter.getModified(removed));
			});


	@HandleEvents
	public void reset(EventContext context, PullStartedEvent event) {
		firstHeadmark = null;
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent zce) {
		isSecondPhase = false;
	}

	@HandleEvents
	public void finalBoss(EventContext context, AbilityUsedEvent event) {
		// Covers transition from first to second phase
		if (event.getSource().getbNpcId() == 0x313C && !isSecondPhase) {
			isSecondPhase = true;
			firstHeadmark = null;
		}
	}

	private boolean isSecondPhase;
	private Long firstHeadmark;

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@HandleEvents(order = -50_000)
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		getHeadmarkOffset(event);
	}


	@AutoFeed
	private final SequentialTrigger<BaseEvent> p1_fourHeadMark = new SequentialTrigger<>(30_000, BaseEvent.class,
			e -> e instanceof AbilityCastStart acs && acs.getAbility().getId() == 0x62DD,
			(e1, s) -> {
				if (s.waitEvents(4, HeadMarkerEvent.class, event -> getHeadmarkOffset(event) == 0)
						.stream().anyMatch(e -> e.getTarget().isThePlayer())) {
					s.accept(p1_firstCleaveMarker.getModified());
				}
				else {
					s.accept(p1_secondCleaveMarker.getModified());
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p1_pairsOfMarkers = new SequentialTrigger<>(20_000, BaseEvent.class,
			e -> e instanceof AbilityUsedEvent acs && acs.getAbility().getId() == 0x62D5,
			(e1, s) -> {
				List<HeadMarkerEvent> marks = s.waitEventsUntil(8, HeadMarkerEvent.class, e -> {
					int headmarkOffset = getHeadmarkOffset(e);
					return headmarkOffset >= 47 && headmarkOffset <= 50;
				}, AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x62DE);
				marks.stream().filter(e -> e.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(myMark -> {
							Optional<HeadMarkerEvent> partnerMarker = marks.stream().filter(e -> !e.getTarget().isThePlayer() && e.getMarkerId() == myMark.getMarkerId())
									.findAny();
							int adjustedId = getHeadmarkOffset(myMark);
							final ModifiableCallout<HeadMarkerEvent> call;
							switch (adjustedId) {
								case 47 -> call = circle;
								case 48 -> call = triangle;
								case 49 -> call = square;
								case 50 -> call = cross;
								default -> {
									return;
								}
							}
							XivCombatant partner = partnerMarker.map(HeadMarkerEvent::getTarget).orElse(null);
							s.accept(call.getModified(Map.of("partner", partner == null ? "nobody" : partner)));
						}, () -> log.error("No personal headmarker! Collected: [{}]", marks));
			}
	);

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 5, 5);
	private final ArenaPos tightArenaPos = new ArenaPos(100, 100, 3, 3);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thordan_firstTrio = new SequentialTrigger<>(28_000, BaseEvent.class,
			e -> e instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x63D3,
			(e1, s) -> {
				log.info("Thordan Trio 1: Start");

				// This new logic should work faster while still preserving pure log compatibility (it will just be delayed)
				// Comes from:
				// Ser Vellguine 12633:3636
				// Ser Paulecrain 12634:3637
				// Ser Ignasse 12635:3638
				List<XivCombatant> dashers;
				s.waitEvent(TargetabilityUpdate.class, tu -> tu.getTarget().getbNpcId() == 12604 && !tu.isTargetable());
				do {
					dashers = getState().getCombatants().values().stream().filter(cbt -> {
						long id = cbt.getbNpcId();
						return id == 12633 || id == 12634 || id == 12635;
					}).filter(cbt -> cbt.getPos() != null && arenaPos.distanceFromCenter(cbt.getPos()) > 20).toList();
					if (dashers.size() < 3) {
						s.refreshCombatants(200);
					}
					else {
						break;
					}
				} while (true);
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.all);
				dashers.stream()
						.map(arenaPos::forCombatant)
						.forEach(badSector -> {
							log.info("Thordan Trio 1: Unsafe spot: {}", badSector);
							safe.remove(badSector);
							safe.remove(badSector.opposite());
						});

				ModifiableCallout<?> safeSpot = null;
				if (safe.contains(ArenaSector.NORTH)) {
					safeSpot = nsSafe;
				}
				else if (safe.contains(ArenaSector.NORTHEAST)) {
					safeSpot = neSwSafe;
				}
				else if (safe.contains(ArenaSector.EAST)) {
					safeSpot = ewSafe;
				}
				else if (safe.contains(ArenaSector.SOUTHEAST)) {
					safeSpot = seNwSafe;
				}
				if (safeSpot != null) {
					s.accept(safeSpot.getModified());
				}
				else {
					log.error("Thordan Trio 1: Bad safespots: {}", safe);
				}


				List<HeadMarkerEvent> marks = s.waitEventsUntil(3,
						HeadMarkerEvent.class, e -> getHeadmarkOffset(e) == 0,
						AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x63DE);

				Job job = getState().getPlayerJob();
				log.info("Thordan Trio 1: Got Markers");
				if (job != null && job.isTank()) {
					s.accept(thordan_trio1_tank.getModified());
				}
				else {
					marks.stream()
							.filter(mark -> mark.getTarget().isThePlayer())
							.findAny()
							.ifPresentOrElse(
									mark -> s.accept(thordan_trio1_blueMarker.getModified(mark)),
									() -> s.accept(thordan_trio1_nothing.getModified()));
				}

				while (true) {
					Optional<ArenaSector> wheresThordan = getState().getCombatants().values().stream()
							.filter(cbt -> cbt.getbNpcId() == 0x313C)
							.map(arenaPos::forCombatant)
							.filter(ArenaSector::isOutside)
							.findAny();
					if (wheresThordan.isPresent()) {
						Map<String, Object> params = Map.of("wheresThordan", wheresThordan.get());
						s.accept(thordan_trio1_wheresThordan.getModified(params));
						break;
					}
					else {
						s.refreshCombatants(200);
					}
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thordan_secondTrio = new SequentialTrigger<>(35_000, BaseEvent.class,
			e -> e instanceof AbilityCastStart acs && acs.getAbility().getId() == 0x63E1,
			(e1, s) -> {
				log.info("Thordan Trio 2: Start");

				List<HeadMarkerEvent> swordMarks = s.waitEventsUntil(2,
						HeadMarkerEvent.class, e -> {
							int offSet = getHeadmarkOffset(e);
							log.info("Thordan Trio 2: Headmark offset {}", offSet);
							return offSet == -280 || offSet == -279;
						},
						AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x63D0);

				XivCombatant first = swordMarks.stream().filter(mark -> getHeadmarkOffset(mark) == -280)
						.map(HeadMarkerEvent::getTarget)
						.findAny()
						.orElse(null);
				XivCombatant second = swordMarks.stream().filter(mark -> getHeadmarkOffset(mark) == -279)
						.map(HeadMarkerEvent::getTarget)
						.findAny()
						.orElse(null);

				log.info("Thordan Trio 2: Got Markers {}", swordMarks);
				s.accept(thordan_trio2_swordMark.getModified(Map.of(
						"sword1", first == null ? "?" : first,
						"sword2", second == null ? "?" : second)));

				s.waitMs(2000);

				log.info("Thordan Trio 2: Waiting for combatants");
				while (true) {
					Optional<XivCombatant> jan = getState().getCombatantsListCopy().stream()
							// Should be Ser Janlenoux
							.filter(cbt -> cbt.getbNpcId() == 12632)
							.findAny();
					if (jan.isPresent()) {
						log.info("Jan present");
						Position pos = jan.get().getPos();
						if (pos != null) {
							log.info("Jan pos: {}", pos);
							if (pos.getX() > 100) {
								s.accept(thordan_trio2_ccw.getModified());
							}
							else {
								s.accept(thordan_trio2_cw.getModified());
							}
							break;
						}
						log.info("No Jan pos");
					}
					s.refreshCombatants(200);

				}

			});

	private enum IceFireRole {
		METEOR_ON_YOU,
		METEOR_ON_ROLE,
		NO_METEOR
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thordan_iceFire = new SequentialTrigger<>(60_000, BaseEvent.class,
			e -> e instanceof AbilityCastStart acs && acs.getAbility().getId() == 0x63E1,
			(e1, s) -> {
				// First call role (meteor on your, meteor on same role, no meteor)
				IceFireRole yourRole;
				List<BuffApplied> marks = s.waitEvents(2, BuffApplied.class, ba -> ba.getBuff().getId() == 0x232);
				if (marks.stream().anyMatch(mark -> mark.getTarget().isThePlayer())) {
					yourRole = IceFireRole.METEOR_ON_YOU;
				}
				else {
					Job pj = getState().getPlayerJob();
					if (pj == null) {
						log.error("Thordan Ice/Fire: player job was null!");
						return;
					}
					// TODO : meteor partner
					boolean playerIsDps = pj.isDps();
					boolean meteorIsDps = marks.stream().anyMatch(mark -> mark.getTarget() instanceof XivPlayerCharacter pc && pc.getJob().isDps());
					if (playerIsDps == meteorIsDps) {
						yourRole = IceFireRole.METEOR_ON_ROLE;
					}
					else {
						yourRole = IceFireRole.NO_METEOR;
					}
				}
				List<XivCombatant> meteors = marks.stream().map(BuffApplied::getTarget).toList();
				Map<String, Object> params = Map.of("meteors", meteors);
				switch (yourRole) {
					case METEOR_ON_YOU -> s.accept(thordan_trio2_meteorMark.getModified(params));
					case METEOR_ON_ROLE -> s.accept(thordan_trio2_meteorRoleMark.getModified(params));
					case NO_METEOR -> s.accept(thordan_trio2_nonMeteorRole.getModified(params));
				}
				s.waitEvent(BuffApplied.class, ba -> ba.getBuff().getId() == 0xB57);
				s.accept(thordan_trio2_firstTower.getModified());
				s.waitEvent(AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x62DC);
				double dist = arenaPos.distanceFromCenter(getState().getPlayer());
				boolean isOutside = dist > 5.0;
				if (isOutside) {
					s.accept(thordan_trio2_kbImmune.getModified());
				}
				else {
					s.accept(thordan_trio2_getKnockedBack.getModified());
				}
			});


	@AutoFeed
	private final SequentialTrigger<BaseEvent> meteorHelper = new SequentialTrigger<>(25_000, BaseEvent.class,
			e -> e instanceof BuffApplied ba && ba.getBuff().getId() == 0x232 && ba.getTarget().isThePlayer(),
			(e1, s) -> {
				log.info("Meteor helper start");
				// Logic:
				// Wait for tower to resolve first (I think this is 'conviction' 0x737C
				AbilityUsedEvent e = s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x737C);
				// Comets drop in pairs of two. Seven pairs total.
				MutableInt count = new MutableInt(1);
				s.accept(meteorDrop.getModified(e, Map.of("num", (Supplier<Integer>) count::getValue)));
				for (int i = 1; i <= 7; i++) {
					count.setValue(i);
					// Wait for the pair to drop
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x63E9 && aue.isFirstTarget());
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x63E9 && aue.isFirstTarget());
					log.info("Dropped meteor {}", i);
				}

			}
	);

	private final Predicate<BuffApplied> wyrmholeNumber = ba -> {
		long id = ba.getBuff().getId();
		return ba.getTarget().isThePlayer() && id >= 0xBBC && id <= 0xBBE;
	};

	private final Predicate<BuffApplied> wyrmholeDive = ba -> {
		long id = ba.getBuff().getId();
		return id >= 0xAC3 && id <= 0xAC5;
	};

	@AutoFeed
	private final SequentialTrigger<BaseEvent> wyrmhole = new SequentialTrigger<>(60_000, BaseEvent.class,
			// Start on final chorus
			e -> e instanceof AbilityUsedEvent a && a.getAbility().getId() == 0x6709 && a.isFirstTarget(),
			(e1, s) -> {
				log.info("Nidhogg start");
				// first/second/third in line
				BuffApplied inLineBuffApplied = s.waitEvent(BuffApplied.class, wyrmholeNumber.and(ba -> ba.getTarget().isThePlayer()));
				long myBuffId = inLineBuffApplied.getBuff().getId();
				int linePos = (int) myBuffId - 0xBBB;
				log.info("Nidhogg line pos: {}", linePos);
				switch (linePos) {
					case 1 -> s.accept(estinhog_headmark1.getModified(inLineBuffApplied));
					case 2 -> s.accept(estinhog_headmark2.getModified(inLineBuffApplied));
					case 3 -> s.accept(estinhog_headmark3.getModified(inLineBuffApplied));
				}
				boolean isMiddle = false;

//				s.accept(wyrmhole_number.getModified(Map.of("number", linePos)));

				final String whereDive;
				BuffApplied diveBuffApplied = s.waitEvent(BuffApplied.class, wyrmholeDive.and(ba -> ba.getTarget().isThePlayer()));
				// on/front/back
				int diveBuffId = (int) diveBuffApplied.getBuff().getId();
				log.info("Nidhogg dive buff: {}", diveBuffId);
				// If you have the front/rear buffs, then the problem is solved.
				// If you have the circle, you need to wait for the rest of the buffs to go out, so that you can see if
				// your *group* has all circles or not.
				whereDive = switch (diveBuffId) {
					case 0xAC3 -> {
						while (true) {
							Map<XivCombatant, Optional<BuffApplied>> collectedBuffs = getBuffs().getBuffs().stream().filter(ba -> ba.getBuff().getId() == myBuffId)
									.map(BuffApplied::getTarget)
									.collect(Collectors.toMap(Function.identity(), cbt -> getBuffs().statusesOnTarget(cbt)
											.stream()
											.filter(wyrmholeDive)
											.findAny()));
							if (collectedBuffs.values().stream().anyMatch(Optional::isEmpty)) {
								// Wait
								log.info("Waiting for more buffs. So far: {}", collectedBuffs);
								s.waitEvent(BuffApplied.class, wyrmholeDive);
							}
							else {
								if (collectedBuffs.values().stream().allMatch(o -> o.get().getBuff().getId() == 0xAC3)) {
									s.accept(estinhog_highJumpAll.getModified(diveBuffApplied));
									yield "Any Spot";
								}
								else {
									s.accept(estinhog_highJumpOnlyYou.getModified(diveBuffApplied));
									yield "On You";
								}
							}
						}
					}
					case 0xAC4 -> {
						s.accept(estinhog_spineshatter.getModified(diveBuffApplied));
						yield "In Front";
					}
					case 0xAC5 -> {
						s.accept(estinhog_elusiveJump.getModified(diveBuffApplied));
						yield "Behind You";
					}
					default -> "?";
				};

				// First, wait for the initial gnash and lash to start casting
				// If you are #1, you will place towers
				// If you are #2, you will stack
				// If you are #3, you will stack then soak towers
				{
					GnashLash firstGnashLash = waitGnashLash(s);
					Map<String, Object> params = Map.of("where", whereDive, "first", firstGnashLash.first, "second", firstGnashLash.second);
					if (linePos == 1) {
						s.accept(wyrmhole_place1.getModified(params));
					}
					else if (linePos == 3) {
						s.accept(wyrmhole_soak1.getModified(params));
					}
					else {
						s.accept(wyrmhole_nothing1.getModified(params));
					}
				}
				// First towers placed
				s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xBBC);
				if (linePos == 1) {
					// Try to guess whether player was middle or not
					ArenaSector sector = tightArenaPos.forCombatant(getState().getPlayer());
					isMiddle = sector == ArenaSector.NORTH || sector == ArenaSector.SOUTH;
				}
				log.info("Nidhogg: First Towers Placed");
//				if (linePos == 3) {
//					s.accept(estinhog_soakFirst.getModified());
//				}
				// 6711 is the damage from actually soaking
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x6711);
				log.info("Nidhogg: First Towers Soaked");
				if (linePos == 2) {
					s.accept(wyrmhole_place2.getModified(Map.of("where", whereDive)));
				}

				// Wait for second in line to go off, then call "soak 2" if needed
				s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xBBD);
				if (linePos == 1 && !isMiddle) {
					s.accept(wyrmhole_soak2_firstPart.getModified());

				}

				// TODO: "soak 2" call needs to be earlier
				// Second gnash/lash starts casting
				{
					GnashLash secondGnashLash = waitGnashLash(s);
					Map<String, Object> params = Map.of("where", whereDive, "first", secondGnashLash.first, "second", secondGnashLash.second);
					if (linePos == 1) {
						if (isMiddle) {
							s.accept(wyrmhole_soak3_as1.getModified(params));
						}
						else {
							s.accept(wyrmhole_soak2_secondPart.getModified(params));
						}
					}
					else if (linePos == 2) {
						s.accept(wyrmhole_soak3_as2.getModified(params));
					}
					else if (linePos == 3) {
						s.accept(wyrmhole_place3.getModified(params));
					}
				}
			});

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	private record GnashLash(AbilityCastStart event, String first, String second) {
	}

	private static GnashLash waitGnashLash(SequentialTriggerController<BaseEvent> s) {
//			0x6712 -> estinhog_gnashAndLash; out then in
//			0x6713 -> estinhog_lashAndGnash; in then out
		AbilityCastStart gnashLash = s.waitEvent(AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x6712 || acs.getAbility().getId() == 0x6713);
		long id = gnashLash.getAbility().getId();
		String first = id == 0x6712 ? "Out" : "In";
		String second = id == 0x6713 ? "Out" : "In";
		return new GnashLash(gnashLash, first, second);
	}

	@AutoFeed
	private final SequentialTrigger<AbilityUsedEvent> gnashLashHelper = new SequentialTrigger<>(8_000, AbilityUsedEvent.class,
			e -> {
				long id = e.getAbility().getId();
				return id == 0x6712 || id == 0x6713;
			}, (e1, s) -> {
		// 6712 -> out then in (gnash)
		// 6713 -> in then out (lash gnash)
		// 6715 -> the actual out (gnash)
		// 6716 -> the actual in (lash)
		boolean outFirst = e1.getAbility().getId() == 0x6712;
		RawModifiedCallout<?> firstCall = outFirst ? estinhog_gnash.getModified() : estinhog_lash.getModified();
		s.updateCall(firstCall);
		s.waitEvent(AbilityUsedEvent.class, aue -> aue.isFirstTarget() && (aue.getAbility().getId() == 0x6715 || aue.getAbility().getId() == 0x6716));
		s.updateCall(!outFirst ? estinhog_gnash.getModified() : estinhog_lash.getModified());
	});

	@HandleEvents
	public void geirskogul(EventContext ctx, AbilityUsedEvent event) {
		// I **think** this doesn't come up later in the fight judging by a p6 log I perused
		// 671B appears to be the "failed tower" ability
		long id = event.getAbility().getId();
		if ((id == 0x6711 || id == 0x6717 || id == 0x6718 || id == 0x6719 || id == 0x671A) && event.getTarget().isThePlayer()) {
			ctx.accept(estinhog_baitGeir.getModified());
		}
	}

	private RawModifiedCallout previousRedBlueCall;

	@HandleEvents
	public void doRedBlueTethers(EventContext ctx, BuffApplied ba) {
		if (ba.getTarget().isThePlayer()) {
			RawModifiedCallout call;
			long id = ba.getBuff().getId();
			if (id == 0xAD7) {
				call = redTether.getModified(ba);
			}
			else if (id == 0xAD8) {
				call = blueTether.getModified(ba);
			}
			else {
				return;
			}
			if (previousRedBlueCall != null) {
				call.setReplaces(previousRedBlueCall);
			}
			ctx.accept(call);
			previousRedBlueCall = call;
		}
	}

//	private final SequentialTrigger<BaseEvent> tetherTracker = new SequentialTrigger<>(100_000, BaseEvent.class,
//			// Start on steep of rage
//			be -> be instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x68BA && aue.getSource().getbNpcId() == 13119,
//			(e1, s) -> {
//				log.info("Tether tracker start");
//				while (true) {
//					List<BuffApplied> newApp = s.waitEventsUntil(1, BuffApplied.class, ba -> {
//						long id = ba.getBuff().getId();
//						return ba.getTarget().isThePlayer() && (id == 0xAD7 || id == 0xAD8);
//					}, BaseEvent.class, be ->
//							be instanceof WipeEvent
//									|| be instanceof EntityKilledEvent eke
//									&& (eke.getTarget().getbNpcId() == 12609 || eke.getTarget().getbNpcId() == 12610));
//					if (newApp.isEmpty()) {
//						log.info("Tether tracker done");
//						return;
//					}
//					else {
//						BuffApplied buff = newApp.get(0);
//						if (buff.getBuff().getId() == 0xAD7) {
//							s.updateCall(redTether.getModified(buff));
//						}
//						else {
//							s.updateCall(blueTether.getModified(buff));
//						}
//					}
//				}
//			}
//	);

	// TODO: figure out bug with this not disappearing
	private final class HaurchefantHpTracker {
		private final XivCombatant haurchInitial;
		private final BaseEvent e1;

		private HaurchefantHpTracker(XivCombatant haurchInitial, BaseEvent e1) {
			this.haurchInitial = haurchInitial;
			this.e1 = e1;
		}

		public XivCombatant getNewData() {
			return getState().getLatestCombatantData(haurchInitial);
		}

		public boolean isExpired() {
			if (e1.getEffectiveTimeSince().toSeconds() > 45) {
				return true;
			}
			if (getState().getLatestCombatantData(haurchInitial) == null) {
				return true;
			}
			return getBuffs().statusesOnTarget(haurchInitial).stream().noneMatch(ba -> ba.getBuff().getId() == 2977);
		}

		public Component getComponent() {
			RefreshingHpBar bar = new RefreshingHpBar(this::getNewData);
			bar.setPreferredSize(new Dimension(200, 20));
			bar.setFgTransparency(220);
			bar.setBgTransparency(128);
			return bar;
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> haurch_hp_track = new SequentialTrigger<>(60_000, BaseEvent.class,
			be -> be instanceof BuffApplied ba && ba.getBuff().getId() == 2977,
			(e1, s) -> {
				log.info("Haurch HP tracker start");
				XivCombatant haurch = ((BuffApplied) e1).getTarget();
				HaurchefantHpTracker tracker = new HaurchefantHpTracker(haurch, e1);
				s.accept(haurch_hp.getModified(tracker));
				log.info("Haurch HP tracker end");
			});

	@HandleEvents
	public void lightning(EventContext context, BuffApplied event) {
		if (event.getTarget().isThePlayer() && event.buffIdMatches(0xB11)) {
			context.accept(thordan2_trio1_lightningOnYou.getModified(event));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thordan2_trio1 = new SequentialTrigger<>(30_000, BaseEvent.class,
			event -> event instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x6B89,
			(e1, s) -> {
				List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, tether -> tether.getId() == 5);
				HeadMarkerEvent blueMark = s.waitEvent(HeadMarkerEvent.class);
				if (blueMark.getTarget().isThePlayer()) {
					s.accept(thordan2_trio1_blueMark.getModified(blueMark));
				}
				else {
					Optional<TetherEvent> tetherOnPlayer = tethers.stream()
							.filter(tether -> tether.eitherTargetMatches(XivCombatant::isThePlayer))
							.findAny();
					if (tetherOnPlayer.isPresent()) {
						s.accept(thordan2_trio1_tether.getModified(tetherOnPlayer.get()));
					}
					else {
						s.accept(thordan2_trio1_neither.getModified());
					}
				}

				HeadMarkerEvent greenMark = s.waitEvent(HeadMarkerEvent.class);
				if (greenMark.getTarget().isThePlayer()) {
					s.accept(thordan2_trio1_greenMark.getModified(greenMark));
				}
				AbilityCastStart spread = s.waitEvent(AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x63CA);
				s.accept(thordan2_trio1_protean.getModified(spread));

				AbilityCastStart donut = s.waitEvent(AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x62DA);
				if (getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(buff -> buff.getBuff().getId() == 0xB11)) {
					s.accept(thordan2_trio1_inLightning.getModified(donut));
				}
				else {
					s.accept(thordan2_trio1_in.getModified(donut));
				}

			}

	);


	@AutoFeed
	private final SequentialTrigger<AbilityUsedEvent> thordan2_liquidHeaven = new SequentialTrigger<>(20_000, AbilityUsedEvent.class,
			event -> event.getAbility().getId() == 0x6b91 && event.getTarget().isThePlayer() && event.isFirstTarget() && event.getSource().getbNpcId() == 12646,
			(e1, s) -> {
				AtomicInteger counter = new AtomicInteger(1);
				Supplier<Integer> num = counter::get;
				// Call first puddle
				s.updateCall(thordan2_liquidHeavenOnYou.getModified(e1, Map.of("num", num)));
				// Update for puddles 2-5
				for (int i = 2; i <= 5; i++) {
					// Intentionally not restricting it to first target, since you could die and it goes on someone else
					s.waitEvent(AbilityUsedEvent.class, event -> event.getAbility().getId() == 0x6b91 && event.isFirstTarget());
					counter.set(i);
				}
			}
	);


	private static final int baseHeadmarkerOffset = -49;
	private static final int endHeadmarkerOffset = baseHeadmarkerOffset + 3;

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thordan2_trio2 = new SequentialTrigger<>(60_000, BaseEvent.class,
			event -> event instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x6B92,
			(e1, s) -> {
				log.info("Death of the Heavens Start");
				List<BuffApplied> dooms = s.waitEvents(4, BuffApplied.class, ba -> ba.getBuff().getId() == 0xBA0);
				Optional<BuffApplied> playerDoom = dooms.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny();
				boolean playerHasDoom;
				if (playerDoom.isPresent()) {
					s.updateCall(doom.getModified(playerDoom.get()));
					playerHasDoom = true;
				}
				else {
					s.updateCall(noDoom.getModified());
					playerHasDoom = false;
				}
				log.info("Death of the Heavens: player has doom? {}", playerHasDoom);

				List<HeadMarkerEvent> marks = s.waitEventsUntil(8, HeadMarkerEvent.class, e -> {
					int headmarkOffset = getHeadmarkOffset(e);
					return headmarkOffset >= baseHeadmarkerOffset && headmarkOffset <= endHeadmarkerOffset;
				}, AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x62DE);
				marks.stream().filter(e -> e.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(myMark -> {
							Optional<HeadMarkerEvent> partnerMarker = marks.stream().filter(e -> !e.getTarget().isThePlayer() && e.getMarkerId() == myMark.getMarkerId())
									.findAny();
							int adjustedId = getHeadmarkOffset(myMark);
							final ModifiableCallout<HeadMarkerEvent> call;
							switch (adjustedId) {
								case baseHeadmarkerOffset -> call = t2_circleDoom;
								case baseHeadmarkerOffset + 1 ->
										call = playerHasDoom ? t2_triangleDoom : t2_triangleNoDoom;
								case baseHeadmarkerOffset + 2 -> call = playerHasDoom ? t2_squareDoom : t2_squareNoDoom;
								case baseHeadmarkerOffset + 3 -> call = t2_crossNoDoom;
								default -> {
									return;
								}
							}
							XivCombatant partner = partnerMarker.map(HeadMarkerEvent::getTarget).orElse(null);
							s.updateCall(call.getModified(Map.of("partner", partner == null ? "nobody" : partner)));
						}, () -> log.error("No personal headmarker! Collected: [{}]", marks));
				log.info("Death of the Heavens: done");

			});


	@HandleEvents
	public void abilityUsed(EventContext ctx, AbilityUsedEvent event) {
		if (event.getAbility().getId() == 0x6B8B && event.isFirstTarget()) {
			ctx.accept(twister.getModified(event));
		}
	}

	private enum WrothFlamesRole {
		SPREAD,
		STACK,
		NOTHING
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p6_wrothFlames = new SequentialTrigger<>(40_000, BaseEvent.class,
			// Start on Wroth Flames cast
			be -> be instanceof AbilityUsedEvent aue && aue.getAbility().getId() == 0x6D45,
			(e1, s) -> {
				log.info("Wroth Flames: Begin");
				// extra stop condition is for if people are dead
				List<BuffApplied> buffs = s.waitEventsUntil(6,
						BuffApplied.class, ba -> ba.buffIdMatches(2758, 2759),
						// Stop on Akh Morn
						AbilityCastStart.class, acs -> acs.abilityIdMatches(0x6D46));
				log.info("Wroth Flames: Collected buffs: {}", buffs);

				buffs.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny().ifPresentOrElse(
						buff -> {
							log.info("Wroth Flames: Player has buff {}", buff.getBuff().getId());
							if (buff.buffIdMatches(2758)) {
								s.updateCall(p6_spread.getModified(buff));
							}
							else {
								s.updateCall(p6_stackWithDebuff.getModified(buff));
							}
						}, () -> {
							// We still need duration from *something* for this to display the timer, so just grab one of the white debuffs
							buffs.stream()
									.filter(ba -> ba.buffIdMatches(2759))
									.findFirst()
									.ifPresent(buffApplied -> s.updateCall(p6_stackWithoutDebuff.getModified(buffApplied)));
						}
				);

				if (getP6_useAutoMarks().get()) {
					List<XivPlayerCharacter> noBuff = new ArrayList<>(getState().getPartyList());
					Map<WrothFlamesRole, List<XivPlayerCharacter>> playerMechs = new EnumMap<>(WrothFlamesRole.class);
					buffs.forEach(ba -> {
						// *Should* always be true, but just in case...
						if (ba.getTarget() instanceof XivPlayerCharacter player) {
							if (ba.getBuff().getId() == 2758) {
								playerMechs.computeIfAbsent(WrothFlamesRole.SPREAD, k -> new ArrayList<>()).add(player);
							}
							else {
								playerMechs.computeIfAbsent(WrothFlamesRole.STACK, k -> new ArrayList<>()).add(player);
							}
							noBuff.remove(player);
						}
					});
					playerMechs.put(WrothFlamesRole.NOTHING, noBuff);

					log.info("Wroth player mechs: {}", playerMechs);

					Comparator<XivPlayerCharacter> jobSort = getP6_sortSetting().getPlayerJailSortComparator();
					Comparator<XivPlayerCharacter> sort;
					if (getP6_rotPrioHigh().get()) {
						@Nullable XivCombatant rotPlayer = getBuffs().getBuffs().stream()
								.filter(ba -> ba.buffIdMatches(2896))
								.map(BuffApplied::getTarget)
								.findAny()
								.orElse(null);
						sort = Comparator.<XivPlayerCharacter, Integer>comparing(pc -> pc.equals(rotPlayer) ? -1 : 0).thenComparing(jobSort);
					}
					else {
						sort = jobSort;
					}
					if (getP6_reverseSort().get()) {
						sort = sort.reversed();
					}
					Comparator<XivPlayerCharacter> finalSort = sort;
					playerMechs.values().forEach(list -> list.sort(finalSort));

					List<XivPlayerCharacter> spreaders = playerMechs.get(WrothFlamesRole.SPREAD);
					List<XivPlayerCharacter> stackers = playerMechs.get(WrothFlamesRole.STACK);
					List<XivPlayerCharacter> otherStackers = playerMechs.get(WrothFlamesRole.NOTHING);

					// Give out markers
					spreaders.forEach(player -> s.accept(new SpecificAutoMarkRequest(player, MarkerSign.ATTACK_NEXT)));

					boolean altMode = getP6_altMarkMode().get();

					// People might be dead, so check count
					if (stackers.size() >= 1 && otherStackers.size() >= 1) {
						s.accept(new SpecificAutoMarkRequest(stackers.get(0), MarkerSign.BIND1));
						s.accept(new SpecificAutoMarkRequest(otherStackers.get(0), altMode ? MarkerSign.BIND2 : MarkerSign.IGNORE1));
					}
					if (stackers.size() >= 2 && otherStackers.size() >= 2) {
						s.accept(new SpecificAutoMarkRequest(stackers.get(1), altMode ? MarkerSign.IGNORE1 : MarkerSign.BIND2));
						s.accept(new SpecificAutoMarkRequest(otherStackers.get(1), MarkerSign.IGNORE2));
					}
					else {
						// but still warn that something went wrong
						log.warn("Wroth: Not enough stackers! With buff: {}, without: {}", stackers, otherStackers);
					}

					s.waitMs(25_000);
					s.accept(new ClearAutoMarkRequest());
				}
			}
	);

	private static final Predicate<AbilityCastStart> niddHraesBuster = acs -> acs.abilityIdMatches(0x6D32, 0x6D33, 0x6D34, 0x6D35);

	@AutoFeed
	private final SequentialTrigger<AbilityCastStart> p6_tankbuster = new SequentialTrigger<>(20_000, AbilityCastStart.class,
			niddHraesBuster,
			(e1, s) -> {
				AbilityCastStart e2 = s.waitEvent(AbilityCastStart.class, niddHraesBuster);
				boolean hraesGlow = e1.abilityIdMatches(0x6D35) || e2.abilityIdMatches(0x6D35);
				boolean niddGlow = e1.abilityIdMatches(0x6D33) || e2.abilityIdMatches(0x6D33);
				if (hraesGlow && niddGlow) {
					s.accept(p6_tankbuster_stack.getModified(e1));
				}
				else if (niddGlow) {
					s.accept(p6_tankbuster_hraesBuster.getModified(e1));
				}
				else if (hraesGlow) {
					s.accept(p6_tankbuster_niddBuster.getModified(e1));
				}
				else {
					s.accept(p6_tankbuster_bothBuster.getModified(e1));
				}
			});

	@HandleEvents
	public void p6casts(EventContext context, AbilityCastStart event) {
		// Hraesvelgr
		long npc = event.getSource().getbNpcId();
		int id = (int) event.getAbility().getId();
		final ModifiableCallout<AbilityCastStart> call;
		if (npc == 12613) {
			boolean playerIsTank = getState().playerJobMatches(Job::isTank);
			switch (id) {
				case 0x6D23 -> call = playerIsTank ? hallowedWingsAndPlume_leftIn : hallowedWingsAndPlume_leftOut;
				case 0x6D24 -> call = playerIsTank ? hallowedWingsAndPlume_leftOut : hallowedWingsAndPlume_leftIn;
				case 0x6D26 -> call = playerIsTank ? hallowedWingsAndPlume_rightIn : hallowedWingsAndPlume_rightOut;
				case 0x6D27 -> call = playerIsTank ? hallowedWingsAndPlume_rightOut : hallowedWingsAndPlume_rightIn;
				case 0x6D41 -> call = akhAfah;
				default -> {
					return;
				}
			}
		}
		else if (npc == 12612) {
			switch (id) {
				case 0x6D2B -> call = p6_hotWing;
				case 0x6D2D -> call = p6_hotTail;
				default -> {
					return;
				}
			}
		}
		else {
			return;
		}
		context.accept(call.getModified(event));
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> akhAfahHpCheck = new SequentialTrigger<>(20_000, BaseEvent.class,
			be -> be instanceof AbilityCastStart acs && acs.abilityIdMatches(0x6D41) && acs.getSource().getbNpcId() == 12613,
			(e1, s) -> {
				XivState state = getState();
				List<XivCombatant> cbts = state.getCombatantsListCopy();
				XivCombatant nidhogg = cbts.stream().filter(cbt -> cbt.getbNpcId() == 12612).findAny().orElseThrow(() -> new RuntimeException("Could not find Nidhogg!"));
				XivCombatant hraes = cbts.stream().filter(cbt -> cbt.getbNpcId() == 12613).findAny().orElseThrow(() -> new RuntimeException("Could not find Hraesvelgr!"));
				Supplier<String> hpCheckSupp = () -> {
					XivCombatant nidNow = state.getLatestCombatantData(nidhogg);
					XivCombatant hraesNow = state.getLatestCombatantData(hraes);

					//noinspection ConstantConditions - just let it error out if null
					double nidPct = nidNow.getHp().getPercent();
					//noinspection ConstantConditions
					double hraesPct = hraesNow.getHp().getPercent();
					double diff = hraesPct - nidPct;
					// Actual percentage is 2.9, but we want a buffer
					if (diff > 0.015) {
						return "Attack Hraesvelgr";
					}
					else if (diff < -0.015) {
						return "Attack Nidhogg";
					}
					else {
						return "Even";
					}
				};
				s.updateCall(akhAfahHpCheckCall.getModified((AbilityCastStart) e1, Map.of("hpcheck", hpCheckSupp)));


			}
	);

	@AutoFeed
	private final SequentialTrigger<BuffApplied> p6_hotCold = new SequentialTrigger<>(20_000, BuffApplied.class,
			ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xB52, 0xB53),
			(e1, s) -> {
				log.info("p6 hot/cold start");
				s.waitMs(e1.getEstimatedRemainingDuration().toMillis() - 7_000);
				if (e1.buffIdMatches(0xB52)) {
					log.info("p6 HOT: part 1");
					s.updateCall(hotDebuff.getModified(e1));
					s.waitMs(e1.getEstimatedRemainingDuration().toMillis() - 4_500);
					log.info("p6 HOT: part 2");
					s.updateCall(pyretic_pre.getModified(e1));
					BuffApplied pyreticApplied = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0x3C0));
					log.info("p6 HOT: part 3");
					s.updateCall(pyretic.getModified(pyreticApplied));
				}
				else {
					log.info("p6 COLD");
					s.updateCall(coldDebuff.getModified(e1));
				}
			});

	private final ModifiableCallout<AbilityCastStart> p7_exaflare_windup = ModifiableCallout.durationBasedCall("P7: Exaflare Windup", "Exaflare");
	private final ModifiableCallout<AbilityUsedEvent> p7_exaflare_1 = new ModifiableCallout<>("P7: Exaflare Hit #1", "Move", "Exaflare");
	private final ModifiableCallout<AbilityUsedEvent> p7_exaflare_2 = new ModifiableCallout<>("P7: Exaflare Hit #2", "Move", "Exaflare");
	private final ModifiableCallout<StatusLoopVfxApplied> p7_fire = new ModifiableCallout<>("P7: Fire", "Fire - Out");
	private final ModifiableCallout<StatusLoopVfxApplied> p7_ice = new ModifiableCallout<>("P7: Fire", "Ice - In");

	private final ModifiableCallout<AbilityCastStart> p7_akhMornEdge = ModifiableCallout.durationBasedCall("P7: Akh Morn's Edge", "Stacks");

	private final ModifiableCallout<AbilityCastStart> p7_gigaflareWindup = ModifiableCallout.durationBasedCall("P7: Gigaflare Windup", "Gigaflare");
	private final ModifiableCallout<AbilityUsedEvent> p7_gigaflare1 = new ModifiableCallout<>("P7: Gigaflare Hit #1", "Move", "Gigaflare");
	private final ModifiableCallout<AbilityUsedEvent> p7_gigaflare2 = new ModifiableCallout<>("P7: Gigaflare Hit #2", "Move", "Gigaflare");

	private final ModifiableCallout<AbilityCastStart> p7_enrage = ModifiableCallout.durationBasedCall("P7: Enrage", "Enrage");

	@HandleEvents
	public void p7casts(EventContext context, AbilityCastStart event) {
		if (event.getSource().getbNpcId() == 12616)
			if (event.abilityIdMatches(28051)) {
				context.accept(p7_akhMornEdge.getModified(event));
			}
			else if (event.abilityIdMatches(28206)) {
				context.accept(p7_enrage.getModified(event));
			}
	}

	@HandleEvents
	public void vfxHandler(EventContext context, StatusLoopVfxApplied event) {
		ModifiableCallout<StatusLoopVfxApplied> call;
		switch ((int) event.getStatusLoopVfx().getId()) {
			case 298 -> call = p7_fire;
			case 299 -> call = p7_ice;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p7_exaflare = new SequentialTrigger<>(20_000, BaseEvent.class,
			be -> be instanceof AbilityCastStart acs && acs.abilityIdMatches(28059),
			(e1, s) -> {
				s.updateCall(p7_exaflare_windup.getModified((AbilityCastStart) e1));
				{
					AbilityUsedEvent firstHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(28060));
					s.updateCall(p7_exaflare_1.getModified(firstHit));
				}
				{
					AbilityUsedEvent secondHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(28061));
					s.updateCall(p7_exaflare_2.getModified(secondHit));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> p7_gigaflare = new SequentialTrigger<>(20_000, BaseEvent.class,
			be -> be instanceof AbilityCastStart acs && acs.abilityIdMatches(28057),
			(e1, s) -> {
				s.updateCall(p7_gigaflareWindup.getModified((AbilityCastStart) e1));
				{
					AbilityUsedEvent firstHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(28058, 28114, 28115) && aue.isFirstTarget());
					s.updateCall(p7_gigaflare1.getModified(firstHit));
				}
				{
					AbilityUsedEvent secondHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(28058, 28114, 28115) && aue.isFirstTarget());
					s.updateCall(p7_gigaflare2.getModified(secondHit));
				}
			});

//	@HandleEvents
//	public void p6_pyretic(EventContext context, BuffApplied event) {
//		if (event.getTarget().isThePlayer() && event.getBuff().getId() == 0xB52) {
//			context.accept(pyretic.getModified(event));
//		}
//	}

	/*
		P7 notes:

		"Alternative End"
		29752: raidwide

		"Exaflare's Edge"
		28059: exaflare cast from real DKT (12616)
		28060: 3x exaflare initial hits from fake DKTs (9020)
		28061: 9x exaflare follow up hits from fake DKTs (later hits are 5x)

		"Trinity"
		0x6D9E: Trinity from real DKT (no telegraph)
		0x6D9F: Trinity (Dark resistance down)
		0x6DA0: Trinity (Light resistance down)
		0x6DA1: Trinity (Light + dark + phys resist down)
		2x trinity

		"Akh Morn's Edge"
		28051: tanks/light parties real cast "Akh Morn's Edge"
		29452/3/4: tanks/light parties actual hits (from fakes) (54 is tanks, 52 and 53 are the side hits)
		28050: ? comes from fake, immediately after (dynamo/chariot?) "Ice of Ascalon"

		"Akh Morn's Edge"
		28052: Comes from real
		28054: hits H/D
		28055: Hits Ts
			then 28052 again?
		Then lots of 28054 on H/D, 28055 on DPS (akh morns?)

		Is 28051 vs 28052 in vs out?

		Another trinity pair

		28057: real "Gigaflare's Edge"
		28058, 28114, 28115 from fakes
		28058 hits everyone
		28049 after (chariot/dynamo)? "Flames of Ascalon"
		28114 on everyone
		28115 on everyone

		Another Trinity pair

		Another Exaflare

		Another Trinity pair

		Another tanks/light parties cast

		Another 28052 etc set

		Another Trinity Pair

		Another 28057 mechanic

		Another Trinity pair

		Another Exaflare

		Another Trinity pair

		Another tanks/light parties cast

		Another 28052 set

		Another Trinity pair

		28206 from real - enrage?
		29455/6/7 from fakes




{
    "action_data": {
        "28049": {
            "Name_de": "Flamme von Askalon",
            "Name_en": "Flames of Ascalon",
            "Name_fr": "Feu d'Ascalon",
            "Name_ja": "\u30d5\u30ec\u30a4\u30e0\u30fb\u30aa\u30d6\u30fb\u30a2\u30b9\u30ab\u30ed\u30f3"
        },
        "28050": {
            "Name_de": "Eis von Askalon",
            "Name_en": "Ice of Ascalon",
            "Name_fr": "Glace d'Ascalon",
            "Name_ja": "\u30a2\u30a4\u30b9\u30fb\u30aa\u30d6\u30fb\u30a2\u30b9\u30ab\u30ed\u30f3"
        },
        "28051": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28052": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28053": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28054": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28055": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28056": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "28057": {
            "Name_de": "Gigaflare-Klinge",
            "Name_en": "Gigaflare's Edge",
            "Name_fr": "Lame de GigaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30ae\u30ac\u30d5\u30ec\u30a2"
        },
        "28058": {
            "Name_de": "Gigaflare-Klinge",
            "Name_en": "Gigaflare's Edge",
            "Name_fr": "Lame de GigaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30ae\u30ac\u30d5\u30ec\u30a2"
        },
        "28059": {
            "Name_de": "Exaflare-Klinge",
            "Name_en": "Exaflare's Edge",
            "Name_fr": "Lame d'ExaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a8\u30af\u30b5\u30d5\u30ec\u30a2"
        },
        "28060": {
            "Name_de": "Exaflare-Klinge",
            "Name_en": "Exaflare's Edge",
            "Name_fr": "Lame d'ExaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a8\u30af\u30b5\u30d5\u30ec\u30a2"
        },
        "28061": {
            "Name_de": "Exaflare-Klinge",
            "Name_en": "Exaflare's Edge",
            "Name_fr": "Lame d'ExaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a8\u30af\u30b5\u30d5\u30ec\u30a2"
        },
        "28114": {
            "Name_de": "Gigaflare-Klinge",
            "Name_en": "Gigaflare's Edge",
            "Name_fr": "Lame de GigaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30ae\u30ac\u30d5\u30ec\u30a2"
        },
        "28115": {
            "Name_de": "Gigaflare-Klinge",
            "Name_en": "Gigaflare's Edge",
            "Name_fr": "Lame de GigaBrasier",
            "Name_ja": "\u9a0e\u7adc\u5263\u30ae\u30ac\u30d5\u30ec\u30a2"
        },
        "28206": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "28207": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "28208": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "28209": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "29452": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "29453": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "29454": {
            "Name_de": "Akh Morns Klinge",
            "Name_en": "Akh Morn's Edge",
            "Name_fr": "Lame d'Akh Morn",
            "Name_ja": "\u9a0e\u7adc\u5263\u30a2\u30af\u30fb\u30e2\u30fc\u30f3"
        },
        "29455": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "29456": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "29457": {
            "Name_de": "Morn Afahs Klinge",
            "Name_en": "Morn Afah's Edge",
            "Name_fr": "Lame de Morn Afah",
            "Name_ja": "\u9a0e\u7adc\u5263\u30e2\u30fc\u30f3\u30fb\u30a2\u30d5\u30a1\u30fc"
        },
        "29752": {
            "Name_de": "Ein neues Ende",
            "Name_en": "Alternative End",
            "Name_fr": "Fin alternative",
            "Name_ja": "\u30a2\u30eb\u30c6\u30a3\u30e1\u30c3\u30c8\u30a8\u30f3\u30c9\u30fb\u30aa\u30eb\u30bf\u30ca"
        },
        "9259": {
            "Name_ko": "\uadf8\ub79c\ub4dc\ud06c\ub85c\uc2a4: \uc54c\ud30c"
        },
        "9260": {
            "Name_ko": "\uadf8\ub79c\ub4dc\ud06c\ub85c\uc2a4: \ub378\ud0c0"
        },
        "9261": {
            "Name_ko": "\uadf8\ub79c\ub4dc\ud06c\ub85c\uc2a4: \uc624\uba54\uac00"
        }
    },
    "bnpc_data": {
        "11319": {
            "Singular_de": "K\u00f6nig Thordan",
            "Singular_en": "Dragon-king Thordan",
            "Singular_fr": "Thordan le Dieu Dragon",
            "Singular_ja": "\u9a0e\u7adc\u795e\u30c8\u30fc\u30eb\u30c0\u30f3"
        },
        "6055": {
            "Singular_ko": "\ub124\uc624 \uc5d1\uc2a4\ub370\uc2a4"
        }
    },
    "instancecontenttextdata_data": {
        "17817": {
            "Text_ko": "\ubaa8\ub4e0 \uae30\uc5b5, \ubaa8\ub4e0 \uc874\uc7ac, \ubaa8\ub4e0 \ucc28\uc6d0\uc744 \uc18c\uba78\uc2dc\ud0a4\uace0\u2026\u2026"
        },
        "17818": {
            "Text_ko": "\uadf8\ub9ac\uace0 \ub098\ub3c4 \uc18c\uba78\ud560 \uac83\uc774\ub2e4\u2026\u2026."
        },
        "17819": {
            "Text_ko": "\uc601\uc6d0\ud788!!"
        },
        "32600": {
            "Text_de": "Den Lauf der Geschichte vermag niemand umzukehren. Und doch erwacht ein jeder hin wieder aus einem Traum, in dem es ihm gelang. Dies ist das Lied einer ertr\u00e4umten Zukunft, in der ein teurer Freund dem Tode knapp entrann...",
            "Text_en": "'Tis said that there are no ifs in history, yet man is wont to dream. Let us dream, then, of a future where a dear comrade lived...",
            "Text_fr": "Le pass\u00e9 a beau \u00eatre immuable, il n'emp\u00eachera jamais les hommes de r\u00eaver... Et si nous r\u00eavions tous ensemble \u00e0 la survie d'un ami cher disparu dans la fleur de l'\u00e2ge?",
            "Text_ja": "\u6b74\u53f2\u306b\u3082\u3057\u3082\u306f\u306a\u3044\u3068\u8a00\u3046\u304c \u305d\u308c\u3067\u3082\u4eba\u306f\u5922\u60f3\u3059\u308b\u3082\u306e\u306a\u3089\u3070 \u3053\u3053\u306b\u8a60\u304a\u3046 \u76df\u53cb\u3092\u6551\u3063\u305f\u5148\u306b\u5f85\u3064\u672a\u6765\u3092\u2015\u2015"
        },
        "32613": {
            "Text_de": "Und so ward die erste Strophe einer anderen Zukunft geschrieben. Doch die Tr\u00e4ume eines fahrenden S\u00e4ngers sind lang, und das ganze Lied \u00fcber die vielen Pfade der Geschichte noch nicht zuende gesungen...",
            "Text_en": "Thus did one song draw to a close. But here and now, this minstrel shall perform anothera song of imagination transcending...",
            "Text_fr": "Ainsi s'ach\u00e8ve ce chant des dragons, mais le m\u00e9nestrel errant a bien d'autres vers tenant du miracle au bout des cordes de sa harpe...",
            "Text_ja": "\u304b\u304f\u3066 \u3072\u3068\u3064\u306e\u7adc\u8a69\u306f\u7d42\u308f\u308a\u3092\u544a\u3052\u308b\u3060\u304c \u8a69\u4eba\u3068\u3057\u3066\u3053\u3053\u306b\u8a60\u304a\u3046 \u8d77\u3053\u308b\u306f\u305a\u306e\u306a\u3044\u5947\u8de1\u306e\u8a69\u3092\u2015\u2015"
        },
        "32615": {
            "Text_de": "Hahahaha! Seht mich an! Erzittert vor der Macht des allm\u00e4chtigen Gottes, gen\u00e4hrt von den Augen seiner Feinde!",
            "Text_en": "Hahahaha! By the power of mine enemy's eyes, I am become a god eternal!",
            "Text_fr": "Hahahahaha! Admirez la puissance des Yeux du dragon, et tremblez devant votre nouveau Dieu!",
            "Text_ja": "\u30cf\u30cf\u30cf\u30cf\u30cf\uff01\u3000\u898b\u3088\uff01\u3000\u754f\u308c\u3088\uff01\u3059\u3079\u3066\u306e\u7adc\u306e\u773c\u3092\u5f97\u3066\u3001\u6c38\u9060\u306e\u795e\u304c\u3001\u4eca\u3053\u3053\u306b\u964d\u81e8\u3059\u308b\u306e\u3060\uff01"
        },
        "32617": {
            "Text_de": "O Askalon! L\u00e4utere die vom Licht Befleckten mit unermesslichen Qualen!",
            "Text_en": "O Ascalon! Purge the tainted with the light of sorrow immeasurable!",
            "Text_fr": "\u00d4 Ascalon, sainte \u00e9p\u00e9e! Que ta lame funeste pourfende la Lumi\u00e8re!",
            "Text_ja": "\u773c\u3092\u55b0\u3089\u3044\u3057\u8056\u5263\u3088\uff01\u305d\u306e\u60b2\u5606\u306e\u8f1d\u304d\u3067\u3001\u5149\u306e\u4f7f\u5f92\u3092\u713c\u304d\u5c3d\u304f\u305b\uff01"
        },
        "32618": {
            "Text_de": "O Askalon! Zerr\u00fctte die Unw\u00fcrdigen mit dem Glei\u00dfen endlosen Grolls!",
            "Text_en": "O Ascalon! Consign the wicked with the light of rancor unquenchable!",
            "Text_fr": "\u00d4 Ascalon, sainte \u00e9p\u00e9e! Que ta lame rageuse s'abatte et donne la mort!",
            "Text_ja": "\u773c\u3092\u55b0\u3089\u3044\u3057\u8056\u5263\u3088\uff01\u305d\u306e\u6028\u5ff5\u306e\u8f1d\u304d\u3067\u3001\u6b7b\u306e\u5e95\u306b\u53e9\u304d\u843d\u3068\u305b\uff01"
        },
        "32619": {
            "Text_de": "O Askalon! Versenge die Erde mit der bei\u00dfenden Glut des Grams!",
            "Text_en": "O Ascalon! Scorch the earth with the light of regret unfathomable!",
            "Text_fr": "\u00d4 Ascalon, sainte \u00e9p\u00e9e! Que ta lame sinistre consume la terre enti\u00e8re!",
            "Text_ja": "\u773c\u3092\u55b0\u3089\u3044\u3057\u8056\u5263\u3088\uff01\u305d\u306e\u6094\u6068\u306e\u8f1d\u304d\u3067\u3001\u5927\u5730\u3082\u308d\u3068\u3082\u713c\u304d\u7126\u304c\u305b\uff01"
        },
        "32620": {
            "Text_de": "Augen des Feindes! Es ist Zeit, die Ungl\u00e4ubigen zur Strecke zu bringen!",
            "Text_en": "Your time is come! Eyes of mine enemy, bring oblivion unto the unbelievers!",
            "Text_fr": "Yeux du dragon! Soyez magnanimes, et offrez la mort \u00e9ternelle aux ennemis du Saint-Si\u00e8ge!",
            "Text_ja": "\u305d\u308d\u305d\u308d\u7d42\u3044\u3060\u2026\u2026\uff01\u3059\u3079\u3066\u306e\u773c\u3088\u3001\u6c38\u9060\u306e\u6b7b\u3092\u3053\u3053\u306b\u2026\u2026\uff01"
        },
        "32622": {
            "Text_de": "Fortschritt im alternativen Drachenkrieg",
            "Text_en": "Alternate Dragonsong War Progress",
            "Text_fr": "Avancement de la Guerre du chant des dragons alternative",
            "Text_ja": "\u507d\u5178\u7adc\u8a69\u6226\u4e89\u9032\u884c\u5ea6"
        },
        "32625": {
            "Text_de": "Ein einziges Leben vermag den Lauf der Geschichte zu \u00e4ndern... Unterjocht von der Kraft eines uralten Reliktes, stellte der heilige Drache Hraesvelgr sich auf die Seite seines rachs\u00fcchtigen Bruders.",
            "Text_en": "A single life can alter the course of history... Enslaved by an ancient relic, the great wyrm Hraesvelgr descended upon Ishgard at his vengeful brother's side.",
            "Text_fr": "Il suffit parfois d'un seul \u00eatre pour changer le cours de l'Histoire, et c'est ainsi que le dragon sacr\u00e9 Hraesvelgr, sous l'influence de la technologie allagoise, plongea sur Ishgard pour se battre aux c\u00f4t\u00e9s de son fr\u00e8re...",
            "Text_ja": "\u3072\u3068\u3064\u306e\u547d\u304c \u6b74\u53f2\u306e\u6d41\u308c\u3092\u5909\u3048\u3066\u3086\u304f\u2015\u2015\u53e4\u4ee3\u306e\u907a\u7269\u306b\u3088\u308a\u64cd\u3089\u308c\u3057\u8056\u7adc\u304c \u90aa\u7adc\u3068\u5171\u306b\u821e\u3044\u964d\u308a\u308b"
        },
        "32626": {
            "Text_de": "K\u00f6nig Thordan nutzte die dunkle Gunst der Stunde und verleibte sich die Lebenskraft beider Drachen ein, die ihm eine noch g\u00f6ttlichere, noch entsetzlichere Macht verliehen als zuvor.",
            "Text_en": "Thus did a dreadful new divinity ariseone endowed with the nigh-irrepressible life force of two great wyrms...",
            "Text_fr": "Conscient de cette opportunit\u00e9 unique, le roi profita de l'occasion pour immoler les deux cr\u00e9atures anciennes sur l'autel d'une divinit\u00e9 dont la f\u00e9rocit\u00e9 d\u00e9passe l'imagination: Thordan le Dieu Dragon...",
            "Text_ja": "\u90aa\u7adc\u3068\u8056\u7adc\u306e\u547d\u3092\u7ce7\u3068\u3057 \u65b0\u305f\u306a\u308b\u6c38\u9060\u306e\u795e\u304c\u964d\u81e8\u3057\u305f\u3042\u3048\u3066 \u305d\u306e\u540d\u3092\u3053\u3046\u547c\u307c\u3046 \u9a0e\u7adc\u795e\u30c8\u30fc\u30eb\u30c0\u30f3\u3068\u2015\u2015"
        },
        "32627": {
            "Text_de": "Nun, Krieger des Lichts? Sag mir, wie gedenkst du, diesem Krieg ein Ende zu setzen?",
            "Text_en": "Tell me, Warrior of Light. How do you propose to end this conflict?",
            "Text_fr": "Laisse-moi te poser une question, Guerri\u0002\b\r\u0005\u0005\u00e8re\u0003er\u0003 de la Lumi\u00e8re... Comment comptes-tu mettre fin \u00e0 ce conflit mill\u00e9naire?",
            "Text_ja": "\u3055\u3066\u3001\u5149\u306e\u4f7f\u5f92\u3088\u3001\u3053\u3053\u3067\u554f\u304a\u3046\u3002\u8cb4\u69d8\u306f\u3044\u304b\u306b\u3057\u3066\u3001\u3053\u306e\u5343\u5e74\u6226\u4e89\u3092\u7d42\u308f\u3089\u305b\u3088\u3046\u3068\u3044\u3046\u306e\u3060\uff1f"
        },
        "32628": {
            "Text_de": "Glaubst du, mein Tod k\u00f6nne den tausend Jahre w\u00e4hrenden Blutdurst des Schlachtens stillen? Dann bist du ein t\u00f6richter Narr des Lichts!",
            "Text_en": "If you believe that eliminating me will undo a thousand years of strife and suffering, then you are a fool.",
            "Text_fr": "Ne crois pas que tu y parviendras simplement en m'\u00e9liminant... Rien, pas m\u00eame ma disparition, ne saurait effacer mille ans de souffrance et de chaos...",
            "Text_ja": "\u30ef\u30b7\u3092\u6392\u305b\u3070\u3001\u3053\u306e\u6226\u304c\u7d42\u308f\u308a\u3001\u5343\u5e74\u306e\u798d\u6839\u304c\u65ad\u3066\u308b\u3068\u3067\u3082\uff1f\u3082\u3046\u3084\u3081\u3088\u3046\u3067\u306f\u306a\u3044\u304b\u3001\u5149\u306e\u4f7f\u5f92\u3088\u2026\u2026\u3002"
        }
    },
    "logmessage_data": {
        "2853": {
            "Text_ko": "\uc6b0 \uc8fc \uc758 \ubc95 \uce59 \uc774 \ud750 \ud2b8 \ub7ec \uc9d1 \ub2c8 \ub2e4 !!"
        }
    },
    "npcyell_data": {
        "13580": {
            "Text_de": "Du... bist wohlauf? Welch ein Gl\u00fcck...",
            "Text_en": "You...you are unharmed? Thank goodness...",
            "Text_fr": "Tu n'as rien... Tant mieux...",
            "Text_ja": "\u7121\u4e8b\u2026\u3060\u3063\u305f\u306e\u3060\u306a\u2026\u3088\u304b\u3063\u305f\u2026\u672c\u5f53\u306b\u2026"
        },
        "13586": {
            "Text_de": "Ich ergebe mich... Bitte, hab Gnade...",
            "Text_en": "I yield... Have mercy, I beg you...",
            "Text_fr": "Je me rends... Par piti\u00e9, \u00e9pargne-moi...",
            "Text_ja": "\u8ca0\u3051\u3092\u2026\u8a8d\u3081\u3088\u3046\u2026\u3086\u3048\u306b\u3001\u547d\u3060\u3051\u306f\u2026\u983c\u3080\u2026"
        },
        "13587": {
            "Text_de": "Das ist Thordan! Was um alles in der Welt hat er vor...?!",
            "Text_en": "Thordan!? Seven hells, he cannot mean to!",
            "Text_fr": "... Thordan!! Voil\u00e0 donc le v\u00e9ritable pouvoir des Yeux du dragon...",
            "Text_ja": "\u2026\u3042\u308c\u306f\u3001\u6559\u7687\u30c8\u30fc\u30eb\u30c0\u30f3\uff01\u307e\u3055\u304b\u3001\u3059\u3079\u3066\u306e\u7adc\u306e\u773c\u3092\u2026\uff01\uff1f"
        },
        "13588": {
            "Text_de": "Ha! Dein Mitgef\u00fchl wird dein Untergang sein...",
            "Text_en": "Hmph, your compassion will be the end of you...",
            "Text_fr": "Tu regretteras ta mis\u00e9ricorde, disciple de la Lumi\u00e8re...",
            "Text_ja": "\u7518\u3044\u306a\u3001\u5149\u306e\u4f7f\u5f92\u3088\u2026\u305d\u306e\u7518\u3055\u3092\u5f8c\u6094\u305b\u306c\u3053\u3068\u3060\u2026"
        },
        "13597": {
            "Text_de": "Bringen wir Estinien in Sicherheit! Rasch! ",
            "Text_en": "We must see Estinien to safety!",
            "Text_fr": "Ugh... Il faut mettre Estinien en s\u00e9curit\u00e9 imm\u00e9diatement!",
            "Text_ja": "\u304f\u3063\u2026\u4eca\u306f\u3001\u30a8\u30b9\u30c6\u30a3\u30cb\u30a2\u30f3\u6bbf\u3092\u5b89\u5168\u306a\u5834\u6240\u3078\u2026\uff01"
        }
    },
    "status_data": {
        "1379": {
            "Description_ko": "\ucc9c\uccb4 \ub9c8\ubc95\uc5d0 \uac78\ub9b0 \uc0c1\ud0dc. HP\uac00 \uc11c\uc11c\ud788 \uc904\uc5b4\ub4e0\ub2e4.",
            "Name_ko": "\uc54c\ub9c8\uac8c\uc2a4\ud2b8"
        },
        "2748": {
            "Description_de": "Eines Freundes inniger Wunsch erm\u00f6glicht den Angriff auf Nidhoggs rechtes Auge.",
            "Description_en": "A beloved friend is making it possible to attack Nidhogg's right eye.",
            "Description_fr": "Un ami cher permet d'attaquer l'\u0153il droit de Nidhogg.",
            "Description_ja": "\u76df\u53cb\u306b\u8a17\u3055\u308c\u305f\u60f3\u3044\u306b\u3088\u3063\u3066\u3001\u90aa\u7adc\u306e\u53f3\u773c\u3078\u306e\u653b\u6483\u304c\u53ef\u80fd\u306a\u72b6\u614b\u3002",
            "Name_de": "Essenz der Freundschaft",
            "Name_en": "Soul of Friendship",
            "Name_fr": "Amiti\u00e9 \u00e9ternelle",
            "Name_ja": "\u76df\u53cb\u306e\u60f3\u3044"
        },
        "2749": {
            "Description_de": "Eisherz' inniger Wunsch erm\u00f6glicht den Angriff auf Nidhoggs linkes Auge.",
            "Description_en": "A faithful ally is making it possible to attack Nidhogg's left eye.",
            "Description_fr": "C\u0153ur-de-glace permet d'attaquer l'\u0153il gauche de Nidhogg.",
            "Description_ja": "\u6c37\u306e\u5deb\u5973\u306b\u8a17\u3055\u308c\u305f\u60f3\u3044\u306b\u3088\u3063\u3066\u3001\u90aa\u7adc\u306e\u5de6\u773c\u3078\u306e\u653b\u6483\u304c\u53ef\u80fd\u306a\u72b6\u614b\u3002",
            "Name_de": "Essenz der Tugend",
            "Name_en": "Soul of Devotion",
            "Name_fr": "D\u00e9votion \u00e9ternelle",
            "Name_ja": "\u5deb\u5973\u306e\u60f3\u3044"
        },
        "2758": {
            "Description_de": "Gefesselt von der Macht Nidhoggs unb\u00e4ndigen Durstes nach Vergeltung. ",
            "Description_en": "Powerless against Nidhogg's desire for vengeance.",
            "Description_fr": "La cible est assujettie par la volont\u00e9 de vengeance de Nidhogg.",
            "Description_ja": "\u90aa\u7adc\u30cb\u30fc\u30ba\u30d8\u30c3\u30b0\u306e\u5fa9\u8b90\u3092\u671b\u3080\u5ff5\u306b\u7e1b\u3089\u308c\u305f\u72b6\u614b\u3002",
            "Name_de": "Flammende Rache",
            "Name_en": "Spreading Flames",
            "Name_fr": "Vengeance consumante",
            "Name_ja": "\u5fa9\u8b90\u306e\u708e"
        },
        "2759": {
            "Description_de": "Wehrlos gegen Nidhoggs Wunsch, sein Leid auch andere sp\u00fcren zu lassen.",
            "Description_en": "Powerless against Nidhogg's desire that another share his suffering.",
            "Description_fr": "La cible est assujettie par la volont\u00e9 d'enchev\u00eatrement de Nidhogg.",
            "Description_ja": "\u90aa\u7adc\u30cb\u30fc\u30ba\u30d8\u30c3\u30b0\u306e\u9053\u9023\u308c\u3092\u671b\u3080\u5ff5\u306b\u7e1b\u3089\u308c\u305f\u72b6\u614b\u3002",
            "Name_de": "Verwobene Flammen",
            "Name_en": "Entangled Flames",
            "Name_fr": "Flammes enchev\u00eatr\u00e9es",
            "Name_ja": "\u9053\u9023\u308c\u306e\u708e"
        },
        "2777": {
            "Description_de": "Kein Zustandswechsel zwischen Nidhoggs Fang und Klaue m\u00f6glich. ",
            "Description_en": "Unable to transition between Clawbound and Fangbound states.",
            "Description_fr": "Les caract\u00e9ristiques de la griffe et du croc de Nidhogg sont immuables.",
            "Description_ja": "\u90aa\u7adc\u306e\u722a\u3068\u90aa\u7adc\u306e\u7259\u306e\u6027\u8cea\u304c\u5909\u5316\u3057\u306a\u304f\u306a\u3063\u305f\u72b6\u614b\u3002",
            "Name_de": "Nidhoggs Stigmata",
            "Name_en": "Bound and Determined",
            "Name_fr": "Stigmates de Nidhogg",
            "Name_ja": "\u722a\u7259\u4e0d\u5909"
        },
        "2895": {
            "Description_de": "H\u00e4lt sein Versprechen an Shiva, kein fremdes Blut mehr zu vergie\u00dfen. ",
            "Description_en": "Recognized under the oath Hraesvelgr swore to his beloved Shivathat he would never kill her kin.",
            "Description_fr": "Hraesvelgr a promis \u00e0 Shiva de ne plus tuer d'humains.",
            "Description_ja": "\u8056\u7adc\u30d5\u30ec\u30fc\u30b9\u30f4\u30a7\u30eb\u30b0\u304c\u3001\u611b\u3059\u308b\u30b7\u30f4\u30a1\u3092\u55b0\u3089\u3063\u305f\u969b\u306b\u7acb\u3066\u305f\u4e0d\u6bba\u306e\u8a93\u3044\u3002",
            "Name_de": "Schwur des Friedens",
            "Name_en": "Solemn Vow",
            "Name_fr": "Serment de paix",
            "Name_ja": "\u4e0d\u6bba\u306e\u8a93\u3044"
        },
        "2896": {
            "Description_de": "Unter Einfluss von Nidhoggs Schwur, sich an Ratatoskrs M\u00f6rdern zu r\u00e4chen. Erhaltene Heileffekte sind verringert und es wird schrittweise Schaden erlitten. Bei Ende des Effekts wird allen Umstehenden schlimmer Schmerz zugef\u00fcgt.",
            "Description_en": "Condemned by Nidhogg's vow to avenge his brood-sister. Healing potency is decreased. Taking damage over time, and will inflict anguish on those nearby in turn when this effect expires.",
            "Description_fr": "Cible de la haine de Nidhogg ayant pour cause la perte de Ratatosk. Les d\u00e9g\u00e2ts inflig\u00e9s et la puissance des effets curatifs prodigu\u00e9s sont r\u00e9duits, et des d\u00e9g\u00e2ts p\u00e9riodiques sont subis. Lorsque l'effet prend fin, une douleur atroce est inflig\u00e9e aux alentours.",
            "Description_ja": "\u90aa\u7adc\u30cb\u30fc\u30ba\u30d8\u30c3\u30b0\u304c\u611b\u3059\u308b\u8a69\u7adc\u3092\u5931\u3063\u305f\u969b\u306b\u7acb\u3066\u305f\u6ec5\u6bba\u306e\u8a93\u3044\u3001\u305d\u306e\u5bfe\u8c61\u3068\u306a\u3063\u305f\u72b6\u614b\u3002\u81ea\u8eab\u304b\u3089\u306e\uff28\uff30\u56de\u5fa9\u52b9\u679c\u304c\u4f4e\u4e0b\u3057\u3001\u304b\u3064\uff28\uff30\u304c\u5f90\u3005\u306b\u5931\u308f\u308c\u308b\u3002\u307e\u305f\u52b9\u679c\u7d42\u4e86\u6642\u306b\u3001\u5468\u56f2\u306b\u82e6\u75db\u3092\u4e0e\u3048\u308b\u3002",
            "Name_de": "Schwur der Vergeltung",
            "Name_en": "Mortal Vow",
            "Name_fr": "V\u0153u d'an\u00e9antissement",
            "Name_ja": "\u6ec5\u6bba\u306e\u8a93\u3044"
        },
        "2897": {
            "Description_de": "Von Nidhoggs Schwur der Vergeltung befreit.",
            "Description_en": "No longer condemned by Nidhogg's Mortal Vow.",
            "Description_fr": "La cible est lib\u00e9r\u00e9e du v\u0153u d'an\u00e9antissement de Nidhogg.",
            "Description_ja": "\u6ec5\u6bba\u306e\u8a93\u3044\u306e\u5bfe\u8c61\u304b\u3089\u5916\u308c\u305f\u72b6\u614b\u3002",
            "Name_de": "Versiegte Vergeltung",
            "Name_en": "Mortal Atonement",
            "Name_fr": "V\u0153u d'an\u00e9antissement rompu",
            "Name_ja": "\u6ec5\u6bba\u306e\u511f\u3044"
        },
        "2977": {
            "Description_de": "Erhaltene Heileffekte sind um 20 % verringert.",
            "Description_en": "HP recovery is reduced by 20%.",
            "Description_fr": "L'effet des sorts de restauration des PV est r\u00e9duit de 20%.",
            "Description_ja": "\u81ea\u8eab\u306b\u5bfe\u3059\u308b\uff28\uff30\u56de\u5fa9\u52b9\u679c\u304c20\uff05\u4f4e\u4e0b\u3057\u305f\u72b6\u614b\u3002",
            "Name_de": "Heilung -",
            "Name_en": "HP Recovery Down",
            "Name_fr": "Soins diminu\u00e9s",
            "Name_ja": "\u88ab\u56de\u5fa9\u4f4e\u4e0b"
        },
        "2978": {
            "Description_de": "Erhaltene Heileffekte sind um 100 % verringert.",
            "Description_en": "HP recovery is reduced by 100%.",
            "Description_fr": "L'effet des sorts de restauration des PV est r\u00e9duit de 100%.",
            "Description_ja": "\u81ea\u8eab\u306b\u5bfe\u3059\u308b\uff28\uff30\u56de\u5fa9\u52b9\u679c\u304c100\uff05\u4f4e\u4e0b\u3057\u305f\u72b6\u614b\u3002",
            "Name_de": "Heilung -",
            "Name_en": "HP Recovery Down",
            "Name_fr": "Soins diminu\u00e9s",
            "Name_ja": "\u88ab\u56de\u5fa9\u4f4e\u4e0b"
        }
    }
}

	 */


	public BooleanSetting getP6_useAutoMarks() {
		return p6_useAutoMarks;
	}

	public BooleanSetting getP6_altMarkMode() {
		return p6_altMarkMode;
	}

	public JobSortSetting getP6_sortSetting() {
		return sortSetting;
	}

	public BooleanSetting getP6_rotPrioHigh() {
		return p6_rotPrioHigh;
	}

	public BooleanSetting getP6_reverseSort() {
		return p6_reverseSort;
	}

	private XivState getState() {
		return state;
	}
}
