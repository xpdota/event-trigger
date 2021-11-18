package gg.xp.reevent.events;

public interface EventHandler<X extends Event> {
	void handle(EventContext context, X event);

	default int getOrder() {
		return 0;
	}
}
