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

	private final ModifiableCallout<AbilityCastStart> wardensWrath = ModifiableCallout.durationBasedCall("Warden's Wrath", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> shiningCells = ModifiableCallout.durationBasedCall("Shining Cells", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> slamShut = ModifiableCallout.durationBasedCall("Slam Shut", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> topFirst = ModifiableCallout.durationBasedCall("Intemp Top First", "Top First");
	private final ModifiableCallout<AbilityCastStart> bottomFirst = ModifiableCallout.durationBasedCall("Intemp Bottom First", "Bottom First");

	private final ModifiableCallout<AbilityCastStart> leftToRight = ModifiableCallout.durationBasedCall("Flail: Left, Right", "Left to Right");
	private final ModifiableCallout<AbilityCastStart> rightToLeft = ModifiableCallout.durationBasedCall("Flail: Right, Left", "Right to Left");
	private final ModifiableCallout<AbilityCastStart> outThenIn = ModifiableCallout.durationBasedCall("Flail: Out, In", "Out then In");
	private final ModifiableCallout<AbilityCastStart> inThenOut = ModifiableCallout.durationBasedCall("Flail: In, Out", "In then Out");

	private final ModifiableCallout<AbilityCastStart> heavyHand = ModifiableCallout.durationBasedCall("Heavy Hand", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> flailOfGrace = ModifiableCallout.durationBasedCall("Pitiless Flail of Grace", "Buster, Knockback, Stack");
	private final ModifiableCallout<AbilityCastStart> flailOfPurgation = ModifiableCallout.durationBasedCall("Pitiless Flail of Purgation", "Buster, Knockback, Flare");

	private final ModifiableCallout<BuffApplied> shacklesOfTime_you = ModifiableCallout.durationBasedCall("Shackles of Time on You", "Opposite Color from Party");
	private final ModifiableCallout<BuffApplied> shacklesOfTime_notYou = ModifiableCallout.durationBasedCall("Shackles of Time - Not You", "Stack with Party");

	private final ModifiableCallout<AbilityCastStart> shacklesPrep = ModifiableCallout.durationBasedCall("Shackles Prep", "Shackles");

	private final ModifiableCallout<BuffApplied> shacklesRedGeneric = ModifiableCallout.durationBasedCall("Red Shackle", "Out");
	private final ModifiableCallout<BuffApplied> shacklesPurpleGeneric = ModifiableCallout.durationBasedCall("Purple Shackle", "In");

	private final ModifiableCallout<BuffApplied> shacklesRed3 = ModifiableCallout.durationBasedCall("Red Shackle 1 (3s)", "Out 1");
	private final ModifiableCallout<BuffApplied> shacklesRed8 = ModifiableCallout.durationBasedCall("Red Shackle 2 (8s)", "Out 2");
	private final ModifiableCallout<BuffApplied> shacklesRed13 = ModifiableCallout.durationBasedCall("Red Shackle 3 (13s)", "Out 3");
	private final ModifiableCallout<BuffApplied> shacklesRed18 = ModifiableCallout.durationBasedCall("Red Shackle 4 (18s)", "Out 4");
	private final ModifiableCallout<BuffApplied> shacklesPurp3 = ModifiableCallout.durationBasedCall("Purple Shackle 1 (3s)", "In 1");
	private final ModifiableCallout<BuffApplied> shacklesPurp8 = ModifiableCallout.durationBasedCall("Purple Shackle 2 (8s)", "In 2");
	private final ModifiableCallout<BuffApplied> shacklesPurp13 = ModifiableCallout.durationBasedCall("Purple Shackle 3 (13s)", "In 3");
	private final ModifiableCallout<BuffApplied> shacklesPurp18 = ModifiableCallout.durationBasedCall("Purple Shackle 4 (18s)", "In 4");

	// Specifically NOT making these duration-based
	private final ModifiableCallout<BuffApplied> greenSafe = new ModifiableCallout<>("Green/Light Safe", "Light Safe");
	private final ModifiableCallout<BuffApplied> redSafe = new ModifiableCallout<>("Red/Fire Safe", "Fire Safe");

	@HandleEvents
	public void shackles(EventContext context, BuffApplied buff) {
		if (!buff.getTarget().isThePlayer()) {
			return;
		}
		final ModifiableCallout<BuffApplied> call;
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
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x662A) {
				call = wardensWrath;
			}
			else if (id == 0x6616) {
				call = shiningCells;
			}
			else if (id == 0x6617) {
				call = slamShut;
			}
			else if (id == 0x661F) {
				call = bottomFirst;
			}
			else if (id == 0x6620) {
				call = topFirst;
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
