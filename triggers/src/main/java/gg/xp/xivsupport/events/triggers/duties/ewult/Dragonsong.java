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
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

	private final ModifiableCallout<BuffApplied> estinhog_headmark1 = new ModifiableCallout<>("Estinhog: First in Line", "One");
	private final ModifiableCallout<BuffApplied> estinhog_headmark2 = new ModifiableCallout<>("Estinhog: Second in Line", "Two");
	private final ModifiableCallout<BuffApplied> estinhog_headmark3 = new ModifiableCallout<>("Estinhog: Third in Line", "Three");

	private final ModifiableCallout<BuffRemoved> estinhog_baitGeir = new ModifiableCallout<>("Estinhog: Bait Geirskogul", "Bait Geirskogul");

	private final ModifiableCallout<?> estinhog_placeSecond = new ModifiableCallout<>("Estinhog: Place Second Tower", "Place Second Tower");
	private final ModifiableCallout<?> estinhog_placeThird = new ModifiableCallout<>("Estinhog: Place Third Tower", "Place Third Tower");

	private final ModifiableCallout<BuffApplied> estinhog_highJump = ModifiableCallout.durationBasedCall("Estinhog: High Jump", "Tower on You");
	private final ModifiableCallout<BuffApplied> estinhog_elusiveJump = ModifiableCallout.durationBasedCall("Estinhog: Elusive Jump", "Tower behind you");
	private final ModifiableCallout<BuffApplied> estinhog_spineshatter = ModifiableCallout.durationBasedCall("Estinhog: Spineshatter", "Tower in front of you");

	private final ModifiableCallout<?> estinhog_soakFirst = ModifiableCallout.durationBasedCall("Estinhog: Soak First", "Soak First Tower");
	private final ModifiableCallout<?> estinhog_soakSecond = ModifiableCallout.durationBasedCall("Estinhog: Soak Second", "Soak Second Tower");
	private final ModifiableCallout<?> estinhog_soakThird_asSecond = ModifiableCallout.durationBasedCall("Estinhog: Spineshatter", "Soak Third Tower");
	private final ModifiableCallout<?> estinhog_soakThird_asFirst = ModifiableCallout.durationBasedCall("Estinhog: Spineshatter", "Soak Third Tower");



	private final ModifiableCallout<AbilityCastStart> estinhog_gnashAndLash = ModifiableCallout.durationBasedCall("Estinhog: Gnash and Lash", "Out then In");
	private final ModifiableCallout<AbilityCastStart> estinhog_lashAndGnash = ModifiableCallout.durationBasedCall("Estinhog: Lash and Gnash", "In then Out");

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
			case 0x6712 -> call = estinhog_gnashAndLash;
			case 0x6713 -> call = estinhog_lashAndGnash;
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
		else if (event.getBuff().getId() == 0xA65 && event.getTarget().isThePlayer()) {
			context.accept(p1_puddleBait.getModified(event));
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
		wyrmhole.feed(context, event);
	}

	private final SequentialTrigger<BaseEvent> p1_fourHeadMark = new SequentialTrigger<>(30_000, BaseEvent.class,
			e -> (e instanceof AbilityCastStart acs) && acs.getAbility().getId() == 0x62DD,
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

	private final ModifiableCallout<?> nsSafe = new ModifiableCallout<>("Trio 1 N/S Safe", "North/South Safe", "North South Safe", Collections.emptyList());
	private final ModifiableCallout<?> neSwSafe = new ModifiableCallout<>("Trio 1 NE/SW Safe", "Northeast/Southwest Safe", "Northeast Southwest Safe", Collections.emptyList());
	private final ModifiableCallout<?> ewSafe = new ModifiableCallout<>("Trio 1 E/W Safe", "East/West Safe", "East West Safe", Collections.emptyList());
	private final ModifiableCallout<?> seNwSafe = new ModifiableCallout<>("Trio 1 SE/NW Safe", "Southeast/Northwest Safe", "Southeast Northwest Safe", Collections.emptyList());

	private final SequentialTrigger<BaseEvent> thordan_firstTrio = new SequentialTrigger<>(30_000, BaseEvent.class,
			e -> e instanceof AbilityCastStart acs && acs.getAbility().getId() == 0x63D3,
			(e1, s) -> {
				log.info("Thordan Trio 1: Start");

				List<AbilityCastStart> dashes = s.waitEvents(3, AbilityCastStart.class, acs -> acs.getAbility().getId() == 0x63D4);
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.all);
				dashes.stream()
						.map(dash -> arenaPos.forCombatant(dash.getSource()))
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
						s.waitEvent(BaseEvent.class);
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
				List<HeadMarkerEvent> marks = s.waitEvents(2, HeadMarkerEvent.class, hm -> getHeadmarkOffset(hm) == -45);
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

	private final SequentialTrigger<BaseEvent> wyrmhole = new SequentialTrigger<>(60_000, BaseEvent.class,
			// Start on final chorus
			e -> e instanceof AbilityUsedEvent a && a.getAbility().getId() == 0x6709 && a.isFirstTarget(),
			(e1, s) -> {
				BuffApplied inLineBuffApplied = s.waitEvent(BuffApplied.class, ba -> {
					long id = ba.getBuff().getId();
					return ba.getTarget().isThePlayer() && id >= 0xBBC && id <= 0xBBE;
				});
				int linePos = (int) inLineBuffApplied.getBuff().getId() - 0xBBB;
				switch (linePos) {
					case 1 -> s.accept(estinhog_headmark1.getModified(inLineBuffApplied));
					case 2 -> s.accept(estinhog_headmark2.getModified(inLineBuffApplied));
					case 3 -> s.accept(estinhog_headmark3.getModified(inLineBuffApplied));
				}

				BuffApplied diveBuffApplied = s.waitEvent(BuffApplied.class, ba -> {
					long id = ba.getBuff().getId();
					return ba.getTarget().isThePlayer() && id >= 0xAC3 && id <= 0xAC5;
				});
				int diveBuffId = (int) diveBuffApplied.getBuff().getId();
				switch (diveBuffId) {
					case 0xAC3 -> s.accept(estinhog_highJump.getModified(diveBuffApplied));
					case 0xAC4 -> s.accept(estinhog_spineshatter.getModified(diveBuffApplied));
					case 0xAC5 -> s.accept(estinhog_elusiveJump.getModified(diveBuffApplied));
				}

				// First towers placed
				s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xBBC);

				if (linePos == 3) {
					s.accept(estinhog_soakFirst.getModified());
				}
				// 6711 is the damage from actually soaking
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x6711);
				if (linePos == 2) {
					s.accept(estinhog_placeSecond.getModified());
				}

				// Second towers placed
				s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xBBD);
				if (linePos == 1) {
					s.accept(estinhog_soakSecond.getModified());
				}
				// Second towers soaked
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getAbility().getId() == 0x6711);
				if (linePos == 3) {
					s.accept(estinhog_placeThird.getModified());
				}

				// Third towers placed
				s.waitEvent(BuffRemoved.class, br -> br.getBuff().getId() == 0xBBE);
				if (linePos == 2) {
					s.accept(estinhog_soakThird_asSecond.getModified());
				}
				// Whoever still has 'first in line' also needs to soak one of these, so check if the player still has it
				else if (linePos == 1 && getBuffs().statusesOnTarget(getState().getPlayer())
						.stream()
						.anyMatch(buff -> buff.getBuff().getId() == 0xBBC)) {
					s.accept(estinhog_soakThird_asFirst.getModified());
				}
			});

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@HandleEvents
	public void buffRemoved(EventContext ctx, BuffRemoved br) {
		if (br.getTarget().isThePlayer()) {
			long id = br.getBuff().getId();
			if (id >= 0xBBC && id <= 0xBBE) {
				ctx.accept(estinhog_baitGeir.getModified(br));
			}
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
