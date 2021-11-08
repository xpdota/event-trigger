package gg.xp.events.debug;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.misc.EchoEvent;
import gg.xp.scan.HandleEvents;

public class DebugEchoCommandHandler {

	private static final String commandPrefix = "c:";

	@HandleEvents
	public static void debugCmd(EventContext<Event> context, EchoEvent event) {
		if (event.getLine().startsWith(commandPrefix)) {
			context.enqueue(new DebugCommand(event.getLine().substring(commandPrefix.length())));
		}
	}
}
