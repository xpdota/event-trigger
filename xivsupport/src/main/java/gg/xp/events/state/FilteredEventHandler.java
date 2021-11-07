package gg.xp.events.state;

import gg.xp.events.Event;
import gg.xp.events.EventContext;

public interface FilteredEventHandler {

	boolean enabled(EventContext<Event> context);

}
