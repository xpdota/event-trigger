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
		registerHandler(new TypedEventHandler<>() {
			@Override
			public Class<? extends Event> getType() {
				return clazz;
			}

			@Override
			public void handle(EventContext context, Event event) {
				if (clazz.isInstance(event)) {
					handler.handle(context, clazz.cast(event));
				}
			}

			@Override
			public String toString() {
				return String.format("TypedHandler(%s:%s)", handler, clazz.getSimpleName());
			}
		});
	}

	void acceptEvent(Event event);

	void setQueue(EventQueue queue);

	StateStore getStateStore();
}
