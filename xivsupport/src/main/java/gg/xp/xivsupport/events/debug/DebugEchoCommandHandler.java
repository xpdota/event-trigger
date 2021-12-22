package gg.xp.xivsupport.events.debug;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.misc.EchoEvent;

public final class DebugEchoCommandHandler {

	private static final String commandPrefix = "c:";

	@HandleEvents
	public static void debugCmd(EventContext context, EchoEvent event) {
		if (event.getLine().startsWith(commandPrefix)) {
			context.enqueue(new DebugCommand(event.getLine().substring(commandPrefix.length())));
		}
	}
}
