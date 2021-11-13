package gg.xp.events.misc;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.debug.DebugCommand;
import gg.xp.events.state.RefreshCombatantsRequest;
import gg.xp.scan.HandleEvents;

public class StressTest {

	@HandleEvents
	public static void handle(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("stresstest")) {
			for (int i = 0; i < 1000; i++) {
				context.enqueue(new RefreshCombatantsRequest());
			}
		}
	}
}
