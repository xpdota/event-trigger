package gg.xp.reevent.scan;

import gg.xp.reevent.events.EventContext;

/**
 * Indicates that for any event handlers annotated with {@link HandleEvents}, that the {@link #enabled(EventContext)}
 * method should be checked first. If it returns false, then the event handler is not called.
 * <p>
 * This is useful for making a set of triggers for a particular fight.
 */
public interface FilteredEventHandler {

	boolean enabled(EventContext context);

}
