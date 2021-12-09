package gg.xp.reevent.scan;

import gg.xp.reevent.events.EventContext;

/**
 * TODO javadoc
 */
public interface FilteredEventHandler {

	boolean enabled(EventContext context);

}
