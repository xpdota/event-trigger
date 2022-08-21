package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.duties.KnownDuty;
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
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "P4S", duty = KnownDuty.P4S)
public class P4S implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P4S.class);
	private final ModifiableCallout<AbilityCastStart> decollation = ModifiableCallout.durationBasedCall("Decollation", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> bloodrake = ModifiableCallout.durationBasedCall("Bloodrake", "Half Raidwide");
	private final ModifiableCallout<AbilityCastStart> evisceration = ModifiableCallout.durationBasedCall("Elegant Evisceration", "Tankbuster on {target}");
	private final ModifiableCallout<AbilityCastStart> searing = ModifiableCallout.durationBasedCall("Searing Stream", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> ultimaImpulse = ModifiableCallout.durationBasedCall("Ultima Impulse", "Big Raidwide");

	private final ModifiableCallout<Event> neither = new ModifiableCallout<>("Neither Mechanic", "Nothing");
	private final ModifiableCallout<Event> tethers = new ModifiableCallout<>("Get Tethers", "Tethers");
	private final ModifiableCallout<Event> rot = new ModifiableCallout<>("Get Rot", "Rot");
	private final ModifiableCallout<Event> tethersAndRot = new ModifiableCallout<>("Get Tethers and Rot", "Both");
	private final ModifiableCallout<Event> towers = new ModifiableCallout<>("Get Towers", "Towers");
	private final ModifiableCallout<Event> towersAndTethers = new ModifiableCallout<>("Get Towers and Tethers", "Towers and Tethers");

	private final ModifiableCallout<AbilityCastStart> hellsSting = ModifiableCallout.durationBasedCall("Hell's Sting", "Protean, dodge in");

	private final ModifiableCallout<PinaxSet> pinax = new ModifiableCallout<>("Pinax Mechanic + Safespot", "{mech1} into {mech2}, {safespot} safe");

	private final ModifiableCallout<AbilityCastStart> shift = ModifiableCallout.durationBasedCall("Sword/Cape", "{direction} {cleavekb}");

	private final ModifiableCallout<BuffApplied> acting = new ModifiableCallout<>("Acting Role", "Acting {role}");
	private final ModifiableCallout<AbilityCastStart> belone = new ModifiableCallout<>("Belone Bursts", "Orb Positions");
	private final ModifiableCallout<AbilityCastStart> periaktoi = new ModifiableCallout<>("Periaktoi", "{element} safe ({safespot})");

	private final ModifiableCallout<AbilityCastStart> nearsightParty = ModifiableCallout.durationBasedCall("Nearsight (Non-Tank)", "Party Out");
	private final ModifiableCallout<AbilityCastStart> nearsightTank = ModifiableCallout.durationBasedCall("Nearsight (Tank)", "Tanks In");
	private final ModifiableCallout<AbilityCastStart> farsightParty = ModifiableCallout.durationBasedCall("Farsight (Non-Tank)", "Party In");
	private final ModifiableCallout<AbilityCastStart> farsightTank = ModifiableCallout.durationBasedCall("Farsight (Tank)", "Tanks Out");
	private final ModifiableCallout<AbilityCastStart> demigodDouble = ModifiableCallout.durationBasedCall("Demigod Double", "Shared buster on {target}");
	private final ModifiableCallout<AbilityCastStart> heartStake = ModifiableCallout.durationBasedCall("Heart Stake", "Bleed buster on {target}");

	private final ModifiableCallout<HeadMarkerEvent> redAct2 = new ModifiableCallout<>("Red Marker (Act 2)", "Red");
	private final ModifiableCallout<HeadMarkerEvent> tealAct2 = new ModifiableCallout<>("Teal Marker (Act 2)", "Teal");
	private final ModifiableCallout<HeadMarkerEvent> purpleAct2 = new ModifiableCallout<>("Purple Marker (Act 2)", "Purple");

	private final ModifiableCallout<HeadMarkerEvent> purpleAct4 = new ModifiableCallout<>("Purple Marker (Act 4)", "Purple");
	private final ModifiableCallout<HeadMarkerEvent> blueAct4 = new ModifiableCallout<>("Blue Marker (Act 4)", "Blue");

	private final ModifiableCallout<HeadMarkerEvent> red = new ModifiableCallout<>("Red Marker (Other)", "Red");
	private final ModifiableCallout<HeadMarkerEvent> teal = new ModifiableCallout<>("Teal Marker (Other)", "Teal");
	private final ModifiableCallout<HeadMarkerEvent> purple = new ModifiableCallout<>("Purple Marker (Other)", "Purple");
	private final ModifiableCallout<HeadMarkerEvent> blue = new ModifiableCallout<>("Blue Marker (Other)", "Blue");

	private final ModifiableCallout<Event> finaleOrder = new ModifiableCallout<>("Finale Order", "Soak tower {number}");
	private final ModifiableCallout<Event> finaleTower = new ModifiableCallout<>("Finale Tower", "Soak {towerspot} tower");

	private final ModifiableCallout<BuffApplied> curtainOrder = ModifiableCallout.durationBasedCall("Curtain Order", "{number} set");

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
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x6A09) {
				call = decollation;
			}
			else if (id == 0x69D8) {
				bloodrakeCounter++;
				call = bloodrake;
			}
			else if (id == 0x6A08) {
				call = evisceration;
			}
			else if (id == 0x6A2B) {
				call = heartStake;
			}
			else if (id == 0x6A2C) {
				call = ultimaImpulse;
			}
			else if (id == 0x6E78) {
				call = demigodDouble;
			}
			else if (id == 0x69D9) {
				beloneCoilsCounter++;
				beloneSuppress = false;
				call = belone;
			}
			else if (id == 0x6A1E) {
				call = hellsSting;
			}
			else {
				return;
			}
			context.accept(call.getModified(event, Map.of("target", event.getTarget())));
		}
	}

	private enum RoleCategory {
		TANK_HEALER("Tank/Healer"),
		DPS("DPS");

		private final String friendlyName;

		RoleCategory(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		public String getFriendlyName() {
			return friendlyName;
		}

		public RoleCategory opposite() {
			return this == TANK_HEALER ? DPS : TANK_HEALER;
		}
	}

	private RoleCategory tetherRole;
	private RoleCategory rotRole;
	private int bloodrakeCounter;
	private int beloneCoilsCounter;
	private boolean beloneSuppress;

	private RoleCategory playerRole() {
		return roleForCombatant(state.getPlayer());
	}

	private static RoleCategory roleForCombatant(XivPlayerCharacter pc) {
		return pc.getJob().isDps() ? RoleCategory.DPS : RoleCategory.TANK_HEALER;
	}

	@HandleEvents
	public void bloodrakeHandler(EventContext context, AbilityUsedEvent event) {
		if (event.getSource().getType() == CombatantType.NPC
				&& event.getAbility().getId() == 0x69D8
				&& bloodrakeCounter <= 2
				&& event.getTargetIndex() == 0
				&& event.getTarget() instanceof XivPlayerCharacter targetPlayer) {
			RoleCategory rakedRole = roleForCombatant(targetPlayer);
			if (bloodrakeCounter == 1) {
				tetherRole = rakedRole.opposite();
				// TODO call? or not useful?
			}
			else if (bloodrakeCounter == 2) {
				rotRole = rakedRole.opposite();
				if (tetherRole == null || rotRole == null) {
					log.error("Error in roles: Expected both roles to be non-null but got: {}/{}", tetherRole, rotRole);
					return;
				}
				RoleCategory playerRole = playerRole();
				boolean playerTether = playerRole == tetherRole;
				boolean playerRot = playerRole == rotRole;
				final ModifiableCallout<Event> call;
				if (playerRot) {
					call = playerTether ? tethersAndRot : rot;
				}
				else {
					call = playerTether ? tethers : neither;
				}
				context.accept(call.getModified(event));
			}
			else {
				log.error("Unexpected bloodrakeCounter: {}", bloodrakeCounter);
			}
		}
	}

	@HandleEvents
	public void beloneHandler(EventContext context, AbilityCastStart event) {
		if (beloneSuppress) {
			return;
		}
		long id = event.getAbility().getId();
		RoleCategory towerRole;
		if (id == 0x69DE) {
			towerRole = RoleCategory.DPS;
		}
		else if (id == 0x69DF) {
			towerRole = RoleCategory.TANK_HEALER;
		}
		else {
			return;
		}
		final ModifiableCallout<Event> call;
		beloneSuppress = true;
		RoleCategory pr = playerRole();
		if (beloneCoilsCounter == 1) {
			tetherRole = towerRole.opposite();
			call = pr == tetherRole ? tethers : towers;
		}
		else if (beloneCoilsCounter == 2) {
			rotRole = towerRole.opposite();
			boolean isTether = pr == tetherRole;
			if (pr == towerRole) {
				call = isTether ? towersAndTethers : towers;
			}
			else if (isTether) {
				call = tethersAndRot;
			}
			else {
				call = rot;
			}
		}
		else {
			log.error("Bad beloneCoilsCounter: {}", beloneCoilsCounter);
			return;
		}
		context.accept(call.getModified(event));
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
			context.accept(shift.getModified(event, Map.of("direction", where.getFriendlyName(), "cleavekb", isSword ? "Cleave" : "Knockback")));
		}
	}

	private AbilityCastStart pinaxStackSpreadTmp;
	private AbilityCastStart pinaxKnockbackProxTmp;

	@HandleEvents
	public void clear(EventContext context, PullStartedEvent newPull) {
		pinaxStackSpreadTmp = null;
		pinaxKnockbackProxTmp = null;
		bloodrakeCounter = 0;
		beloneCoilsCounter = 0;
		tetherRole = null;
		rotRole = null;
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
		context.accept(pinax.getModified(event, args));
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
			context.accept(acting.getModified(event, Map.of("role", role)));
		}
	}

	@HandleEvents
	public void periaktoi(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		final String safespot;
		switch (id) {
			case 0x69F5 -> safespot = "Acid";
			case 0x69F6 -> safespot = "Fire";
			case 0x69F7 -> safespot = "Water";
			case 0x69F8 -> safespot = "Lightning";
			default -> {
				return;
			}
		}
		context.accept(periaktoi.getModified(event, Map.of("element", safespot, "safespot", arenaPos.forCombatant(event.getSource()).getFriendlyName())));
	}

	private boolean isPhase2;

	// Resetting HM counter on phase 2 since there are other HMs in p1
	@HandleEvents
	public void searing(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x6A2D) {
			context.accept(searing.getModified(event));
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
		ModifiableCallout<HeadMarkerEvent> call = switch (currentAct) {
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
			context.accept(call.getModified(event));
		}
	}

	private TetherEvent lastTether;

	@SuppressWarnings("FloatingPointEquality")
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
			final ModifiableCallout<AbilityCastStart> call;
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
			context.accept(call.getModified(event));
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
				context.accept(finaleOrder.getModified(event, Map.of("number", fleetingImpulseDebuffCount)));
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
				context.accept(finaleTower.getModified(tether, Map.of("towerspot", arenaSector.getFriendlyName())));
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
			context.accept(curtainOrder.getModified(event, Map.of("number", number)));
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
