package gg.xp.xivsupport.events.triggers.duties;

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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@CalloutRepo(name = "The Weapon's Refrain", duty = KnownDuty.UWU)
public class Uwu extends AutoChildEventHandler implements FilteredEventHandler {

	// TODO: make a standard for a trigger class to provide extra timeline triggers like Cactbot
	private static final Logger log = LoggerFactory.getLogger(Uwu.class);

	private final ModifiableCallout<BuffApplied> garudaWoken = new ModifiableCallout<>("Garuda Woken");
	private final ModifiableCallout<BuffApplied> ifritWoken = new ModifiableCallout<>("Ifrit Woken");
	private final ModifiableCallout<BuffApplied> titanWoken = new ModifiableCallout<>("Titan Woken");

	//	private final ModifiableCallout<HeadMarkerEvent> mistral = new ModifiableCallout<>("Mistral (Headmark)", "Mistral ({target1} and {target2})");
	private final ModifiableCallout<HeadMarkerEvent> mistralOnYou = new ModifiableCallout<>("Mistral (Headmark)", "Mistral on you");
	private final ModifiableCallout<AbilityCastStart> slipStream = ModifiableCallout.durationBasedCall("Slipstream (Cleave)", "Slipstream");
	private final ModifiableCallout<AbilityCastStart> aerialBlast = ModifiableCallout.durationBasedCall("Aerial Blast", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> friction = ModifiableCallout.durationBasedCall("Friction", "Stack");

	private final ModifiableCallout<AbilityCastStart> crimsonCycloneTransition = ModifiableCallout.durationBasedCall("Crimson Cyclone Transition", "{direction}");
	private final ModifiableCallout<BuffApplied> fetters = ModifiableCallout.<BuffApplied>durationBasedCall("Fetters", "Fetters").autoIcon();
	private final ModifiableCallout<AbilityCastStart> searingWindInitial = ModifiableCallout.<AbilityCastStart>durationBasedCall("Searing Wind (Initial)", "Searing Wind on {event.target}").statusIcon(0x62a);
	private final ModifiableCallout<BuffApplied> searingWindLinger = new ModifiableCallout<BuffApplied>("Searing Wind (Linger Time)", "", "Searing Wind on {event.target} ({event.estimatedRemainingDuration})", ModifiableCallout.durationExpiryPlusDefaultLinger()).statusIcon(0x62a);
	private final ModifiableCallout<HeadMarkerEvent> flamingCrush = new ModifiableCallout<HeadMarkerEvent>("Flaming Crush", "Stack on {event.target}").statusIcon(0x5F7);
	private final ModifiableCallout<AbilityCastStart> hellfire = ModifiableCallout.durationBasedCall("Hellfire", "Raidwide");

	private final ModifiableCallout<?> titanBoulderLeftSafe = new ModifiableCallout<>("Titan Bury: Left Safe", "Left");
	private final ModifiableCallout<?> titanBoulderRightSafe = new ModifiableCallout<>("Titan Bury: Right Safe", "Right");
	private final ModifiableCallout<AbilityCastStart> earthenFury = ModifiableCallout.durationBasedCall("Earthen Fury", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> ultima = ModifiableCallout.durationBasedCall("Ultima", "Ultima");
	private final ModifiableCallout<AbilityCastStart> homingLaser = ModifiableCallout.durationBasedCall("Homing Laser", "Homing Laser on {event.target}");
	private final ModifiableCallout<AbilityUsedEvent> ultimaLandslideLeftSafe = new ModifiableCallout<>("Predation: Back then Left", "Back then Left");
	private final ModifiableCallout<AbilityUsedEvent> ultimaLandslideRightSafe = new ModifiableCallout<>("Predation: Back then Right", "Back then Right");
	private final ModifiableCallout<AbilityCastStart> ultimaLaserLeft = ModifiableCallout.durationBasedCall("Ultima Laser: Left", "Left Laser");
	private final ModifiableCallout<AbilityCastStart> ultimaLaserMiddle = ModifiableCallout.durationBasedCall("Ultima Laser: Middle", "Middle Laser");
	private final ModifiableCallout<AbilityCastStart> ultimaLaserRight = ModifiableCallout.durationBasedCall("Ultima Laser: Right", "Right Laser");

	private final ModifiableCallout<AbilityCastStart> roulette1 = new ModifiableCallout<>("Primal Roulette: Initial", "{primal1}, {primal2}, {primal3}", "{primal1} → {primal2} → {primal3}");
	private final ModifiableCallout<AbilityCastStart> roulette2 = new ModifiableCallout<>("Primal Roulette: Second", "{primal2}", "{primal2} → {primal3}");
	private final ModifiableCallout<AbilityCastStart> roulette3 = new ModifiableCallout<>("Primal Roulette: Last", "{primal3}", "{primal3}");

	private final XivState state;
	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public Uwu(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivStateImpl.class).zoneIs(0x309L);
	}

	public XivState getState() {
		return state;
	}

	@HandleEvents
	public void firstMistral(EventContext context, HeadMarkerEvent hme) {
		if (hme.getMarkerId() == 0x10 && hme.getTarget().isThePlayer()) {
			context.accept(mistralOnYou.getModified(hme));
		}
	}

//	// TODO: what to do with this?
//	@AutoFeed
//	private final SequentialTrigger<HeadMarkerEvent> mistralSeq = new SequentialTrigger<>(5_000,
//			HeadMarkerEvent.class,
//			hme -> hme.getMarkerId() == 0x10,
//			(e1, s) -> {
//				log.info("Mistral 1: {}", e1.getTarget());
//				HeadMarkerEvent e2 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerId() == 0x10);
//				log.info("Mistral 2: {}", e2.getTarget());
//				s.accept(mistral.getModified(e1, Map.of("target1", e1.getTarget(), "target2", e2.getTarget())));
//			}
//	);

	// TODO: we will need better combatant add/remove support


	// TODO: display debuff duration after

	@AutoFeed
	private final SequentialTrigger<BaseEvent> searingWindSeq = new SequentialTrigger<>(60_000, BaseEvent.class,
			(event) -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x2B5B),
			(e1, s) -> {
				s.updateCall(searingWindInitial.getModified((AbilityCastStart) e1));
				BuffApplied buff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x62a));
				s.updateCall(searingWindLinger.getModified(buff));
			});

	@HandleEvents
	public void basicCasts(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x2B53 -> call = slipStream;
			case 0x2B55 -> call = aerialBlast;
			case 0x2B48 -> call = friction;
			case 0x2B5E -> call = hellfire;
			case 0x2B90 -> call = earthenFury;
			case 0x2B7B -> call = homingLaser;
			case 0x2B84 -> call = ultimaLaserMiddle;
			case 0x2B85 -> call = ultimaLaserRight;
			case 0x2B86 -> call = ultimaLaserLeft;
			case 0x2B8B -> call = ultima;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@HandleEvents
	public void basicHeadmarkers(EventContext context, HeadMarkerEvent hme) {
		if (hme.getMarkerId() == 0x75) {
			context.accept(flamingCrush.getModified(hme));
		}
	}

	@HandleEvents
	public void basicBuffs(EventContext context, BuffApplied event) {
		int id = (int) event.getBuff().getId();
		if (event.getTarget().isThePlayer()) {
			switch (id) {
				case 0x179 -> {
					if (!event.isRefresh()) {
						context.accept(fetters.getModified(event));
					}
				}
			}
		}
		else {
			if (id == 0x5F9) {
				switch ((int) event.getTarget().getbNpcId()) {
					case 8722 -> context.accept(garudaWoken.getModified(event));
					case 8730 -> context.accept(ifritWoken.getModified(event));
					case 8727 -> context.accept(titanWoken.getModified(event));
					// We don't care about Lahabrea or Ultima
				}
			}
		}
	}

	private static final List<Position> leftPos = List.of(
			Position.of2d(105, 112),
			Position.of2d(112, 95),
			Position.of2d(88, 105),
			Position.of2d(95, 88)
	);

	private static final List<Position> rightPos = List.of(
			Position.of2d(105, 88),
			Position.of2d(112, 105),
			Position.of2d(88, 95),
			Position.of2d(95, 112)
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> boulderLeftRight = new SequentialTrigger<>(20_000, BaseEvent.class,
			// Start on "Upheaval"
			e -> (e instanceof AbilityCastStart acs && acs.abilityIdMatches(0x2B67)),
			(e1, s) -> {
				log.info("Titan boulder: Start");
				s.waitMs(1_000);
				XivState state = getState();
				List<Position> boulders;
				while (true) {
					boulders = state.getCombatantsListCopy().stream()
							.filter(cbt -> cbt.getbNpcId() == 8728)
							.map(XivCombatant::getPos)
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
					if (boulders.size() < 5) {
						s.refreshCombatants(100);
					}
					else {
						break;
					}
				}
				String debugText = boulders.stream().map(pos -> String.format("[%s, %s]", pos.x(), pos.y())).collect(Collectors.joining(", "));
				log.info("Titan Boulders: {}", debugText);
				for (Position boulder : boulders) {
					for (Position possibleMatch : leftPos) {
						// Allow for some floating point memery
						if (boulder.distanceFrom2D(possibleMatch) < 0.2) {
							s.accept(titanBoulderLeftSafe.getModified());
							return;
						}
					}
					for (Position possibleMatch : rightPos) {
						if (boulder.distanceFrom2D(possibleMatch) < 0.2) {
							s.accept(titanBoulderRightSafe.getModified());
							return;
						}
					}
				}
			});

	private enum PrimalRouletteOption {
		Garuda,
		Ifrit,
		Titan
	}

	private @Nullable PrimalRouletteOption getPrimalRouletteAbility(HasAbility ha) {
		return switch ((int) ha.getAbility().getId()) {
			case 0x2CD3 -> PrimalRouletteOption.Garuda;
			case 0x2CD4 -> PrimalRouletteOption.Ifrit;
			case 0x2CD5 -> PrimalRouletteOption.Titan;
			default -> null;
		};
	}


	@AutoFeed
	private final SequentialTrigger<AbilityCastStart> primalRoulette = new SequentialTrigger<>(90_000, AbilityCastStart.class,
			acs -> getPrimalRouletteAbility(acs) != null,
			(e1, s) -> {
				PrimalRouletteOption primal1 = getPrimalRouletteAbility(e1);
				PrimalRouletteOption primal2;
				PrimalRouletteOption primal3;
				//noinspection ConstantConditions
				switch (primal1) {
					case Garuda -> {
						primal2 = PrimalRouletteOption.Ifrit;
						primal3 = PrimalRouletteOption.Titan;
					}
					case Ifrit -> {
						primal2 = PrimalRouletteOption.Garuda;
						primal3 = PrimalRouletteOption.Titan;
					}
					case Titan -> {
						primal2 = PrimalRouletteOption.Ifrit;
						primal3 = PrimalRouletteOption.Garuda;
					}
					default -> {
						log.info("Bad primal roulette: {}", primal1);
						return;
					}
				}
				Map<String, Object> params = Map.of("primal1", primal1, "primal2", primal2, "primal3", primal3);
				s.updateCall(roulette1.getModified(e1, params));
				AbilityCastStart e2 = s.waitEvent(AbilityCastStart.class, acs -> getPrimalRouletteAbility(acs) != null);
				s.updateCall(roulette2.getModified(e2, params));
				AbilityCastStart e3 = s.waitEvent(AbilityCastStart.class, acs -> getPrimalRouletteAbility(acs) != null);
				s.updateCall(roulette3.getModified(e3, params));
			});

	private static final Position titanLeftPos = Position.of2d(113.7, 113.7);
	private static final Position titanRightPos = Position.of2d(86.3, 113.7);

	@HandleEvents
	public void ultimaLandslideLeftRight(EventContext context, AbilityUsedEvent event) {
		if (event.abilityIdMatches(0x2B68)) {
			Position pos = event.getSource().getPos();
			if (pos == null) {
				return;
			}
			if (titanLeftPos.distanceFrom2D(pos) < 0.2) {
				context.accept(ultimaLandslideLeftSafe.getModified(event));
			}
			else if (titanRightPos.distanceFrom2D(pos) < 0.2) {
				context.accept(ultimaLandslideRightSafe.getModified(event));
			}
		}
	}

	// TODO: this shouldn't call out Titan
//	private final ModifiableCallout woken = new ModifiableCallout("Woken", "{target} awoken");
//	@HandleEvents
//	public void searingWind(EventContext context, BuffApplied event) {
//		if (event.getBuff().getId() == 0x5F9) {
//			context.accept(woken.getModified(Map.of("target", event.getTarget())));
//		}
//	}

	//Called relative to looking at ifrit
	@AutoFeed
	public SequentialTrigger<BaseEvent> crimsonCycloneTransitionSq = SqtTemplates.sq(10_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x2B5F),
			(e1, s) -> {
				log.info("Crimson Cyclone Transition: Start");
				List<AbilityCastStart> plumes = s.waitEvents(10, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x2B61));
				List<ArenaSector> safe = new ArrayList<>(ArenaSector.cardinals);

				for (AbilityCastStart acs : plumes) {
					ArenaSector unsafe = arenaPos.forCombatant(acs.getSource());
					safe.remove(unsafe);
				}

				log.info("Crimson Cyclone Transition: Possible spots: {}", safe);
				ArenaSector ifritLocation = arenaPos.forCombatant(e1.getSource());
				switch (ifritLocation) {
					case NORTH, SOUTH -> {
						safe.remove(ArenaSector.NORTH);
						safe.remove(ArenaSector.SOUTH);
					}
					case EAST, WEST -> {
						safe.remove(ArenaSector.EAST);
						safe.remove(ArenaSector.WEST);
					}
				}

				String direction;
				ArenaSector oneSafe = safe.get(0);
				switch (oneSafe) {
					case NORTH -> {
						if (ifritLocation == ArenaSector.WEST) {
							direction = "right";
						} else {
							direction = "left";
						}
					}
					case SOUTH -> {
						if (ifritLocation == ArenaSector.EAST) {
							direction = "right";
						} else {
							direction = "left";
						}
					}
					case WEST -> {
						if (ifritLocation == ArenaSector.NORTH) {
							direction = "left";
						} else {
							direction = "right";
						}
					}
					case EAST -> {
						if (ifritLocation == ArenaSector.SOUTH) {
							direction = "left";
						} else {
							direction = "right";
						}
					}
					default -> direction = "unknown";
				}

				log.info("Crimson Cyclone Transition: Ifrit is at {}, safe should be {}", ifritLocation, safe);
				s.accept(crimsonCycloneTransition.getModified(e1, Map.of("cardinal", safe, "direction", direction)));
			});
}
