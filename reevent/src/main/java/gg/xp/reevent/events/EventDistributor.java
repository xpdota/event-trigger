package gg.xp.reevent.events;

import gg.xp.reevent.context.StateStore;

public interface EventDistributor {

	/**
	 * Register event handler that will accept all event classes.
	 *
	 * @param handler The handler
	 */
	void registerHandler(EventHandler<Event> handler);

	/**
	 * Register event handler and automatically filter to a particular class.
	 *
	 * @param clazz   Type of event
	 * @param handler The handler
	 * @param <Y>     Type of event
	 */
	default <Y extends Event> void registerHandler(Class<Y> clazz, EventHandler<? super Y> handler) {
		registerHandler((context, event) -> {
			if (clazz.isInstance(event)) {
				handler.handle(context, clazz.cast(event));
			}
		});
	}

	void acceptEvent(Event event);

	void setQueue(EventQueue queue);

	StateStore getStateStore();
}
