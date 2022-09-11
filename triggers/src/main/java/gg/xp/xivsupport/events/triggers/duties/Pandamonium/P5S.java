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
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CalloutRepo(name = "P5S", duty = KnownDuty.P5S)
public class P5S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P5S.class);
	private final ModifiableCallout<AbilityCastStart> searingRay = ModifiableCallout.durationBasedCall("Searing Ray", "Go Behind");
	private final ModifiableCallout<AbilityCastStart> searingRayReflected = ModifiableCallout.durationBasedCall("Searing Ray Reflected", "Go Front");
	private final ModifiableCallout<AbilityCastStart> rubyGlow = ModifiableCallout.durationBasedCall("Ruby Glow", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> sonicHowl = ModifiableCallout.durationBasedCall("Sonic Howl", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> toxicCrunch = ModifiableCallout.durationBasedCall("Toxic Crunch", "Tankbuster on MT");
	// TODO: for these two, make a sequential that calls out mechs as they happen
	private final ModifiableCallout<AbilityCastStart> venomSquall = ModifiableCallout.durationBasedCall("Venom Squall", "Spread then Bait then Light Parties");
	private final ModifiableCallout<AbilityCastStart> venomSurge = ModifiableCallout.durationBasedCall("Venom Surge", "Light Parties then Bait then Spread");
	private final ModifiableCallout<AbilityCastStart> doubleRush = ModifiableCallout.durationBasedCall("Double Rush", "Knockback");
	private final ModifiableCallout<AbilityCastStart> venomousMass = ModifiableCallout.durationBasedCall("Venomous Mass", "Tankbuster with Bleed");
	private final ModifiableCallout<AbilityCastStart> clawToTail = ModifiableCallout.durationBasedCall("Claw to Tail", "Go Back then Front");
	private final ModifiableCallout<AbilityCastStart> tailToClaw = ModifiableCallout.durationBasedCall("Tail to Claw", "Go Front then Back");
	// Can be deleted? Seems redundant with claw/tail calls
//	private final ModifiableCallout<AbilityCastStart> ragingClaw = ModifiableCallout.durationBasedCall("Raging Claw", "Go Behind");

	private final ModifiableCallout<AbilityCastStart> firstRubyGlowSafe = ModifiableCallout.durationBasedCall("First Ruby Ray Safe", "{safe}");

	private final ModifiableCallout<AbilityCastStart> topazCluster = new ModifiableCallout<>("Topaz Cluster Initial", "{allSafeSpots}", 20_000);
	private final ModifiableCallout<AbilityCastStart> topazClusterAfter = new ModifiableCallout<>("Topaz Cluster Followup", "{nextSafeSpot}", "{remainingSafeSpots}");
//	private final ModifiableCallout<AbilityCastStart> topazCluster = new ModifiableCallout<>("Topaz Cluster Initial", "{safe} then {next}", 20_000);
//	private final ModifiableCallout<AbilityCastStart> topazClusterLast = new ModifiableCallout<>("Topaz Cluster Initial", "{safe}", 20_000);
//	private final ModifiableCallout<AbilityCastStart> topazClusterFollowup = new ModifiableCallout<>("Topaz Cluster Initial", "{safe} safe", 5_000);
	// TODO: not sure what duration to use for these, might need some fakery
//	private final ModifiableCallout<AbilityCastStart> topazClusterFollowup = ModifiableCallout.durationBasedCall("Topaz Cluster Followup", "{safe}");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);
	private final ArenaPos tightArenaPos = new ArenaPos(100, 100, 0.1, 0.1);

	public P5S(XivState state, ActiveCastRepository acr) {
		this.state = state;

		this.acr = acr;
	}

	private final XivState state;

	private XivState getState() {
		return this.state;
	}

	private final ActiveCastRepository acr;

	public ActiveCastRepository getAcr() {
		return acr;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P5S);
	}

	private final RepeatSuppressor tailClawRepeat = new RepeatSuppressor(Duration.ofMillis(11_000));

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		if (id == 0x0)
			call = searingRay;
		else if (id == 0x7657)
			call = searingRayReflected;
		else if (id == 0x76F3) {
			call = rubyGlow;
			rubyGlows++;
		}
		else if (id == 0x7720)
			call = sonicHowl;
		else if (id == 0x784A) //has no target TODO: Find who has agro and call (auto attack hack)
			call = toxicCrunch;
		else if (id == 0x7716)
			call = venomSquall;
		else if (id == 0x771D)
			call = venomousMass;
		else if (id == 0x771B)
			call = doubleRush;
		else if (id == 0x7717)
			call = venomSurge;
		else if (id == 0x770E)
			call = clawToTail;
		else if (id == 0x7712)
			call = tailToClaw;
//		else if(id == 0x770F) //TODO: fix calling when boss casts 770E or 7712
//			call = ragingClaw;
		else
			return;
		context.accept(call.getModified(event));
	}

	private int rubyGlows = 0;

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		rubyGlows = 0;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> firstRubyGlow = new SequentialTrigger<>( //76fe CRYSTAL SUMMON, 76FD topaz boss cast
			20_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x76FD, 0x76FE),
			(e1, s) -> {
				if (rubyGlows != 1) {
					//log.error("FirstRubyRay: It's not the first ruby ray!");
					return;
				}
				List<AbilityCastStart> crystalSummons = new ArrayList<>(s.waitEvents(4, AbilityCastStart.class, event -> event.abilityIdMatches(0x76FE, 0x76FD)));
				crystalSummons.add((AbilityCastStart) e1);
				AbilityCastStart rubyGlowCast = crystalSummons.stream().filter(acs -> acs.getAbility().getId() == 0x76FD).findFirst().orElseThrow(() -> new RuntimeException("FirstRubyRay: Couldn't find the original cast!"));
				crystalSummons.remove(rubyGlowCast);
				
				if (crystalSummons.size() != 4) {
					log.error("FirstRubyRay: Invalid number of Crystal Summons found! Data: {}", crystalSummons);
					return;
				}

				s.waitMs(100);
				s.refreshCombatants(100);
				List<XivCombatant> topazCrystals = crystalSummons.stream().map(acs -> this.getState().getLatestCombatantData(acs.getSource())).toList();

				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.quadrants);
				for (XivCombatant c : topazCrystals) {
					int approxX = (int) Math.round(c.getPos().x());
					int approxY = (int) Math.round(c.getPos().y());
					double realX = c.getPos().x();
					double realY = c.getPos().y();
					if ((approxX == 111 || approxX == 89) && (approxY == 111 || approxY == 89)) { //bad corner toxic crystal
						safe.remove(arenaPos.forCombatant(c));
					}
					else if (approxX != 104 && approxX != 96 && approxY != 104 && approxY != 96) { //not safe toxic crystal
						if (realX < 100 && realY < 100) { //nw
							safe.remove(ArenaSector.NORTHWEST);
						}
						else if (realX > 100 && realY < 100) { //ne
							safe.remove(ArenaSector.NORTHEAST);
						}
						else if (realX < 100 && realY > 100) { //sw
							safe.remove(ArenaSector.SOUTHWEST);
						}
						else if (realX > 100 && realY > 100) { //se
							safe.remove(ArenaSector.SOUTHEAST);
						}
						else {
							log.error("FirstRubyRay: ERROR REMOVING UNSAFE SPOTS x: {} y: {}", realX, realY);
							return;
						}
					}
				}

				Map<String, Object> args = Map.of("safe", safe);
				s.accept(firstRubyGlowSafe.getModified((AbilityCastStart) e1, args));
			}
	);

	//TODO:third ruby glow 1 2 3 4 safe spots (crystals spawn 2 2 3 3)

	@AutoFeed
	private final SequentialTrigger<BaseEvent> topazClusterSq = new SequentialTrigger<>(40_000, BaseEvent.class,
			e1 -> e1 instanceof AbilityCastStart acs && acs.abilityIdMatches(0x7702),
			(e1, s) -> {
				// 10 crystals spawn
				// 7703 is the shortest (3.7s)
				// 7704 is 6.2s
				// 7705 is 8.7s
				// 7706 is 11.2s
				// Can use either ID or duration to differentiate
				log.info("Topaz Cluster: Start");
				s.waitMs(200);
				s.refreshCombatants(200);
				log.info("Topaz Cluster: Done Waiting");
				Map<Integer, List<ArenaSector>> unsafeSpots = new HashMap<>();
				Map<Integer, AbilityCastStart> sampleCasts = new HashMap<>();
				// TODO: Does this need the 'in progress' filter?
				getAcr().getAll().stream().filter(ct -> ct.getCast().abilityIdMatches(0x7703, 0x7704, 0x7705, 0x7706))
						.forEach(ct -> {
							AbilityCastStart cast = ct.getCast();
							int index = (int) (cast.getAbility().getId() - 0x7703);
							unsafeSpots.computeIfAbsent(index, unused -> new ArrayList<>())
									.add(tightArenaPos.forCombatant(getState().getLatestCombatantData(cast.getSource())));
							sampleCasts.putIfAbsent(index, cast);
						});
				log.info("Topaz Cluster Unsafe: {} --- {} --- {} --- {}", unsafeSpots.get(0), unsafeSpots.get(1), unsafeSpots.get(2), unsafeSpots.get(3));
				List<List<ArenaSector>> safeSpots = new ArrayList<>(4);
				for (int i = 0; i < 4; i++) {
					List<ArenaSector> unsafe = unsafeSpots.get(i);
					List<ArenaSector> safe = new ArrayList<>(ArenaSector.quadrants);
					safe.removeAll(unsafe);
					safeSpots.add(safe);
					if (safe.size() < 1 || safe.size() > 2) {
						log.error("Topaz cluster error! i={}, unsafe={}, safe={}", i, unsafe, safe);
						return;
					}
				}
				// TODO: this is a mess. Just pick one safe spot from the first two sets, probably based on final wd
				log.info("Topaz Cluster Safe: {} --- {} --- {} --- {}", safeSpots.get(0), safeSpots.get(1), safeSpots.get(2), safeSpots.get(3));
				List<ArenaSector> singleSafeSpots = safeSpots.stream().map(l -> l.get(0)).toList();
				s.updateCall(topazCluster.getModified(sampleCasts.get(0), Map.of("allSafeSpots", singleSafeSpots, "nextSafeSpot", singleSafeSpots.get(0))));

				for (int i = 1; i < 4; i++) {
					// Wait for the current mechanic to actually snapshot before telling player to move
					XivCombatant source = sampleCasts.get(i - 1).getSource();
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.getSource().equals(source) && aue.abilityIdMatches(0x79FFL));
					s.updateCall(topazClusterAfter.getModified(sampleCasts.get(0), Map.of("remainingSafeSpots", singleSafeSpots.subList(i, 4), "nextSafeSpot", singleSafeSpots.get(i))));
				}
			});
}
