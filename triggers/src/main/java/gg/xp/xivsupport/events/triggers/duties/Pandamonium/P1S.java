package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@CalloutRepo("P1S")
public class P1S implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P1S.class);

	private final ModifiableCallout wardensWrath = new ModifiableCallout("Warden's Wrath", "Raidwide");
	private final ModifiableCallout shiningCells = new ModifiableCallout("Shining Cells", "Raidwide");
	private final ModifiableCallout slamShut = new ModifiableCallout("Slam Shut", "Raidwide");

	private final ModifiableCallout leftToRight = new ModifiableCallout("Flail: Left, Right", "Left to Right");
	private final ModifiableCallout rightToLeft = new ModifiableCallout("Flail: Right, Left", "Right to Left");
	private final ModifiableCallout outThenIn = new ModifiableCallout("Flail: Out, In", "Out then In");
	private final ModifiableCallout inThenOut = new ModifiableCallout("Flail: In, Out", "In then Out");

	private final ModifiableCallout heavyHand = new ModifiableCallout("Heavy Hand", "Tankbuster");
	private final ModifiableCallout flailOfGrace = new ModifiableCallout("Pitiless Flail of Grace", "Buster, Knockback, Stack");
	private final ModifiableCallout flailOfPurgation = new ModifiableCallout("Pitiless Flail of Purgation", "Buster, Knockback, Flare");

	private final ModifiableCallout shacklesOfTime_you = new ModifiableCallout("Shackles of Time on You", "Opposite Color from Party");
	private final ModifiableCallout shacklesOfTime_notYou = new ModifiableCallout("Shackles of Time - Not You", "Stack with Party");

	private final ModifiableCallout shacklesPrep = new ModifiableCallout("Shackles Prep", "Shackles");

	private final ModifiableCallout shacklesRedGeneric = new ModifiableCallout("Red Shackle", "Out");
	private final ModifiableCallout shacklesPurpleGeneric = new ModifiableCallout("Purple Shackle", "In");

	private final ModifiableCallout shacklesRed3 = new ModifiableCallout("Red Shackle 1 (3s)", "Out 1");
	private final ModifiableCallout shacklesRed8 = new ModifiableCallout("Red Shackle 2 (8s)", "Out 2");
	private final ModifiableCallout shacklesRed13 = new ModifiableCallout("Red Shackle 3 (13s)", "Out 3");
	private final ModifiableCallout shacklesRed18 = new ModifiableCallout("Red Shackle 4 (18s)", "Out 4");
	private final ModifiableCallout shacklesPurp3 = new ModifiableCallout("Purple Shackle 1 (3s)", "In 1");
	private final ModifiableCallout shacklesPurp8 = new ModifiableCallout("Purple Shackle 2 (8s)", "In 2");
	private final ModifiableCallout shacklesPurp13 = new ModifiableCallout("Purple Shackle 3 (13s)", "In 3");
	private final ModifiableCallout shacklesPurp18 = new ModifiableCallout("Purple Shackle 4 (18s)", "In 4");

	private final ModifiableCallout greenSafe = new ModifiableCallout("Green/Light Safe", "Light Safe");
	private final ModifiableCallout redSafe = new ModifiableCallout("Red/Fire Safe", "Fire Safe");

	@HandleEvents
	public void shackles(EventContext context, BuffApplied buff) {
		if (!buff.getTarget().isThePlayer()) {
			return;
		}
		final ModifiableCallout call;
		long id = buff.getBuff().getId();
		if (id == 0xAB6) {
			call = shacklesPurpleGeneric;
		}
		else if (id == 0xAB7) {
			call = shacklesRedGeneric;
		}
		else if (id == 0xB45) {
			call = shacklesPurp3;
		}
		else if (id == 0xB46) {
			call = shacklesPurp8;
		}
		else if (id == 0xB47) {
			call = shacklesPurp13;
		}
		else if (id == 0xB6B) {
			call = shacklesPurp18;
		}
		else if (id == 0xB48) {
			call = shacklesRed3;
		}
		else if (id == 0xB49) {
			call = shacklesRed8;
		}
		else if (id == 0xB4A) {
			call = shacklesRed13;
		}
		else if (id == 0xB6C) {
			call = shacklesRed18;
		}
		else {
			return;
		}
		context.accept(call.getModified(buff));
	}

	@HandleEvents
	public void shacklesOfTime(EventContext context, BuffApplied buff) {
		if (buff.getBuff().getId() == 0xAB5) {
			if (buff.getTarget().isThePlayer()) {
				context.accept(shacklesOfTime_you.getModified(buff));
			}
			else {
				context.accept(shacklesOfTime_notYou.getModified(buff, Map.of("target", buff.getTarget())));
			}
		}
	}

	@HandleEvents
	public void redGreenSafeSpot(EventContext context, BuffApplied buff) {
		if (buff.getTarget().getType() == CombatantType.NPC && buff.getBuff().getId() == 0x893) {
			if (buff.getRawStacks() == 0x14C) {
				context.accept(greenSafe.getModified(buff));
			}
			else {
				context.accept(redSafe.getModified(buff));
			}
		}
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout call;
			if (id == 0x662A) {
				call = wardensWrath;
			}
			else if (id == 0x6616) {
				call = shiningCells;
			}
			else if (id == 0x6617) {
				call = slamShut;
			}
			else if (id == 0x65F6) {
				call = leftToRight;
			}
			else if (id == 0x65F7) {
				call = rightToLeft;
			}
			else if (id == 0x65F8 || id == 0x65F9) {
				call = outThenIn;
			}
			else if (id == 0x65FA || id == 0x65FB) {
				call = inThenOut;
			}
			else if (id == 0x6629) {
				call = heavyHand;
			}
			else if (id == 0x660E) {
				call = flailOfGrace;
			}
			else if (id == 0x660F) {
				call = flailOfPurgation;
			}
			else if (id == 0x6625) {
				call = shacklesPrep;
			}
			else {
				return;
			}
			context.accept(call.getModified(event));
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3EB);
	}

}
