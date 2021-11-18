package gg.xp.xivsupport.events.debug;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.reevent.scan.HandleEvents;

public class DebugEchoCommandHandler {

	private static final String commandPrefix = "c:";

	@HandleEvents
	public static void debugCmd(EventContext context, EchoEvent event) {
		if (event.getLine().startsWith(commandPrefix)) {
			context.enqueue(new DebugCommand(event.getLine().substring(commandPrefix.length())));
		}
	}
}
