package gg.xp.reevent.context;

/**
 * Marker interface for state objects
 * <p>
 * Note that state objects should be thread-safe if it is expected that they might be updated from different places,
 * unless all of said places would be event handlers (since the event pump would process them serially).
 */
public interface SubState {
}
