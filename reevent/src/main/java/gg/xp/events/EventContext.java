package gg.xp.events;

import gg.xp.context.StateStore;

// TODO: what should the generic type be here
public interface EventContext<X extends Event> {

	/**
	 * Accept an event for immediate processing
	 *
	 * @param event
	 */
	void accept(X event);

	/**
	 * Accept an event for processing as a normal queue event
	 *
	 * @param event
	 */
	void acceptToQueue(X event);

	/**
	 * Return an object conveying state information
	 *
	 * The expectation is that this would be a map of classes to instances of them, so that, e.g. anything related
	 * to a particular implementation can just attach it here, rather than having to plumb more generics through the
	 * entire system.
	 *
	 * @return
	 */
	StateStore getStateInfo();
}
