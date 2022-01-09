package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.CombatantType;
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

	private final ModifiableCallout red = new ModifiableCallout("Red Marker", "Red");
	private final ModifiableCallout purpleHealer = new ModifiableCallout("Purple Healer Marker", "Purple");
	private final ModifiableCallout blue = new ModifiableCallout("Blue Marker", "Blue");

	// TODO: tankbuster in/out, safe spots for act 1/2

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3F1);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
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

	@HandleEvents
	public void act2start(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x6EB4) {
			firstHeadmark = null;
			isPhase2 = true;
		}
	}

	@HandleEvents
	public void zoneChange(EventContext context, DutyCommenceEvent event) {
		isPhase2 = false;
	}

	private Long firstHeadmark;
	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}


	@HandleEvents
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		// TODO: tank with no tether
		ModifiableCallout call = switch (headmarkOffset) {
			case 0:
				yield red;
			case -1:
				yield purpleHealer;
			case -2:
				yield blue;
			default:
				yield null;
		};
		if (call != null) {
			context.accept(call.getModified());
		}
	}

	@HandleEvents
	public void act2headmark(EventContext context, HeadMarkerEvent event) {

	}

}
