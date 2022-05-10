package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
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
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.events.state.RefreshSpecificCombatantsRequest;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@CalloutRepo("Dragonsong's Reprise")
public class Dragonsong implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(Dragonsong.class);

	private final ModifiableCallout<HeadMarkerEvent> p1_firstCleaveMarker = new ModifiableCallout<>("Quad Marker (1st set)", "Marker, First Set");
	private final ModifiableCallout<HeadMarkerEvent> p1_secondCleaveMarker = new ModifiableCallout<>("Quad Marker (2nd set)", "Second Set");
	private final ModifiableCallout<AbilityCastStart> p1_holiestOfHoly = ModifiableCallout.durationBasedCall("Holiest of Holy", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> p1_emptyDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Donut");
	private final ModifiableCallout<AbilityCastStart> p1_fullDimension = ModifiableCallout.durationBasedCall("Empty Dimension", "Out");
	private final ModifiableCallout<AbilityCastStart> p1_heavensblaze = ModifiableCallout.durationBasedCall("Heavensblaze", "Stack on {event.target}");
	private final ModifiableCallout<AbilityCastStart> p1_holiestHallowing = ModifiableCallout.durationBasedCall("Holiest Hallowing", "Interrupt {event.source}");
	private final ModifiableCallout<BuffApplied> p1_brightwing = ModifiableCallout.durationBasedCall("Brightwing", "Pair Cleaves");

//	private final ModifiableCallout<TetherEvent> p1_genericTether = new ModifiableCallout<>("P1 Generic Tethers", "Tether on you", "Tether on you {event.id}", Collections.emptyList());

	private final ModifiableCallout<BuffApplied> p1_puddleBait = ModifiableCallout.durationBasedCall("Puddle", "Puddle on you");

	private final ModifiableCallout<HeadMarkerEvent> circle = new ModifiableCallout<>("Circle", "Red Circle with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> triangle = new ModifiableCallout<>("Triangle", "Green Triangle with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> square = new ModifiableCallout<>("Square", "Purple Square with {partner}");
	private final ModifiableCallout<HeadMarkerEvent> cross = new ModifiableCallout<>("Cross", "Blue Cross with {partner}");

	private final ModifiableCallout<AbilityCastStart> thordan_cleaveBait = ModifiableCallout.durationBasedCall("Ascalon's Mercy", "Cleave Bait");
	private final ModifiableCallout<AbilityCastStart> thordan_quaga = ModifiableCallout.durationBasedCall("Ancient Quaga", "Raidwide");

	private final ModifiableCallout<?> nsSafe = new ModifiableCallout<>("Trio 1 N/S Safe", "North/South Safe", "North South Safe", Collections.emptyList());
	private final ModifiableCallout<?> neSwSafe = new ModifiableCallout<>("Trio 1 NE/SW Safe", "Northeast/Southwest Safe", "Northeast Southwest Safe", Collections.emptyList());
	private final ModifiableCallout<?> ewSafe = new ModifiableCallout<>("Trio 1 E/W Safe", "East/West Safe", "East West Safe", Collections.emptyList());
	private final ModifiableCallout<?> seNwSafe = new ModifiableCallout<>("Trio 1 SE/NW Safe", "Southeast/Northwest Safe", "Southeast Northwest Safe", Collections.emptyList());


	private final ModifiableCallout<?> thordan_trio1_nothing = new ModifiableCallout<>("First Trio: Nothing", "Nothing");
	private final ModifiableCallout<HeadMarkerEvent> thordan_trio1_blueMarker = new ModifiableCallout<>("First Trio: Blue Marker", "Blue Marker");
	private final ModifiableCallout<?> thordan_trio1_tank = new ModifiableCallout<>("First Trio: Tank", "Take Tether");
	private final ModifiableCallout<?> thordan_trio1_wheresThordan = new ModifiableCallout<>("First Trio: Where is Thordan", "Thordan {wheresThordan}");

	private final ModifiableCallout<?> thordan_trio2_swordMark = new ModifiableCallout<>("Second Trio: Swords", "{sword1} and {sword2}");

	private final ModifiableCallout<?> thordan_trio2_meteorMark = new ModifiableCallout<>("Second Trio: Meteors", "Meteor on you");
	private final ModifiableCallout<?> thordan_trio2_meteorRoleMark = new ModifiableCallout<>("Second Trio: Meteors", "Meteor role");
	private final ModifiableCallout<?> thordan_trio2_nonMeteorRole = new ModifiableCallout<>("Second Trio: Meteors", "Non-meteor role");

	private final ModifiableCallout<?> thordan_trio2_firstTower = new ModifiableCallout<>("Second Trio: Tower 1", "Soak First Tower");
	//	private final ModifiableCallout<?> thordan_trio2_secondTower = new ModifiableCallout<>("Second Trio: Tower 2", "Soak Second Tower");
	private final ModifiableCallout<?> thordan_trio2_kbImmune = new ModifiableCallout<>("Second Trio: Knockback Immune", "Knockback Immune in Tower");
	private final ModifiableCallout<?> thordan_trio2_getKnockedBack = new ModifiableCallout<>("Second Trio: Knockback Immune", "Take knockback into tower");
	private final ModifiableCallout<BaseEvent> meteorDrop = new ModifiableCallout<>("Drop Meteors", "Drop Meteors", "Drop Meteor #{num}", ModifiableCallout.expiresIn(Duration.ofSeconds(11)));

	private final ModifiableCallout<AbilityCastStart> thordan_broadSwingL = ModifiableCallout.durationBasedCall("Broad Swing Left", "Back then Left");
	private final ModifiableCallout<AbilityCastStart> thordan_broadSwingR = ModifiableCallout.durationBasedCall("Broad Swing Right", "Back then Right");

	private final ModifiableCallout<BuffApplied> estinhog_headmark1 = new ModifiableCallout<>("Estinhog: First in Line", "One");
	private final ModifiableCallout<BuffApplied> estinhog_headmark2 = new ModifiableCallout<>("Estinhog: Second in Line", "Two");
	private final ModifiableCallout<BuffApplied> estinhog_headmark3 = new ModifiableCallout<>("Estinhog: Third in Line", "Three");

	// TODO: put these back in
	private final ModifiableCallout<BuffApplied> estinhog_highJumpOnlyYou = ModifiableCallout.durationBasedCall("Estinhog: High Jump", "Middle");
	private final ModifiableCallout<BuffApplied> estinhog_highJumpAll = ModifiableCallout.durationBasedCall("Estinhog: High Jump", "Pick Spots");
	private final ModifiableCallout<BuffApplied> estinhog_spineshatter = ModifiableCallout.durationBasedCall("Estinhog: Spineshatter", "West and Face In");
	private final ModifiableCallout<BuffApplied> estinhog_elusiveJump = ModifiableCallout.durationBasedCall("Estinhog: Elusive Jump", "East and Face Out");


	private final ModifiableCallout<BuffRemoved> estinhog_baitGeir = new ModifiableCallout<>("Estinhog: Bait Geirskogul", "Bait Geirskogul");

//	private final ModifiableCallout<?> wyrmhole_number = new ModifiableCallout<>("Wyrmhole: Number Only", "Number {number}");

	private final ModifiableCallout<?> wyrmhole_place1 = new ModifiableCallout<>("Wyrmhole: Place #1", "Place Tower {where}, then {first} then {second}");
	private final ModifiableCallout<?> wyrmhole_soak1 = new ModifiableCallout<>("Wyrmhole: Soak #1", "Stack, {first}, {second}, then soak tower");
	private final ModifiableCallout<?> wyrmhole_nothing1 = new ModifiableCallout<>("Wyrmhole: Nothing #1", "Stack, {first}, {second}");

	private final ModifiableCallout<?> wyrmhole_place2 = new ModifiableCallout<>("Wyrmhole: Place #2", "Place Tower {where}");
	// Actually comes out at the same time as the #3 calls since it gives the whole sequence
	private final ModifiableCallout<?> wyrmhole_soak2 = new ModifiableCallout<>("Wyrmhole: Soak #2", "Soak then {first} then {second}");

	private final ModifiableCallout<?> wyrmhole_place3 = new ModifiableCallout<>("Wyrmhole: Place #3", "Place Tower {where}, then {first} then {second}");
	private final ModifiableCallout<?> wyrmhole_soak3_as1 = new ModifiableCallout<>("Wyrmhole: Soak #3 (As #1)", "Stack, {first}, {second}, then soak tower");
	private final ModifiableCallout<?> wyrmhole_soak3_as2 = new ModifiableCallout<>("Wyrmhole: Soak #3 (As #2)", "Stack, {first}, {second}, then soak tower");


	private final ModifiableCallout<?> estinhog_gnash = new ModifiableCallout<>("Estinhog: Gnash", "Out");
	private final ModifiableCallout<?> estinhog_lash = new ModifiableCallout<>("Estinhog: Lash", "In");


	private final ModifiableCallout<AbilityCastStart> estinhog_drachenlance = ModifiableCallout.durationBasedCall("Estinhog: Drachenlance", "Out of front");

	private final ModifiableCallout<BuffApplied> doom = ModifiableCallout.durationBasedCall("Doom", "Doom");

	private final XivState state;
	private final StatusEffectRepository buffs;

	public Dragonsong(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
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
			case 0x62DA -> call = p1_emptyDimension;
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
		else if (event.getTarget().isThePlayer()) {
			if (event.getBuff().getId() == 0xA65) {
				context.accept(p1_puddleBait.getModified(event));
			}
			else if (event.getBuff().getId() == 0xBA0) {
				context.accept(doom.getModified(event));
			}
		}
	}


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


//	@HandleEvents
//	public void p1_genericTether(EventContext context, TetherEvent event) {
//		long id = event.getId();
//		if (event.eitherTargetMatches(XivCombatant::isThePlayer)
//				&& (id == 0x54 || id == 0x1)) {
//			context.accept(p1_genericTether.getModified(event));
//		}
//	}

	@HandleEvents
	public void feedSeq(EventContext context, BaseEvent event) {
		p1_fourHeadMark.feed(context, event);
		p1_pairsOfMarkers.feed(context, event);
		thordan_firstTrio.feed(context, event);
		thordan_secondTrio.feed(context, event);
		thordan_iceFire.feed(context, event);
		meteorHelper.feed(context, event);
		wyrmhole.feed(context, event);
		if (event instanceof AbilityUsedEvent aue) {
			gnashLashHelper.feed(context, aue);
		}
	}

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

			});

	private enum IceFireRole {
		METEOR_ON_YOU,
		METEOR_ON_ROLE,
		NO_METEOR
	}

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

				// Second gnash/lash starts casting
				{
					GnashLash secondGnashLash = waitGnashLash(s);
					Map<String, Object> params = Map.of("where", whereDive, "first", secondGnashLash.first, "second", secondGnashLash.second);
					if (linePos == 1) {
						if (isMiddle) {
							s.accept(wyrmhole_soak3_as1.getModified(params));
						}
						else {
							s.accept(wyrmhole_soak2.getModified(params));
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

	private final SequentialTrigger<AbilityUsedEvent> gnashLashHelper = new SequentialTrigger<>(10_000, AbilityUsedEvent.class,
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
		s.accept(firstCall);
		s.waitEvent(AbilityUsedEvent.class, aue -> aue.isFirstTarget() && (aue.getAbility().getId() == 0x6715 || aue.getAbility().getId() == 0x6716));
		s.updateCall(!outFirst ? estinhog_gnash.getModified() : estinhog_lash.getModified());
	});

	@HandleEvents
	public void geirskogul(EventContext ctx, AbilityUsedEvent event) {
		// I **think** this doesn't come up later in the fight judging by a p6 log I perused
		long id = event.getAbility().getId();
		if ((id == 0x6711 || id == 0x6717 || id == 0x6718 || id == 0x6719 || id == 0x671B) && event.getTarget().isThePlayer()) {
			ctx.accept(estinhog_baitGeir.getModified());
		}
	}


//	@HandleEvents
//	public void genericHeadMarksOnYou(EventContext context, HeadMarkerEvent event) {
//		if (event.getTarget().isThePlayer()) {
//			ModifiableCallout<HeadMarkerEvent> call;
//			switch (getHeadmarkOffset(event)) {
//				case -11 -> call = estinhog_headmark1;
//				case -10 -> call = estinhog_headmark2;
//				case -9 -> call = estinhog_headmark3;
//				default -> {
//					return;
//				}
//			}
//			context.accept(call.getModified(event));
//		}
//	}

	private XivState getState() {
		return state;
	}
}
