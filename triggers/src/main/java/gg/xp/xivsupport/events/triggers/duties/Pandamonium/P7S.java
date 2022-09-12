package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@CalloutRepo(name = "P7S", duty = KnownDuty.P7S)
public class P7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P7S.class);
	// Generic repetitive calls
	private final ModifiableCallout<AbilityCastStart> boughOfAttisClose = ModifiableCallout.durationBasedCall("Bough of Attis Attack Close", "Go Far");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisFar = ModifiableCallout.durationBasedCall("Bough of Attis Attack Far", "Get Close");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisLeft = ModifiableCallout.durationBasedCall("Bough of Attis Attack Left", "Go Right");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisRight = ModifiableCallout.durationBasedCall("Bough of Attis Attack Right", "Go Left");
	private final ModifiableCallout<AbilityCastStart> dispersedAeroII = ModifiableCallout.durationBasedCall("Dispersed Aero II", "Tank Spread");
	private final ModifiableCallout<AbilityCastStart> condensedAeroII = ModifiableCallout.durationBasedCall("Condensed Aero II", "Tank Stack");
	private final ModifiableCallout<AbilityCastStart> sparkOfLife = ModifiableCallout.durationBasedCall("Spark of Life", "Raidwide with Bleed"); //bleed
	private final ModifiableCallout<AbilityCastStart> bladesOfAttis = ModifiableCallout.durationBasedCall("Blades of Attis", "Exaflare");
	private final ModifiableCallout<AbilityCastStart> lightOfLife = ModifiableCallout.durationBasedCall("Light of Life", "Big Raidwide");

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit1 = new ModifiableCallout<>("Forbidden Fruit 1", "Light Parties in Safe Spots");

	private final ModifiableCallout<BuffApplied> firstSet_stackSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Inviolate Bonds: Stack then Spread", "Stack then Spread").statusIcon(3309);
	private final ModifiableCallout<BuffApplied> firstSet_spreadStack = ModifiableCallout.<BuffApplied>durationBasedCall("Inviolate Bonds: Spread then Stack", "Spread then Stack").statusIcon(3308);
	private final ModifiableCallout<BuffApplied> firstSet_spread = ModifiableCallout.<BuffApplied>durationBasedCall("Inviolate Bonds: Spread (after stack)", "Spread in Safe Spot").statusIcon(3397);
	private final ModifiableCallout<BuffApplied> firstSet_stack = ModifiableCallout.<BuffApplied>durationBasedCall("Inviolate Bonds: Stack (after spread)", "Stack in Safe Spot").statusIcon(3398);

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit2 = new ModifiableCallout<>("Forbidden Fruit 2", "Spread and Knockback");

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit3 = new ModifiableCallout<>("Forbidden Fruit 3", "Light Parties on Platforms");
	private final ModifiableCallout<AbilityCastStart> moveIn = new ModifiableCallout<>("Move In (Healer Stacks + Floor Returns)", "Move In");

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit4 = new ModifiableCallout<>("Forbidden Fruit 4", "Tethers and Baits");
	private final ModifiableCallout<TetherEvent> firstTetherMino = new ModifiableCallout<>("First Tethers: Mino", "Minotaur Tether");
	private final ModifiableCallout<TetherEvent> firstTetherLightning = new ModifiableCallout<>("First Tethers: Lightning", "Lightning Tether");
	private final ModifiableCallout<TetherEvent> firstTetherNothing = new ModifiableCallout<>("First Tethers: Nothing", "Bait Cleave");

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit5 = new ModifiableCallout<>("Forbidden Fruit 5", "Tethers and Towers");
	private final ModifiableCallout<TetherEvent> forbiddenFruit5tether = new ModifiableCallout<>("Forbidden Fruit 5 - Tether", "Tethers");
	private final ModifiableCallout<TetherEvent> forbiddenFruit5noTether = new ModifiableCallout<>("Forbidden Fruit 5 - No Tether", "Tower");
	private final ModifiableCallout<TetherEvent> forbiddenFruit5kb = new ModifiableCallout<>("Forbidden Fruit 5 - KB", "Knockback");

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit6 = new ModifiableCallout<>("Forbidden Fruit 6", "Stacks and Spreads");
	private final ModifiableCallout<BuffApplied> secondAero = new ModifiableCallout<BuffApplied>("Inviolate Purgation", "{nextMechanic}", "{nextMechanic} ({event.estimatedRemainingDuration}) {remainingMechanics}", ModifiableCallout.expiresIn(55)).autoIcon();

	private final ModifiableCallout<AbilityCastStart> forbiddenFruit7 = new ModifiableCallout<>("Forbidden Fruit 7", "Bait Then Run");

//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroIV = ModifiableCallout.durationBasedCall("Hemitheos's Aero IV", "Knockback");

	/*
		3308 is short spread
		3397 is the long-timer spread
		3309 is short stack
		3398 is the long stack

	 */


	private final ModifiableCallout<AbilityCastStart> famineHarvest = ModifiableCallout.durationBasedCall("Famine's Harvest", "Famine");
	private final ModifiableCallout<?> famineTether = new ModifiableCallout<>("Famine's Harvest - Tether", "Take Tether {safe}", "Tethered to {tetherLocation} - go {safe}", ModifiableCallout.expiresIn(12));
	private final ModifiableCallout<?> famineNoTether = new ModifiableCallout<>("Famine's Harvest - No Tether", "No Tether - bait {lightningBait}", 12_000);
	private final ModifiableCallout<AbilityCastStart> deathHarvest = ModifiableCallout.durationBasedCall("Death's Harvest", "Death");
	private final ModifiableCallout<?> deathHarvestSpots = new ModifiableCallout<>("Death's Harvest Spots", "{bad} unsafe, {clockwise ? \"Clockwise\" : \"Counterclockwise\"}");
	private final ModifiableCallout<AbilityCastStart> warHarvest = ModifiableCallout.durationBasedCall("War's Harvest", "War");
	private final ModifiableCallout<TetherEvent> warHarvestIoTether = new ModifiableCallout<>("War's Harvest - Io Tether", "Io Tether from {where}", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestMinoTether = new ModifiableCallout<>("War's Harvest - Mino Tether (Unknown)", "Mino Tether", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestMinoTetherFar = new ModifiableCallout<>("War's Harvest - Mino (Far From Lighting)", "{where} Mino Tether", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestMinoTetherAdj = new ModifiableCallout<>("War's Harvest - Mino (Adjacent to Lightning)", "{where} Mino Tether", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestBirdTether = new ModifiableCallout<>("War's Harvest - Bird Tether (Unknown)", "Bird Tether", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestBirdTetherEmpty = new ModifiableCallout<>("War's Harvest - Bird (Near Empty Bridge)", "{where} Bird Tether", 10_000);
	private final ModifiableCallout<TetherEvent> warHarvestBirdTetherOccupied = new ModifiableCallout<>("War's Harvest - Bird (Both Bridges Occupied)", "{where} Bird Tether", 10_000);

	private final ModifiableCallout<AbilityCastStart> lightOfLifeEnrage = ModifiableCallout.durationBasedCall("Light of Life (Enrage)", "Enrage");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 5, 5);
	private final ArenaPos arenaPosFamine = new ArenaPos(100, 100, 7, 1);
	private final ArenaPos arenaPosTight = new ArenaPos(100, 100, 1, 1);

	public P7S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private final XivState state;

	private XivState getState() {
		return this.state;
	}

	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P7S);
	}


	// TODO: first tether mechanic
	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x7826 -> call = boughOfAttisFar;
			case 0x7821 -> call = boughOfAttisClose;
			case 0x7824 -> call = boughOfAttisRight;
			case 0x7823 -> call = boughOfAttisLeft;
			case 0x7835 -> call = dispersedAeroII;
			case 0x7836 -> call = condensedAeroII;
			case 0x7839 -> call = sparkOfLife;
			case 0x782E -> call = bladesOfAttis;
			case 0x783F -> call = lightOfLifeEnrage;
			case 31311 -> call = famineHarvest; // 7A4F
			case 31312 -> call = deathHarvest; // 7A50
			case 31313 -> call = warHarvest; // 7A51
			// TODO: Knockback
			default -> {
				return;
			}
		}
		// 782F is the exaflare fake actor cast, can be used to see pattern
//		if (id == 0x0)
//			call = boughOfAttisClose;
//		else if (id == 0x0) //????+1 fake
//			call = boughOfAttisFar;
//		else if (id == 0x0 && event.getSource().getPos().x() < 100) //????-1 boss
//			call = boughOfAttisLeft;
//		else if (id == 0x0 && event.getSource().getPos().x() > 100) //????-1 boss
//			call = boughOfAttisRight;
//		else if (id == 0x70) //fake x 8 = ????+1 ~1.3 sec after ???? finishes
//			call = hemitheosHoly;
//		else if (id == 0x0) //fake ????+1 ~0.7 after ???? finishes
//			call = hemitheosGlareIII;
//		else if (id == 0x0) //???+1 fake, longer cast. deals damage
//			call = immortalsObol;
//		else if (id == 0x0) //????+1 fake cast x 2 (1 each target)
//			call = hemitheosAeroII;
//		else if (id == 0x0)
//			call = sparkOfLife;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //io out, ????-2 and ????-1 casted to summon eggs
//			call = staticMoon;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //stymphalide dive, ????-2 and ????-1 casted to summon eggs
//			call = stymphalianStrike;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //????-1 real, but instant
//			call = bladesOfAttis;
//		else if (id == 0x0) //????+1 fake, has location
//			call = hemitheosAeroIV;
//		else
//			return;
//
		context.accept(call.getModified(event));
	}

	// TODO: needs to cancel on wipe
	@AutoFeed
	private final SequentialTrigger<BaseEvent> lightOfLifeSq = SqtTemplates.callWhenDurationIs(
			AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x78E2),
			lightOfLife,
			Duration.ofSeconds(5)
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> firstAeroSet = SqtTemplates.sq(25_000,
			BuffApplied.class,
			e1 -> e1.buffIdMatches(3308, 3397) && e1.getTarget().isThePlayer(),
			(initialBuff, s) -> {
				log.info("First Aero: Start");
				boolean stackFirst = initialBuff.getBuff().getId() == 3397;
				if (stackFirst) {
					s.updateCall(firstSet_stackSpread.getModified(initialBuff));
				}
				else {
					s.updateCall(firstSet_spreadStack.getModified(initialBuff));
				}
				log.info("First Aero: Waiting");
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(3309));
				// Find the long stack and use that as timing basis
				BuffApplied followUp = findAnyBuffWithId(stackFirst ? 3397 : 3398);
				if (stackFirst) {
					// Spread
					s.updateCall(firstSet_spread.getModified(followUp));
				}
				else {
					// Stack
					s.updateCall(firstSet_stack.getModified(followUp));
				}
				log.info("First Aero: Done");
			});

	private BuffApplied findAnyBuffWithId(long id) {
		return buffs.getBuffs().stream()
				.filter(ba -> ba.buffIdMatches(id))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Could not find an existing buff with ID " + id));
	}

	private BuffApplied findAnyBuffWithId(long id, BuffApplied dflt) {
		return buffs.getBuffs().stream()
				.filter(ba -> ba.buffIdMatches(id))
				.findAny()
				.orElse(dflt);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> secondAeroSet = SqtTemplates.sq(70_000,
			BuffApplied.class, e1 -> e1.buffIdMatches(3310),
			(startBuff, s) -> {
				log.info("Second Aero: Start");
				// probably don't need this because we wait for stack 4 which is the last buff
				s.waitMs(200);
				List<BuffApplied> buffs = getBuffs().statusesOnTarget(getState().getPlayer());
				long[] spreadIds = {3310, 3391, 3392, 3393};
				long[] stackIds = {3311, 3394, 3395, 3396};
				boolean[] isSpread = new boolean[4];
				for (BuffApplied buff : buffs) {
					for (int i = 0; i < spreadIds.length; i++) {
						if (buff.getBuff().getId() == spreadIds[i]) {
							isSpread[i] = true;
						}
					}
				}
				List<String> remainingMechanics = new ArrayList<>(4);
				for (boolean spread : isSpread) {
					if (spread) {
						remainingMechanics.add("Spread");
					}
					else {
						remainingMechanics.add("Stack");
					}
				}
				for (int i = 0; i < 4; i++) {
					// TODO: throw the raidwide call in here so that it calls it before the next stack/spread
					boolean nextIsSpread = isSpread[i];
					BuffApplied calloutBuff = findAnyBuffWithId(nextIsSpread ? spreadIds[i] : stackIds[i], startBuff);
					s.updateCall(secondAero.getModified(calloutBuff, Map.of("remainingMechanics", remainingMechanics.subList(i + 1, 4), "nextMechanic", nextIsSpread ? "Spread" : "Stack")));
					long spreadId = spreadIds[i];
					s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(spreadId));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> moveInSq = SqtTemplates.sq(10_000,
			AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x783B),
			(e1, s) -> {
				// This is the floor popping back up
				// Example filter to search in UI:
				// (event instanceof ChatLineEvent && event.line.contains("A foothold")) || (event instanceof ActorControlEvent) || (!event.source.isPc())
				log.info("moveInSq: Start");
				List<ActorControlEvent> aces = s.waitEventsUntil(1,
						ActorControlEvent.class,
						ace -> {
							log.info("moveInsq: {} {}", ace.getCommand(), ace.getData0());
							return ace.getCommand() == 0x8000000dL && ace.getData0() == 0x7fc000L;
						},
						BaseEvent.class, unused -> e1.getEffectiveTimeSince().toMillis() > 5000);
				log.info("moveInSq: result {}", aces.size());
				if (!aces.isEmpty()) {
					s.updateCall(moveIn.getModified(e1));
				}
			});

	private static Set<ArenaSector> platforms() {
		return EnumSet.of(ArenaSector.SOUTH, ArenaSector.NORTHWEST, ArenaSector.NORTHEAST);
	}

	// TODO: what is the best initial trigger for this?
	@AutoFeed
	private final SequentialTrigger<BaseEvent> forbiddenFruitWithTethers = SqtTemplates.multiInvocation(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7811),
			// TODO: First three - mostly covered by other triggers already, but might be worth adding just in case
			(e1, s) -> {
				// Healer stacks
				s.updateCall(forbiddenFruit1.getModified(e1));
			},
			(e1, s) -> {
				// Knockback
				s.updateCall(forbiddenFruit2.getModified(e1));
			},
			(e1, s) -> {
				// Separate Light parties
				s.updateCall(forbiddenFruit3.getModified(e1));
			},
			(e1, s) -> {
				// Tethers and baits
				s.updateCall(forbiddenFruit4.getModified(e1));
				EventCollector<TetherEvent> mino = new EventCollector<>(te -> te.eitherTargetMatches(cbt -> cbt.getbNpcId() == 14899));
				EventCollector<TetherEvent> lightning = new EventCollector<>(te -> true);
				s.collectEvents(6, 30_000, TetherEvent.class, true, List.of(mino, lightning));
				mino.findAny(te -> te.eitherTargetMatches(XivCombatant::isThePlayer))
						.ifPresentOrElse(
								minoTether -> s.updateCall(firstTetherMino.getModified(minoTether)),
								() -> lightning.findAny(te -> te.eitherTargetMatches(XivCombatant::isThePlayer))
										.ifPresentOrElse(
												lightningTether -> s.updateCall(firstTetherLightning.getModified(lightningTether)),
												() -> s.updateCall(firstTetherNothing.getModified())
										));
			},
			(e1, s) -> {
				// Tethers/towers
				s.updateCall(forbiddenFruit5.getModified(e1));
				List<TetherEvent> tethers = s.waitEvents(4, TetherEvent.class, te -> te.eitherTargetMatches(cbt -> cbt.getbNpcId() == 14898));
				tethers.stream()
						.filter(tether -> tether.eitherTargetMatches(XivCombatant::isThePlayer))
						.findAny()
						.ifPresentOrElse(
								tether -> s.updateCall(forbiddenFruit5tether.getModified(tether)),
								() -> s.updateCall(forbiddenFruit5noTether.getModified()));
				s.waitMs(4000);
				s.updateCall(forbiddenFruit5kb.getModified());
			},
			(e1, s) -> {
				// Inviolate Purgation (four stack/spread)
				s.updateCall(forbiddenFruit6.getModified());
			},
			(e1, s) -> {
				// Bait then run to other platform
				s.updateCall(forbiddenFruit7.getModified());
			}
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> famineSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(31311),
			(e1, s) -> {
				log.info("Famine: Start");
				List<TetherEvent> tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> true, Duration.ofMillis(200));
				log.info("Famine: Got Tethers: {}", tethers);
				s.waitThenRefreshCombatants(100);
				ArenaSector lightningBaitSpot;
				{
					Set<ArenaSector> platforms = platforms();
					tethers.stream().map(t -> t.getTargetMatching(cbt -> cbt.getbNpcId() == 14899))
							.filter(Objects::nonNull)
							.map(cbt -> getState().getLatestCombatantData(cbt))
							.peek(cbt -> log.info("Got combatant: {}", cbt))
							.map(arenaPosFamine::forCombatant)
							.peek(pos -> log.info("Resolved position: {}", pos))
							.forEach(platforms::remove);
					log.info("Famine: Raw platforms: {}", platforms);
					lightningBaitSpot = platforms.size() == 1 ? platforms.iterator().next() : ArenaSector.UNKNOWN;
				}
				log.info("Famine: Lightning Bait on {}", lightningBaitSpot);
				tethers.stream().filter(t -> t.eitherTargetMatches(XivCombatant::isThePlayer))
						.findAny()
						.ifPresentOrElse(tether -> {
							ArenaSector tetheredTo;
							ArenaSector tetherBaitSpot;
							XivCombatant tetheredToCbt = tether.getTargetMatching(cbt -> !cbt.isPc());
							if (tetheredToCbt == null) {
								log.warn("Combatant was null!");
								tetheredTo = ArenaSector.UNKNOWN;
								tetherBaitSpot = ArenaSector.UNKNOWN;
							}
							else {
								tetheredTo = arenaPos.forCombatant(tetheredToCbt);
								Set<ArenaSector> platforms = platforms();
								platforms.remove(lightningBaitSpot);
								platforms.remove(tetheredTo);
								tetherBaitSpot = platforms.iterator().next();
							}
							s.updateCall(famineTether.getModified(Map.of("lightningBait", lightningBaitSpot, "tetherLocation", tetheredTo, "safe", tetherBaitSpot)));
						}, () -> s.updateCall(famineNoTether.getModified(Map.of("lightningBait", lightningBaitSpot))));
			});

	private static final Position sPlatform = Position.of2d(100, 116.5);
	private static final Position nwPlatform = Position.of2d(85.71, 91.75);
	private static final Position nePlatform = Position.of2d(114.29, 91.75);
	private static final Position nBridge = Position.of2d(100, 91.75);
	private static final Position swBridge = Position.of2d(92.86, 104.13);
	private static final Position seBridge = Position.of2d(107.14, 104.13);


	@AutoFeed
	private final SequentialTrigger<BaseEvent> deathSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(31312),
			(e1, s) -> {
				// TODO: Death: Is there anything that can be done here? Tethers are all the same tether ID, and the NPCs aren't even in the combatants list
				// For calling CW/CCW:
				// 1. Unsafe platform is highest ID Immature Io (id/name 14897/11378)
				// 2. Tether IOs are lowest and middle ID
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7811));
				s.waitMs(200);
				// This extra position matching stuff is purely because we're pulling all combatants, and there might
				// be junk.
				List<Position> bridges = List.of(nBridge, swBridge, seBridge);
				List<Position> platforms = List.of(sPlatform, nwPlatform, nePlatform);
				Position takenPlatform;
				List<Position> takenBridges;
				List<XivCombatant> ios;
				do {
					do {
						s.waitThenRefreshCombatants(100);
						ios = getState().getCombatantsListCopy()
								.stream()
								.filter(cbt -> cbt.getbNpcId() == 14897)
								.toList();
					} while (ios.size() != 3);
					List<XivCombatant> tmpIos = ios;
					log.info("Death: Ios: {}", ios);
					takenPlatform = platforms.stream().filter(platform -> tmpIos.stream().anyMatch(cbt -> platform.distanceFrom2D(cbt.getPos()) < 1)).findAny().orElse(null);
					takenBridges = bridges.stream().filter(bridge -> tmpIos.stream().anyMatch(cbt -> bridge.distanceFrom2D(cbt.getPos()) < 1)).toList();
					log.info("Death: Taken Platform: {}, Taken Bridges: {}", takenPlatform, takenBridges);
				} while (takenPlatform == null || takenBridges.size() != 2);
				boolean clockwise;
				ArenaSector bad;
				if (takenPlatform == sPlatform) {
					clockwise = takenBridges.contains(swBridge);
					bad = ArenaSector.SOUTH;
				}
				else if (takenPlatform == nwPlatform) {
					clockwise = takenBridges.contains(nBridge);
					bad = ArenaSector.NORTHWEST;
				}
				else if (takenPlatform == nePlatform) {
					clockwise = takenBridges.contains(seBridge);
					bad = ArenaSector.NORTHEAST;
				}
				else {
					throw new RuntimeException("Bad platform! Ios: " + ios);
				}
				s.updateCall(deathHarvestSpots.getModified(Map.of("clockwise", clockwise, "bad", bad)));
			});


	@AutoFeed
	private final SequentialTrigger<BaseEvent> warSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(31313),
			(e1, s) -> {
				log.info("War: Start");
				long stymph = 14898;
				long mino = 14899;
				List<TetherEvent> allTethers = s.waitEventsQuickSuccession(8, TetherEvent.class, te -> true, Duration.ofMillis(400));
				TetherEvent te = allTethers.stream().filter(tether -> tether.eitherTargetMatches(XivCombatant::isThePlayer)).findAny().orElse(null);
				Set<ArenaSector> stymphs = EnumSet.noneOf(ArenaSector.class);
				Set<ArenaSector> minos = EnumSet.noneOf(ArenaSector.class);
				s.waitThenRefreshCombatants(50);
				allTethers.stream()
						.flatMap(tether -> Stream.of(tether.getSource(), tether.getTarget()))
						.filter(cbt -> cbt.getbNpcId() == stymph || cbt.getbNpcId() == mino)
						.forEach(cbt -> {
							XivCombatant latest = getState().getLatestCombatantData(cbt);
							if (cbt.getbNpcId() == stymph) {
								log.warn("Stymph: {}", latest);
								stymphs.add(arenaPosTight.forCombatant(latest));
							}
							else if (cbt.getbNpcId() == mino) {
								log.warn("Mino: {}", latest);
								minos.add(arenaPosTight.forCombatant(latest));
							}
						});
				if (te == null) {
					log.warn("War: No tether, player probably dead");
					return;
				}
				// Possible options here:
				// Lightning Tether
				// Stymphalide, on platform adjacent to both occupied bridges
				// Stymphalide, on platform adjacent to empty bridge
				// Minotaur, on bridge far from lightning (adjacent to both stymphs)
				// Minotaur, on bridge close to lightning
				// Can't use tether IDs because they change with distance.
				if (te.getSource().getbNpcId() == mino) {
					// Minotaur, on bridge
					ArenaSector tetheredTo = arenaPosTight.forCombatant(getState().getLatestCombatantData(te.getSource()));
					Map<String, Object> params = Map.of("where", tetheredTo);
					List<ArenaSector> adjacent;
					switch (tetheredTo) {
						case NORTH -> adjacent = List.of(ArenaSector.NORTHWEST, ArenaSector.NORTHEAST);
						case SOUTHEAST -> adjacent = List.of(ArenaSector.SOUTH, ArenaSector.NORTHEAST);
						case SOUTHWEST -> adjacent = List.of(ArenaSector.NORTHWEST, ArenaSector.SOUTH);
						default -> {
							// Fallback option
							log.warn("War fail! Bad position for {}", tetheredTo);
							s.updateCall(warHarvestMinoTether.getModified(te));
							return;
						}
					}
					if (stymphs.size() != 2) {
						log.warn("War fail! Bad stymphs: {}", stymphs);
						s.updateCall(warHarvestMinoTether.getModified(te));
					}
					else if (stymphs.containsAll(adjacent)) {
						s.updateCall(warHarvestMinoTetherFar.getModified(te, params));
					}
					else {
						s.updateCall(warHarvestMinoTetherAdj.getModified(te, params));
					}
				}
				else if (te.getSource().getbNpcId() == stymph) {
					// Stymphalide, on platform
					ArenaSector tetheredTo = arenaPosTight.forCombatant(getState().getLatestCombatantData(te.getSource()));
					Map<String, Object> params = Map.of("where", tetheredTo);
					List<ArenaSector> adjacent;
					switch (tetheredTo) {
						case SOUTH -> adjacent = List.of(ArenaSector.SOUTHWEST, ArenaSector.SOUTHEAST);
						case NORTHWEST -> adjacent = List.of(ArenaSector.NORTH, ArenaSector.SOUTHWEST);
						case NORTHEAST -> adjacent = List.of(ArenaSector.NORTH, ArenaSector.SOUTHEAST);
						default -> {
							// Fallback option
							log.warn("War fail! Bad position for {}", tetheredTo);
							s.updateCall(warHarvestBirdTether.getModified(te));
							return;
						}
					}
					if (minos.size() != 2) {
						log.warn("War fail! Bad minos: {}", stymphs);
						s.updateCall(warHarvestBirdTether.getModified(te));
					}
					else if (stymphs.containsAll(adjacent)) {
						s.updateCall(warHarvestBirdTetherOccupied.getModified(te, params));
					}
					else {
						s.updateCall(warHarvestBirdTetherEmpty.getModified(te, params));
					}
				}
				// This one isn't a real combatant
				else {
					// Lightning
					Set<ArenaSector> platforms = platforms();
					ArenaSector tetherSpot;
					platforms.removeAll(stymphs);
					if (platforms.size() == 1) {
						tetherSpot = platforms.iterator().next();
					}
					else {
						tetherSpot = ArenaSector.UNKNOWN;
					}
					s.updateCall(warHarvestIoTether.getModified(te, Map.of("where", tetherSpot)));
				}
			});

	/*
		BUFFS
		3308 is short spread
		3397 is the long-timer spread
		3309 is short stack
		3398 is the long stack

		// 1 = 10s, 2 = 25s, 3 = 40s, 4 = 55s
		3310 is aero 1
		3391 is aero 2
		3392 is aero 3
		3393 is aero 4

		3311 is stack 1
		3394 is stack 2
		3395 is stack 3
		3396 is stack 4
	 */
}
