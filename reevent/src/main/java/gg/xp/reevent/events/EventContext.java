package gg.xp.reevent.events;

import gg.xp.reevent.context.StateStore;

public interface EventContext {

	/**
	 * Accept an event for immediate processing
	 *
	 * @param event
	 */
	void accept(Event event);

	/**
	 * Accept an event for processing as a normal queue event
	 *
	 * @param event
	 */
	void enqueue(Event event);

	/**
	 * Return an object conveying state information
	 * <p>
	 * The expectation is that this would be a map of classes to instances of them, so that, e.g. anything related
	 * to a particular implementation can just attach it here, rather than having to plumb more generics through the
	 * entire system.
	 *
	 * @return
	 */
	StateStore getStateInfo();
}
