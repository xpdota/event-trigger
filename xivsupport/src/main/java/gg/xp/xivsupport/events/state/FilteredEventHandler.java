package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;

public interface FilteredEventHandler {

	boolean enabled(EventContext<Event> context);

}
