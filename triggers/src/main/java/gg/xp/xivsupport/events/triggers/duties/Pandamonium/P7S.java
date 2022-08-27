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

import java.util.Map;

@CalloutRepo(name = "P7S", duty = KnownDuty.P7S)
public class P7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P7S.class);
	//private final ModifiableCallout<AbilityCastStart> test = ModifiableCallout.durationBasedCall("Test", "testing");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P7S(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43E);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		/*ModifiableCallout<AbilityCastStart> call;
		if (id == 0x0)
			call = test;
		else
			return;

		context.accept(call.getModified(event, Map.of("target", event.getTarget())));*/
	}
}
