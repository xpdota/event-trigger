package gg.xp.xivsupport.triggers.dtex;

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
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@CalloutRepo(name = "EX1", duty = KnownDuty.DtEx1)
public class DTEx1 extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(DTEx1.class);

	private XivState state;
	private ActiveCastRepository acr;
	private StatusEffectRepository buffs;

	public DTEx1(XivState state, ActiveCastRepository acr, StatusEffectRepository buffs) {
		this.state = state;
		this.acr = acr;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.DtEx1);
	}


	@NpcCastCallout({0x8FD6, 0x8FD8, 0x8FDA})
	private final ModifiableCallout<AbilityCastStart> disasterZone = ModifiableCallout.durationBasedCall("Disaster Zone", "Raidwide");

	@NpcCastCallout(0x9008)
	private final ModifiableCallout<AbilityCastStart> tulidisaster = ModifiableCallout.durationBasedCall("Tulidisaster", "Multiple Raidwides");

	@NpcCastCallout(0x95C3)
	private final ModifiableCallout<AbilityCastStart> skyruinLightning = ModifiableCallout.durationBasedCallWithOffset("Skyruin (Lightning)", "Raidwide with Bleed", Duration.ofMillis(5_300));

	@NpcCastCallout(0x95C4)
	private final ModifiableCallout<AbilityCastStart> skyruinFire = ModifiableCallout.durationBasedCallWithOffset("Skyruin (Fire)", "Raidwide with Bleed", Duration.ofMillis(5_300));

	@NpcCastCallout(0x8FD1)
	private final ModifiableCallout<AbilityCastStart> skyruinIce = ModifiableCallout.durationBasedCallWithOffset("Skyruin (Ice)", "Raidwide with Bleed", Duration.ofMillis(5_300));

	private static final Duration buddiesOffset = Duration.ofMillis(800);

	@NpcCastCallout(0x8FC7)
	private final ModifiableCallout<AbilityCastStart> coneAndBuddies = ModifiableCallout.durationBasedCallWithOffset("Cone+Buddies", "Front Corners and Partners", buddiesOffset);

	@NpcCastCallout(0x8FCB)
	private final ModifiableCallout<AbilityCastStart> outAndBuddies = ModifiableCallout.durationBasedCallWithOffset("Out+Buddies", "Out and Partners", buddiesOffset);

	@NpcCastCallout(0x8FCF)
	private final ModifiableCallout<AbilityCastStart> middleAndBuddies = ModifiableCallout.durationBasedCallWithOffset("In Middle+Buddies", "Middle and Partners", buddiesOffset);

	@NpcCastCallout(0x8FC5)
	private final ModifiableCallout<AbilityCastStart> coneAndEruption = ModifiableCallout.durationBasedCall("Cone+Twister", "Front Corners and Bait Twister");

	@NpcCastCallout(0x8FC9)
	private final ModifiableCallout<AbilityCastStart> outAndEruption = ModifiableCallout.durationBasedCall("Out+Twister", "Out and Bait Twister");

	@NpcCastCallout(0x8FCD)
	private final ModifiableCallout<AbilityCastStart> middleAndEruption = ModifiableCallout.durationBasedCall("In Middle+Twister", "Middle and Bait Twister");

	@NpcCastCallout(0x8FE7)
	private final ModifiableCallout<AbilityCastStart> trisource = ModifiableCallout.durationBasedCall("Triscourge", "Raidwide");

	@NpcAbilityUsedCallout(value = 0x8FEF, suppressMs = 200)
	private final ModifiableCallout<AbilityUsedEvent> eruptionMove = new ModifiableCallout<>("Eruption: Move", "Move");

	private final ModifiableCallout<BuffApplied> lightning = ModifiableCallout.durationBasedCall("Lightning Soon", "Spread Soon");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> lightningDebuffHandler = SqtTemplates.callWhenDurationIs(
			BuffApplied.class, ba -> ba.buffIdMatches(0xEEF) && ba.getTarget().isThePlayer(), lightning, Duration.ofSeconds(8));

	private final ModifiableCallout<BuffApplied> freezingSoon = ModifiableCallout.<BuffApplied>durationBasedCall("Freeze Soon", "Move Soon").autoIcon();
	private final ModifiableCallout<BuffApplied> freezingVerySoon = ModifiableCallout.<BuffApplied>durationBasedCall("Freeze Very Soon", "Move!").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> freezingHandler = SqtTemplates.sq(130_000,
			BuffApplied.class, ba -> ba.buffIdMatches(0xEEE) && ba.getTarget().isThePlayer(),
			(e1, s) -> {
				log.info("freezingHandler 1");
				s.waitDuration(e1.remainingDurationPlus(Duration.ofSeconds(-5)));
				log.info("freezingHandler 2");
				s.updateCall(freezingSoon, e1);
				log.info("freezingHandler 3");
				s.waitDuration(e1.remainingDurationPlus(Duration.ofSeconds(-2)));
				log.info("freezingHandler 4");
				s.updateCall(freezingVerySoon, e1);
				log.info("freezingHandler 5");
			}
	);

	private final ModifiableCallout<BuffApplied> inferno = ModifiableCallout.durationBasedCall("Inferno Soon", "Light Party Stacks then Eruptions");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> infernoHandler = SqtTemplates.callWhenDurationIs(
			BuffApplied.class, ba -> ba.buffIdMatches(0xEEA), inferno, Duration.ofSeconds(8)
	);

	private final ModifiableCallout<AbilityCastStart> mountainFireInitial = ModifiableCallout.durationBasedCall("Mountain Fire Initial", "Tank Tower and Cleaves");
	private final ModifiableCallout<AbilityUsedEvent> mountainFireCleave = new ModifiableCallout<>("Mountain Fire Cleave", "{safe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mountainFireSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x900C),
			(e1, s) -> {
				s.updateCall(mountainFireInitial, e1);
				var e2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x900C));
				s.setParam("safe", ArenaSector.CENTER);
				s.updateCall(mountainFireCleave, e2);
				for (int i = 0; i < 5; i++) {
					// Old, slow - look at the cast IDs of the actual boss
					var nextEvent = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x900D, 0x900E, 0x900F, 0x9010, 0x9011, 0x9012));
					var safe = switch ((int) nextEvent.getAbility().getId()) {
						case 0x900E, 0x9010 -> ArenaSector.WEST;
						case 0x900F, 0x9011 -> ArenaSector.CENTER;
						case 0x900D, 0x9012 -> ArenaSector.EAST;
						default -> ArenaSector.UNKNOWN;
					};
					// Faster - look at angle of fake cast
//					double heading;
////					var nextevent = s.waitevent(abilitycaststart.class, acs -> acs.abilityidmatches(0x9019));
////					var locationdata = s.waiteventuntil(castlocationdataevent.class, clde -> clde.originalevent() == nextevent, baseevent.class, unused -> nextevent.geteffectivetimesince().tomillis() > 100);
//					var nextevent = s.waitevent(abilityusedevent.class, acs -> acs.abilityidmatches(0x901a));
//					var locationdata = s.waiteventuntil(snapshotlocationdataevent.class, clde -> clde.originalevent() == nextevent, baseevent.class, unused -> nextevent.geteffectivetimesince().tomillis() > 100);
//					if (locationdata != null) {
//						heading = locationdata.getbestheading();
//					}
//					else {
//						s.waitthenrefreshcombatants(100);
//						heading = state.getlatestcombatantdata(nextevent.getsource()).getpos().heading();
//					}
//					arenasector rawfacing = arenapos.combatantfacing(heading).opposite();
//					var safe = switch (rawfacing) {
//						case south -> arenasector.center;
//						case southwest -> arenasector.west;
//						case southeast -> arenasector.east;
//						default -> rawfacing;
//					};
					s.setParam("safe", safe);
					s.updateCall(mountainFireCleave, nextEvent);
				}
			});

	private final ModifiableCallout<AbilityCastStart> calamitousInitialTank = new ModifiableCallout<>("Calamitous Cry: Tank", "Line Stacks, In Front of Party");
	private final ModifiableCallout<AbilityCastStart> calamitousInitialNonTank = new ModifiableCallout<>("Calamitous Cry: Non-Tank", "Line Stacks, Behind Tank");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> calamitousCry = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9002),
			(e1, s) -> {
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(calamitousInitialTank, e1);
				}
				else {
					s.updateCall(calamitousInitialNonTank, e1);
				}
			});

	private final ModifiableCallout<BuffApplied> stormIceCall = ModifiableCallout.durationBasedCall("Calamity's Frost", "Spread, Up");
	private final ModifiableCallout<BuffApplied> stormLightningCall = ModifiableCallout.durationBasedCall("Calamity's Fulgur", "Spread, Down");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stormIceSq = SqtTemplates.callWhenDurationIs(
			BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xEEC),
			stormIceCall, Duration.ofSeconds(8));

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stormLightningSq = SqtTemplates.callWhenDurationIs(
			BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xEF0),
			stormLightningCall, Duration.ofSeconds(8));

	private double bestHeadingFor(AbilityCastStart event) {
		DescribesCastLocation<AbilityCastStart> li = event.getLocationInfo();
		if (li != null) {
			return li.getBestHeading();
		}
		else {
			return state.getLatestCombatantData(event.getSource()).getPos().heading();
		}
	}

	private Position bestLocationFor(AbilityCastStart event) {
		DescribesCastLocation<AbilityCastStart> li = event.getLocationInfo();
		if (li != null) {
			return li.getPos();
		}
		else {
			return state.getLatestCombatantData(event.getSource()).getPos();
		}
	}

	private final ArenaPos arena = new ArenaPos(100, 100, 3, 3);

	private final ModifiableCallout<AbilityCastStart> hailOfFeathersStart = ModifiableCallout.durationBasedCall("Hail of Feathers: Start", "Start {start}, Rotate {{ clockwise ? 'Clockwise' : 'Counter-clockwise' }}");
	private final ModifiableCallout<?> hailOfFeathersEnd = new ModifiableCallout<>("Hail of Feathers: End", "Kill {end} feather");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> hailOfFeathersSq = SqtTemplates.sq(30_000,
			// Boss cast is 8FDD
			// Other casts are 901D - 9022, based on duration. 901D is the shortest duration, so we care about that one.
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8FDD),
			(e1, s) -> {
				AbilityCastStart firstCast;
				AbilityCastStart finalCast;
				while (true) {
					firstCast = acr.getActiveCastById(0x901D).map(CastTracker::getCast).orElse(null);
					finalCast = acr.getActiveCastById(0x9022).map(CastTracker::getCast).orElse(null);
					if (firstCast == null || finalCast == null) {
						s.waitMs(100);
					}
					else {
						break;
					}
				}
				ArenaSector first = arena.forPosition(bestLocationFor(firstCast));
				ArenaSector last = arena.forPosition(bestLocationFor(finalCast));
				// e.g. if first is west and last is northwest, then first.eightsTo(last) == 1, but we are rotating ccw
				boolean clockwise = first.eighthsTo(last) < 0;

				s.setParam("first", first);
				s.setParam("start", first.opposite());

				s.setParam("last", last);
				s.setParam("clockwise", clockwise);
				s.setParam("end", last.opposite());


				s.updateCall(hailOfFeathersStart, firstCast);

				s.waitMs(12_000);

				s.updateCall(hailOfFeathersEnd);
			});

	@NpcCastCallout(value = 0x8FC1, suppressMs = 3000)
	private final ModifiableCallout<AbilityCastStart> cracklingCataclysm = ModifiableCallout.durationBasedCall("Crackling Cataclysm", "Move");

	private final ModifiableCallout<AbilityCastStart> thunderousBreath = ModifiableCallout.durationBasedCall("Thunderous Breath Safe Row", "Row {safeRow} Safe - Go Up");
	private final ModifiableCallout<AbilityCastStart> thunderousBreathError = ModifiableCallout.durationBasedCall("Thunderous Breath (Error)", "Find Safe Row, Go Up");
	private final ModifiableCallout<AbilityUsedEvent> thunderousBreathSpread = new ModifiableCallout<>("Thunderous Breath Spread", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> thunderousBreathSq = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8FE2),
			(e1, s) -> {
				List<XivCombatant> orbs;
				do {
					s.waitThenRefreshCombatants(100);
					orbs = state.npcsById(16770);
				} while (orbs.size() < 5);
				List<Double> yCoords = orbs.stream().map(cbt -> state.getLatestCombatantData(cbt).getPos().y()).toList();
				// The orb positions are 87.5, 92.5, ... 112.5
				Integer safeRow = null;
				for (int i = 1; i <= 6; i++) {
					double rowStart = 82 + 5 * i;
					double rowEnd = rowStart + 1;
					if (yCoords.stream().noneMatch(yc -> yc >= rowStart && yc <= rowEnd)) {
						safeRow = i;
					}
				}
				if (safeRow != null) {
					s.setParam("safeRow", safeRow);
					s.updateCall(thunderousBreath, e1);
				}
				else {
					s.updateCall(thunderousBreathError, e1);
				}
				var afterOrb = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x985A));
				s.updateCall(thunderousBreathSpread, afterOrb);
			});

	private final ModifiableCallout<AbilityCastStart> ruinfallTowerTank = ModifiableCallout.durationBasedCall("Ruinfall Tower: Tank", "Tank Tower, Knockback");
	private final ModifiableCallout<AbilityCastStart> ruinfallTowerNonTank = ModifiableCallout.durationBasedCall("Ruinfall Tower: Non-Tank", "Avoid Tower, Knockback");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> ruinfallTower = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8FFD),
			(e1, s) -> {
				if (state.playerJobMatches(Job::isTank)) {
					s.updateCall(ruinfallTowerTank, e1);
				}
				else {
					s.updateCall(ruinfallTowerNonTank, e1);
				}
			}
	);

	private final ModifiableCallout<?> calamityTankAway = new ModifiableCallout<>("Calamity's Embers - Tank", "Tank Buster - Avoid Party");
	private final ModifiableCallout<?> calamityHealerStacks = new ModifiableCallout<>("Calamity's Embers - Non-Tank", "Healer Stacks - Avoid Tanks");
	private final ModifiableCallout<?> chillingCataclysmSafe = new ModifiableCallout<>("Chilling Cataclysm - Ice Safe Spot", "{safe} safe");

	private void chillingCataclysmHandler(SequentialTriggerController<?> s) {
		Position northernMost;
		do {
			s.waitThenRefreshCombatants(100);
			// https://github.com/OverlayPlugin/cactbot/blob/main/ui/raidboss/data/07-dt/trial/valigarmanda-ex.ts#L694
			northernMost = state.npcsById(16667).stream().map(XivCombatant::getPos).filter(Objects::nonNull).filter(pos -> pos.y() <= 90).findFirst().orElse(null);
		} while (northernMost == null);
		ArenaSector safe = northernMost.x() > 100 ? ArenaSector.NORTHWEST : ArenaSector.NORTHEAST;
		s.setParam("safe", safe);
		s.updateCall(chillingCataclysmSafe);

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> calamitysEmbers = SqtTemplates.sq(150_000,
			BuffApplied.class, ba -> ba.buffIdMatches(0xEED),
			(e1, s) -> {
				s.waitMs(4_000);
				if (buffs.isStatusOnTarget(state.getPlayer(), 0xEED)) {
					s.updateCall(calamityTankAway);
				}
				else {
					s.updateCall(calamityHealerStacks);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8FC8, 0x8FCC, 0x8FD0));
				chillingCataclysmHandler(s);

			});


	// 8FC6
	private final ModifiableCallout<AbilityCastStart> iceCone = ModifiableCallout.durationBasedCall("Cone", "Cone, North Corners");
	// 8FCA
	private final ModifiableCallout<AbilityCastStart> iceOut = ModifiableCallout.durationBasedCall("Out", "Out");
	// 8FCE
	private final ModifiableCallout<AbilityCastStart> iceIn = ModifiableCallout.durationBasedCall("In", "In");
	private final ModifiableCallout<AbilityCastStart> avalancheWithCone = ModifiableCallout.durationBasedCall("Avalanche + Cone", "Cone, {safe}");
	private final ModifiableCallout<AbilityCastStart> avalancheWithIn = ModifiableCallout.durationBasedCall("Avalanche + In", "In, {safe}");
	private final ModifiableCallout<AbilityCastStart> avalancheWithOut = ModifiableCallout.durationBasedCall("Avalanche + Out", "Out, {safe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> calamitysEmbersSafeSpots = SqtTemplates.sq(150_000,
			BuffApplied.class, ba -> ba.buffIdMatches(0xEED),
			(e1, s) -> {
				var mapEffect = s.waitEvent(MapEffectEvent.class, mee -> (mee.getFlags() == 0x0002_0001 || mee.getFlags() == 0x00200010) && mee.getLocation() == 3);
				var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8FC8, 0x8FCC, 0x8FD0));
				ArenaSector avalancheSafe;
				if (mapEffect.getFlags() == 0x0002_0001) {
					avalancheSafe = ArenaSector.NORTHEAST;
				}
				else {
					avalancheSafe = ArenaSector.SOUTHWEST;
				}
				s.setParam("avalancheSafe", avalancheSafe);
				ArenaSector safe;
				if (cast.abilityIdMatches(0x8FC8) && avalancheSafe == ArenaSector.SOUTHWEST) {
					safe = ArenaSector.NORTHWEST;
				}
				else {
					safe = avalancheSafe;
				}
				s.setParam("safe", safe);
				switch ((int) cast.getAbility().getId()) {
					case 0x8FC8 -> s.updateCall(avalancheWithCone, cast);
					case 0x8FCC -> s.updateCall(avalancheWithOut, cast);
					case 0x8FD0 -> s.updateCall(avalancheWithIn, cast);
				}
			});

	private static final BiConsumer<AbilityCastStart, SequentialTriggerController<BaseEvent>> noop = (a, b) -> {
	};

	@NpcCastCallout(0x8FF0)
	private final ModifiableCallout<AbilityCastStart> freezingDust = ModifiableCallout.durationBasedCall("Freezing Dust", "Move");

	@NpcCastCallout(0x995)
	private final ModifiableCallout<AbilityCastStart> wrathUnfurled = ModifiableCallout.durationBasedCall("Wrath Unfurled", "Raidwide");

	/*
	Notes
	Spikesickle 8FF2 => Start West
	 */

	private final ModifiableCallout<MapEffectEvent> spikesicleWest = new ModifiableCallout<>("Spikesicle: Start West", "Start West");
	private final ModifiableCallout<MapEffectEvent> spikesicleEast = new ModifiableCallout<>("Spikesicle: Start East", "Start East");
	private final ModifiableCallout<MapEffectEvent> eruptionWest = new ModifiableCallout<>("Eruption: West Safe", "West Safe");
	private final ModifiableCallout<MapEffectEvent> eruptionEast = new ModifiableCallout<>("Eruption: East Safe", "East Safe");

	private final RepeatSuppressor mapEffectSupp = new RepeatSuppressor(Duration.ofSeconds(4));

	@HandleEvents
	public void mapEffects(EventContext context, MapEffectEvent event) {
		// Spikecicle
		if (event.getFlags() == 0x00020004L) {
			if (!mapEffectSupp.check(event)) {
				return;
			}
			switch ((int) event.getLocation()) {
				case 4 -> context.accept(spikesicleWest.getModified(event));
				case 5 -> context.accept(spikesicleEast.getModified(event));
				default -> log.warn("Unknown spikecicle: {}", event);
			}
		}
		// Volcano
		else if (event.getFlags() == 0x00200010) {
			if (!mapEffectSupp.check(event)) {
				return;
			}
			switch ((int) event.getLocation()) {
				case 0xE -> context.accept(eruptionWest.getModified(event));
				case 0xF -> context.accept(eruptionEast.getModified(event));
				default -> log.warn("Unknown Volcano: {}", event);
			}
		}
	}

	/*
	Susurrant, Slithering, Strangling
	These all seem to come in a pair of real + fake cast

	8FC6 - Cone, paired with ice octagonal things
	8FC7 - Cone+Buddies, boss cast
	8FC8 - Cone+Buddies, actual aoe, used with both 8FC6 and 8FC7

	8FCA - Out, paired with ice?
	8FCB - Out+Buddies, boss cast
	8FCC - Out+Buddies, actual aoe

	8FCE - Cone, paired with ice?
	8FCF - Cone+Buddies, boss cast
	8FD0 - Cone+Buddies, actual aoe



	 */


	@AutoFeed
	private final SequentialTrigger<BaseEvent> outMiddleConeMechStandalone = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8FC6, 0x8FCA, 0x8FCE),
			(e1, s) -> {
				// no need if that trigger is already active
				if (calamitysEmbersSafeSpots.isActive()) {
					return;
				}
				var initialCall = switch ((int) e1.getAbility().getId()) {
					case 0x8FC6 -> iceCone;
					case 0x8FCA -> iceOut;
					case 0x8FCE -> iceIn;
					default -> null;
				};
				if (initialCall != null) {
					s.updateCall(initialCall, e1);
				}
				chillingCataclysmHandler(s);

			});

}
