package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
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
		String stackSpread = event.pinaxKnockbackProx.getAbility().getId() == 0x69D4 ? "Spread" : "Light Parties";
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


}
