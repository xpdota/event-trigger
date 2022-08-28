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
import gg.xp.xivsupport.models.CombatantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@CalloutRepo(name = "P5", duty = KnownDuty.P5)
public class P5 extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P5.class);
	private final ModifiableCallout<AbilityCastStart> searingRay = ModifiableCallout.durationBasedCall("Searing Ray", "Behind");
	private final ModifiableCallout<AbilityCastStart> searingRayReflected = ModifiableCallout.durationBasedCall("Searing Ray Reflected", "Front");
	private final ModifiableCallout<AbilityCastStart> rubyGlow = ModifiableCallout.durationBasedCall("Ruby Glow", "raidwide");
	private final ModifiableCallout<AbilityCastStart> crunch = ModifiableCallout.durationBasedCall("Crunch", "buster");
	private final ModifiableCallout<AbilityCastStart> sonicHowl = ModifiableCallout.durationBasedCall("Sonic Howl", "raidwide");
	private final ModifiableCallout<AbilityCastStart> toxicCrunch = ModifiableCallout.durationBasedCall("Toxic Crunch", "tankbuster");
	private final ModifiableCallout<AbilityCastStart> venomPool = ModifiableCallout.durationBasedCall("Venom Pool", "stack");
	private final ModifiableCallout<AbilityCastStart> venomRain = ModifiableCallout.durationBasedCall("Venom Rain", "spread");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P5(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x439);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		//if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x76D7)
				call = searingRay;
			else if (id == 0x76D8)
				call = searingRayReflected;
			else if(id == 0x76D4)
				call = rubyGlow;
			else if(id == 0x76F0)
				call = crunch;
			else if(id == 0x76F2)
				call = sonicHowl;
			else if(id == 0x76F1)
				call = toxicCrunch;
			else if(id == 0x76EF)
				call = venomPool;
			else if(id == 0x76ED && event.getTarget().isThePlayer())
				call = venomRain;
			else
				return;
			context.accept(call.getModified(event));
		//}
	}
}
