package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;

public final class StressTest {

	@HandleEvents
	public static void handle(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("stresstest")) {
			for (int i = 0; i < 1000; i++) {
				context.enqueue(new RefreshCombatantsRequest());
			}
		}
	}
}
