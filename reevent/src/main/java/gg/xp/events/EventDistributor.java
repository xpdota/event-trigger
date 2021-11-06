package gg.xp.events;

import gg.xp.context.StateStore;

public interface EventDistributor<X extends Event> {

	/**
	 * Register event handler that will accept all event classes.
	 *
	 * @param handler The handler
	 */
	void registerHandler(EventHandler<X> handler);

	/**
	 * Register event handler and automatically filter to a particular class.
	 *
	 * @param clazz   Type of event
	 * @param handler The handler
	 * @param <Y>     Type of event
	 */
	default <Y extends X> void registerHandler(Class<Y> clazz, EventHandler<Y> handler) {
		registerHandler((context, event) -> {
			if (clazz.isInstance(event)) {
				handler.handle(context, (Y) event);
			}
		});
	}

	void acceptEvent(Event event);

	StateStore getStateStore();
}
