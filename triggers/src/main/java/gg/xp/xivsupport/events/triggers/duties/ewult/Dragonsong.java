package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
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
import gg.xp.xivsupport.speech.CalloutEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
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

@CalloutRepo("Dragonsong's Reprise")
public class Dragonsong extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(Dragonsong.class);

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

	private final ModifiableCallout<AbilityCastStart> thordan_cleaveBait = ModifiableCallout.durationBasedCall("Ascalon's Mercy", "Cleave Bait");
	private final ModifiableCallout<AbilityCastStart> thordan_quaga = ModifiableCallout.durationBasedCall("Ancient Quaga", "Raidwide");

	private final ModifiableCallout<?> nsSafe = new ModifiableCallout<>("Trio 1 N/S Safe", "North/South Safe", "North South Safe");
	private final ModifiableCallout<?> neSwSafe = new ModifiableCallout<>("Trio 1 NE/SW Safe", "Northeast/Southwest Safe", "Northeast Southwest Safe");
	private final ModifiableCallout<?> ewSafe = new ModifiableCallout<>("Trio 1 E/W Safe", "East/West Safe", "East West Safe");
	private final ModifiableCallout<?> seNwSafe = new ModifiableCallout<>("Trio 1 SE/NW Safe", "Southeast/Northwest Safe", "Southeast Northwest Safe");


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

	private final ModifiableCallout<BuffApplied> redTether = new ModifiableCallout<BuffApplied>("Red Tether", "Red").autoIcon();
	private final ModifiableCallout<BuffApplied> blueTether = new ModifiableCallout<BuffApplied>("Blue Tether", "Blue").autoIcon();

	private final ModifiableCallout<AbilityUsedEvent> twister = new ModifiableCallout<>("Thordan II: Twister", "Twister");

	private final ModifiableCallout<BuffApplied> p6_spread = ModifiableCallout.<BuffApplied>durationBasedCall("Dragons: Spread", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> p6_stackWithDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Dragons: Stack Debuff", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> p6_stackWithoutDebuff = ModifiableCallout.durationBasedCall("Dragons: No Debuff", "Nothing");

	private final BooleanSetting p6_useAutoMarks;

	private final XivState state;
	private final StatusEffectRepository buffs;

	public Dragonsong(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
		this.state = state;
		this.buffs = buffs;
		p6_useAutoMarks = new BooleanSetting(pers, "triggers.dragonsong.use-auto-marks", false);
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
				switch (yourRole) {
					case METEOR_ON_YOU -> s.accept(thordan_trio2_meteorMark.getModified());
					case METEOR_ON_ROLE -> s.accept(thordan_trio2_meteorRoleMark.getModified());
					case NO_METEOR -> s.accept(thordan_trio2_nonMeteorRole.getModified());
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
		CalloutEvent firstCall = outFirst ? estinhog_gnash.getModified() : estinhog_lash.getModified();
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

	private CalloutEvent previousRedBlueCall;

	@HandleEvents
	public void doRedBlueTethers(EventContext ctx, BuffApplied ba) {
		if (ba.getTarget().isThePlayer()) {
			CalloutEvent call;
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

	private final ModifiableCallout<HaurchefauntHpTracker> haurch_hp = new ModifiableCallout<>("Haurchefaunt HP bar", "", "HP", HaurchefauntHpTracker::isExpired)
			.guiProvider(HaurchefauntHpTracker::getComponent);

	// TODO: figure out bug with this not disappearing
	private final class HaurchefauntHpTracker {
		private final XivCombatant haurchInitial;

		private HaurchefauntHpTracker(XivCombatant haurchInitial) {
			this.haurchInitial = haurchInitial;
		}

		public XivCombatant getNewData() {
			return getState().getLatestCombatantData(haurchInitial);
		}

		public boolean isExpired() {
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
				HaurchefauntHpTracker tracker = new HaurchefauntHpTracker(haurch);
				s.accept(haurch_hp.getModified(tracker));
				log.info("Haurch HP tracker end");
			});

	private final ModifiableCallout<BuffApplied> thordan2_trio1_lightningOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("Wrath of the Heavens: Lightning on you", "Lightning").autoIcon();

	private final ModifiableCallout<HeadMarkerEvent> thordan2_trio1_blueMark = new ModifiableCallout<>("Wrath of the Heavens: Blue Marker", "Blue Marker");
	private final ModifiableCallout<TetherEvent> thordan2_trio1_tether = new ModifiableCallout<>("Wrath of the Heavens: Tether", "Tether");
	private final ModifiableCallout<?> thordan2_trio1_neither = new ModifiableCallout<>("Wrath of the Heavens: Nothing", "Nothing");
	private final ModifiableCallout<HeadMarkerEvent> thordan2_trio1_greenMark = new ModifiableCallout<>("Wrath of the Heavens: Blue Marker", "Green Marker");

	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_protean = ModifiableCallout.durationBasedCall("Wrath of the Heavens: Protean", "Spread");

	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_in = ModifiableCallout.durationBasedCall("Wrath of the Heavens: In", "In");
	private final ModifiableCallout<AbilityCastStart> thordan2_trio1_inLightning = ModifiableCallout.durationBasedCall("Wrath of the Heavens: In with Lightning", "In with Lightning");

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


	private final ModifiableCallout<AbilityUsedEvent> thordan2_liquidHeavenOnYou = new ModifiableCallout<>("Liquid Heaven on you", "Liquid Heaven", "Liquid Heaven #{num}/5", ModifiableCallout.expiresIn(5));

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

	private final ModifiableCallout<BuffApplied> doom = ModifiableCallout.<BuffApplied>durationBasedCall("Thordan II: Doom", "Doom").autoIcon();
	private final ModifiableCallout<?> noDoom = new ModifiableCallout<>("Thordan II: Non-Doom", "Puddle");
	private final ModifiableCallout<HeadMarkerEvent> t2_circleDoom = new ModifiableCallout<>("Circle (Doom)", "Red Circle (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_triangleNoDoom = new ModifiableCallout<>("Triangle (No Doom)", "Green Triangle");
	private final ModifiableCallout<HeadMarkerEvent> t2_triangleDoom = new ModifiableCallout<>("Triangle (Doom)", "Green Triangle (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_squareNoDoom = new ModifiableCallout<>("Square (No Doom)", "Purple Square");
	private final ModifiableCallout<HeadMarkerEvent> t2_squareDoom = new ModifiableCallout<>("Square (Doom)", "Purple Square (Doom)");
	private final ModifiableCallout<HeadMarkerEvent> t2_crossNoDoom = new ModifiableCallout<>("Cross (No Doom)", "Blue Cross");

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

					List<XivPlayerCharacter> spreaders = playerMechs.get(WrothFlamesRole.SPREAD);

					List<XivPlayerCharacter> stackers = playerMechs.get(WrothFlamesRole.STACK);
					List<XivPlayerCharacter> otherStackers = playerMechs.get(WrothFlamesRole.NOTHING);

					// Give out markers
					spreaders.forEach(player -> s.accept(new SpecificAutoMarkRequest(player, MarkerSign.ATTACK_NEXT)));

					// People might be dead, so check count
					if (stackers.size() >= 1 && otherStackers.size() >= 1) {
						s.accept(new SpecificAutoMarkRequest(stackers.get(0), MarkerSign.BIND1));
						s.accept(new SpecificAutoMarkRequest(otherStackers.get(0), MarkerSign.BIND2));
					}
					if (stackers.size() >= 2 && otherStackers.size() >= 2) {
						s.accept(new SpecificAutoMarkRequest(stackers.get(1), MarkerSign.IGNORE1));
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

	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_stack = ModifiableCallout.durationBasedCall("P6 Tankbuster (Stack)", "Stack for Buster");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_niddBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Nidhogg Buster)", "Nidd Buster, Hraes Cleave");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_hraesBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Hraes Buster)", "Hraes Buster, Nidd Cleave");
	private final ModifiableCallout<AbilityCastStart> p6_tankbuster_bothBuster = ModifiableCallout.durationBasedCall("P6 Tankbuster (Both Buster)", "Busters");

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

	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_leftIn = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Left, near Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_rightIn = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Right, near Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_leftOut = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Left, away from Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> hallowedWingsAndPlume_rightOut = ModifiableCallout.durationBasedCall("Hallowed Wings and Plume", "Right, away from Hraesvelgr");
	private final ModifiableCallout<AbilityCastStart> akhAfah = ModifiableCallout.durationBasedCall("Akh Afah", "Light Party Stacks");
	private final ModifiableCallout<AbilityCastStart> akhAfahHpCheckCall = new ModifiableCallout<AbilityCastStart>("Akh Afah HP Check", "", "HP Check: {hpcheck}", ModifiableCallout.durationExpiry());


	private final ModifiableCallout<BuffApplied> hotDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Boiling", "Get hit by Hraesvelgr").autoIcon();
	private final ModifiableCallout<BuffApplied> coldDebuff = ModifiableCallout.<BuffApplied>durationBasedCall("Freezing", "Get hit by Nidhogg").autoIcon();
	private final ModifiableCallout<BuffApplied> pyretic_pre = ModifiableCallout.<BuffApplied>durationBasedCall("Pyretic (Pre-Call)", "Stop Moving Soon").statusIcon(0x3C0);
	private final ModifiableCallout<BuffApplied> pyretic = new ModifiableCallout<BuffApplied>("Pyretic", "Stop Moving", "Stop Moving", buff -> !getBuffs().originalStatusActive(buff)).statusIcon(0x3C0);

	@HandleEvents
	public void p6casts(EventContext context, AbilityCastStart event) {
		// Hraesvelgr
		if (event.getSource().getbNpcId() == 12613) {
			final ModifiableCallout<AbilityCastStart> call;
			boolean playerIsTank = getState().playerJobMatches(Job::isTank);
			switch ((int) event.getAbility().getId()) {
				case 0x6D23 -> call = playerIsTank ? hallowedWingsAndPlume_leftIn : hallowedWingsAndPlume_leftOut;
				case 0x6D24 -> call = playerIsTank ? hallowedWingsAndPlume_leftOut : hallowedWingsAndPlume_leftIn;
				case 0x6D26 -> call = playerIsTank ? hallowedWingsAndPlume_rightIn : hallowedWingsAndPlume_rightOut;
				case 0x6D27 -> call = playerIsTank ? hallowedWingsAndPlume_rightOut : hallowedWingsAndPlume_rightIn;
				case 0x6D41 -> call = akhAfah;
				default -> {
					return;
				}
			}
			context.accept(call.getModified(event));
		}
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
					if (diff > 0.02) {
						return "Attack Hraesvelgr";
					}
					else if (diff < -0.02) {
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
					s.waitMs(e1.getEstimatedRemainingDuration().toMillis() - 1_500);
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

//	@HandleEvents
//	public void p6_pyretic(EventContext context, BuffApplied event) {
//		if (event.getTarget().isThePlayer() && event.getBuff().getId() == 0xB52) {
//			context.accept(pyretic.getModified(event));
//		}
//	}


	public BooleanSetting getP6_useAutoMarks() {
		return p6_useAutoMarks;
	}

	private XivState getState() {
		return state;
	}
}
