package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "P5S", duty = KnownDuty.P5S)
public class P5S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P5S.class);
	private final ModifiableCallout<AbilityCastStart> searingRay = ModifiableCallout.durationBasedCall("Searing Ray", "Behind");
	private final ModifiableCallout<AbilityCastStart> searingRayReflected = ModifiableCallout.durationBasedCall("Searing Ray Reflected", "Front");
	private final ModifiableCallout<AbilityCastStart> rubyGlow = ModifiableCallout.durationBasedCall("Ruby Glow", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> crunch = ModifiableCallout.durationBasedCall("Crunch", "Tankbuster on {event.target}");
	private final ModifiableCallout<AbilityCastStart> sonicHowl = ModifiableCallout.durationBasedCall("Sonic Howl", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> toxicCrunch = ModifiableCallout.durationBasedCall("Toxic Crunch", "Tankbuster on {event.target}");
	private final ModifiableCallout<AbilityCastStart> venomPool = ModifiableCallout.durationBasedCall("Venom Pool", "Stack");
	private final ModifiableCallout<AbilityCastStart> venomRain = ModifiableCallout.durationBasedCall("Venom Rain", "Spread");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P5S(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43A);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		if (id == 0x0)
			call = searingRay;
		else if (id == 0x0)
			call = searingRayReflected;
		else if(id == 0x0)
			call = rubyGlow;
		else if(id == 0x0)
			call = crunch;
		else if(id == 0x0)
			call = sonicHowl;
		else if(id == 0x0)
			call = toxicCrunch;
		else if(id == 0x0)
			call = venomPool;
		else if(id == 0x0 && event.getTarget().isThePlayer())
			call = venomRain;
		else
			return;
		context.accept(call.getModified(event));
	}
}
