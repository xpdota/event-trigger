package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.Position;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CalloutRepo("P4S")
public class P4S implements FilteredEventHandler {
	private final ModifiableCallout decollation = new ModifiableCallout("Decollation", "Raidwide");
	private final ModifiableCallout bloodrake = new ModifiableCallout("Bloodrake", "Half Raidwide");
	private final ModifiableCallout evisceration = new ModifiableCallout("Elegant Evisceration", "Tankbuster on {target}");

	private final ModifiableCallout pinax = new ModifiableCallout("Pinax Mechanic + Safespot", "{mech1} into {mech2}, {safespot} safe");

	private final ModifiableCallout shift = new ModifiableCallout("Sword/Cape", "{direction} {cleavekb}");

	private final ModifiableCallout acting = new ModifiableCallout("Acting Role", "Acting {role}");

	private final ModifiableCallout nearsightParty = new ModifiableCallout("Nearsight (Non-Tank)", "Party Out");
	private final ModifiableCallout nearsightTank = new ModifiableCallout("Nearsight (Tank)", "Tanks In");
	private final ModifiableCallout farsightParty = new ModifiableCallout("Farsight (Non-Tank)", "Party In");
	private final ModifiableCallout farsightTank = new ModifiableCallout("Farsight (Tank)", "Tanks Out");

	private final ModifiableCallout redAct2 = new ModifiableCallout("Red Marker (Act 2)", "Red");
	private final ModifiableCallout tealAct2 = new ModifiableCallout("Teal Marker (Act 2)", "Teal");
	private final ModifiableCallout purpleAct2 = new ModifiableCallout("Purple Marker (Act 2)", "Purple");

	private final ModifiableCallout purpleAct4 = new ModifiableCallout("Purple Marker (Act 4)", "Purple");
	private final ModifiableCallout blueAct4 = new ModifiableCallout("Blue Marker (Act 4)", "Blue");

	private final ModifiableCallout red = new ModifiableCallout("Red Marker (Other)", "Red");
	private final ModifiableCallout teal = new ModifiableCallout("Teal Marker (Other)", "Teal");
	private final ModifiableCallout purple = new ModifiableCallout("Purple Marker (Other)", "Purple");
	private final ModifiableCallout blue = new ModifiableCallout("Blue Marker (Other)", "Blue");

	private final ModifiableCallout finaleOrder = new ModifiableCallout("Finale Order", "Soak tower {number}");
	private final ModifiableCallout finaleTower = new ModifiableCallout("Finale Tower", "Soak {towerspot} tower");

	private final ModifiableCallout curtainOrder = new ModifiableCallout("Curtain Order", "{number} set");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P4S(XivState state) {
		this.state = state;
	}

	private enum Act {
		PRE,
		ONE,
		TWO,
		THREE,
		FOUR,
		FINALE,
		CURTAIN
	}

	private Act currentAct = Act.PRE;

	private final XivState state;

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3F1);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		// TODO: tb in/out
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout call;
			if (id == 0x6A09) {
				call = decollation;
			}
			else if (id == 0x69D8) {
				call = bloodrake;
			}
			else if (id == 0x6A08) {
				call = evisceration;
			}
			else {
				return;
			}
			context.accept(call.getModified(Map.of("target", event.getTarget())));
		}
	}

	// TODO: integrate this into safespot call
	@HandleEvents
	public void directionlyShift(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			final boolean isSword;
			final ArenaSector where;
			int id = (int) event.getAbility().getId();
			switch (id) {
				case 0x6A02 -> {
					isSword = true;
					where = ArenaSector.NORTH;
				}
				case 0x6A03 -> {
					isSword = true;
					where = ArenaSector.SOUTH;
				}
				case 0x6A04 -> {
					isSword = true;
					where = ArenaSector.EAST;
				}
				case 0x6A05 -> {
					isSword = true;
					where = ArenaSector.WEST;
				}
				case 0x69FD -> {
					isSword = false;
					where = ArenaSector.NORTH;
				}
				case 0x69FE -> {
					isSword = false;
					where = ArenaSector.SOUTH;
				}
				case 0x69FF -> {
					isSword = false;
					where = ArenaSector.EAST;
				}
				case 0x6A00 -> {
					isSword = false;
					where = ArenaSector.WEST;
				}
				default -> {
					return;
				}
			}
			context.accept(shift.getModified(Map.of("direction", where.getFriendlyName(), "cleavekb", isSword ? "Cleave" : "Knockback")));
		}
	}

	private AbilityCastStart pinaxStackSpreadTmp;
	private AbilityCastStart pinaxKnockbackProxTmp;

	@HandleEvents
	public void clear(EventContext context, PullStartedEvent newPull) {
		pinaxStackSpreadTmp = null;
		pinaxKnockbackProxTmp = null;
	}
	/*
			else if (id == 0x69D4) {
				call = acidPinax;
			}
			else if (id == 0x69D5) {
				call = firePinax;
			}
			else if (id == 0x69D6) {
				call = waterPinax;
			}
			else if (id == 0x69D7) {
				call = lightningPinax;
			}
	 */

	@HandleEvents
	public void pinaxCollect(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		// Each of these seems to have two IDs, but the higher one is fake.
		if (id == 0x69D4) {
			pinaxStackSpreadTmp = event;
		}
		else if (id == 0x69D5) {
			pinaxStackSpreadTmp = event;
		}
		else if (id == 0x69D6) {
			pinaxKnockbackProxTmp = event;
		}
		else if (id == 0x69D7) {
			pinaxKnockbackProxTmp = event;
		}
		else {
			return;
		}
		if (pinaxKnockbackProxTmp != null && pinaxStackSpreadTmp != null) {
			context.accept(new PinaxSet(pinaxStackSpreadTmp, pinaxKnockbackProxTmp));
			pinaxKnockbackProxTmp = null;
			pinaxStackSpreadTmp = null;
		}
	}

	@HandleEvents
	public void pinaxSolved(EventContext context, PinaxSet event) {
		String stackSpread = event.pinaxStackSpread.getAbility().getId() == 0x69D4 ? "Spread" : "Light Parties";
		String knockbackProx = event.pinaxKnockbackProx.getAbility().getId() == 0x69D6 ? "Knockback" : "Away";
		ArenaSector badspot1 = arenaPos.forCombatant(event.pinaxKnockbackProx.getSource());
		ArenaSector badspot2 = arenaPos.forCombatant(event.pinaxStackSpread.getSource());
		List<ArenaSector> goodSpots = new ArrayList<>(ArenaSector.quadrants);
		goodSpots.remove(badspot1);
		goodSpots.remove(badspot2);
		String safespots;
		if (goodSpots.size() == 2) {
			@Nullable ArenaSector halfArenaSafe = ArenaSector.tryCombineTwoQuadrants(goodSpots);
			if (halfArenaSafe == null) {
				safespots = String.format("%s and %s", goodSpots.get(0).getFriendlyName(), goodSpots.get(1).getFriendlyName());
			}
			else {
				safespots = halfArenaSafe.getFriendlyName();
			}
		}
		else {
			safespots = "Unknown";
		}
		Map<String, Object> args = new HashMap<>(3);
		args.put("mech1", knockbackProx);
		args.put("mech2", stackSpread);
		args.put("safespot", safespots);
		context.accept(pinax.getModified(args));
	}

	private static final class PinaxSet extends BaseEvent {
		@Serial
		private static final long serialVersionUID = -5052611495100188678L;
		private final AbilityCastStart pinaxStackSpread;
		private final AbilityCastStart pinaxKnockbackProx;

		private PinaxSet(AbilityCastStart pinaxStackSpread, AbilityCastStart pinaxKnockbackProx) {
			this.pinaxStackSpread = pinaxStackSpread;
			this.pinaxKnockbackProx = pinaxKnockbackProx;
		}
	}

	@HandleEvents
	public void actingRole(EventContext context, BuffApplied event) {
		if (event.getTarget().isThePlayer()) {
			int id = (int) event.getBuff().getId();
			final String role;
			switch (id) {
				case 0xB6D -> role = "DPS";
				case 0xB6E -> role = "Healer";
				case 0xB6F -> role = "Tank";
				default -> {
					return;
				}
			}
			context.accept(acting.getModified(Map.of("role", role)));
		}
	}

	private boolean isPhase2;

	// Resetting HM counter on phase 2 since there are other HMs in p1
	@HandleEvents
	public void phase2start(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x6A2D) {
			isPhase2 = true;
		}
	}

	@HandleEvents
	public void reset(EventContext context, DutyCommenceEvent event) {
		isPhase2 = false;
		currentAct = Act.PRE;
		firstHeadmark = null;
		fleetingImpulseDebuffCount = 0;
		myFleetingImpulseNumber = 0;
		fleetingImpulseTetherCount = 0;
	}

	private Long firstHeadmark;

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}


	@HandleEvents
	public void act2headmark(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		// TODO: tank with no tether
		ModifiableCallout call = switch (currentAct) {
			case TWO -> switch (headmarkOffset) {
				case 0 -> redAct2;
				case -1 -> tealAct2;
				case -2 -> purpleAct2;
				default -> null;
			};
			case FOUR -> switch (headmarkOffset) {
				case -2 -> purpleAct4;
				case -3 -> blueAct4;
				default -> null;
			};
			default -> switch (headmarkOffset) {
				case 0 -> red;
				case -1 -> teal;
				case -2 -> purple;
				case -3 -> blue;
				default -> null;
			};
		};
		if (call != null) {
			context.accept(call.getModified());
		}
	}

	private TetherEvent lastTether;

	@HandleEvents
	public void tetherHandler(EventContext context, TetherEvent tether) {
		// Ignore repeated tethers - we only care about first tether.
		if (isPhase2 && tether.getId() == 173 && (lastTether == null || lastTether.getEffectiveTimeSince().toMillis() > 20_000)) {
			lastTether = tether;
			Position tetherPos = tether.getSource().getPos();
			if (tetherPos == null) {
				return;
			}
			// TODO: floating point equality memes
			// TODO: weird tethers right before Wreath of Thorns cast start? This probably doesn't work right yet.
			if (tetherPos.getX() == 100.0d) {
				// east/west first
			}
			else if (tetherPos.getY() == 100.0d) {
				// north/south first
			}
		}
	}

	@HandleEvents
	public void nearFarSight(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			final ModifiableCallout call;
			if (event.getAbility().getId() == 0x6A26) {
				Job playerJob = state.getPlayerJob();
				if (playerJob != null && playerJob.isTank()) {
					call = nearsightTank;
				}
				else {
					call = nearsightParty;
				}
			}
			else if (event.getAbility().getId() == 0x6A27) {
				Job playerJob = state.getPlayerJob();
				if (playerJob != null && playerJob.isTank()) {
					call = farsightTank;
				}
				else {
					call = farsightParty;
				}
			}
			else {
				return;
			}
			context.accept(call.getModified());
		}
	}

	private int fleetingImpulseDebuffCount;
	private int fleetingImpulseTetherCount;
	private int myFleetingImpulseNumber;

	@HandleEvents
	public void fleetingImpulseCounter(EventContext context, AbilityUsedEvent event) {
		if (event.getAbility().getId() == 0x6A1C) {
			fleetingImpulseDebuffCount++;
			if (event.getTarget().isThePlayer()) {
				context.accept(finaleOrder.getModified(Map.of("number", fleetingImpulseDebuffCount)));
				myFleetingImpulseNumber = fleetingImpulseDebuffCount;
			}
		}
	}

	@HandleEvents
	public void fleetingImpulseTether(EventContext context, TetherEvent tether) {
		// TODO: this should really just look at first tower and compute based off of that
		if (currentAct == Act.FINALE && tether.getId() == 0xad) {
			fleetingImpulseTetherCount++;
			if (fleetingImpulseTetherCount == myFleetingImpulseNumber) {
				ArenaSector arenaSector = arenaPos.forCombatant(tether.getSource());
				context.accept(finaleTower.getModified(Map.of("towerspot", arenaSector.getFriendlyName())));
			}
		}
	}

	@HandleEvents
	public void curtainCallNumber(EventContext context, BuffApplied event) {
		if (currentAct == Act.CURTAIN && event.getBuff().getId() == 0xAF4 && event.getTarget().isThePlayer()) {
			long dur = event.getInitialDuration().toSeconds();
			final String number;
			if (dur < 15) {
				number = "First";
			}
			else if (dur < 25) {
				number = "Second";
			}
			else if (dur < 35) {
				number = "Third";
			}
			else {
				number = "Last";
			}
			context.accept(curtainOrder.getModified(Map.of("number", number)));
		}
	}


	@HandleEvents
	public void actTracker(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			int id = (int) event.getAbility().getId();
			switch (id) {
				case 0x6A0C -> currentAct = Act.ONE;
				case 0x6EB4 -> currentAct = Act.TWO;
				case 0x6EB5 -> currentAct = Act.THREE;
				case 0x6EB6 -> currentAct = Act.FOUR;
				case 0x6EB7 -> currentAct = Act.FINALE;
				case 0x6A36 -> currentAct = Act.CURTAIN;
			}
		}

	}

}
