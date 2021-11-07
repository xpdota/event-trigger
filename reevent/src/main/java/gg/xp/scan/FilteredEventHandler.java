package gg.xp.scan;

import gg.xp.events.Event;
import gg.xp.events.EventContext;

public interface FilteredEventHandler {

	boolean enabled(EventContext<Event> context);

}
