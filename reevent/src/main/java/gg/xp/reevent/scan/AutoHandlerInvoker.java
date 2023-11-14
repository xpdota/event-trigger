package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;

public interface AutoHandlerInvoker {

	boolean requiresContext();

	void handle(EventContext context, Event event);

}
