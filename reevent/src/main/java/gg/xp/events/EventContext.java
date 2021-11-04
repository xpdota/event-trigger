package gg.xp.events;

// TODO: what should the generic type be here
public interface EventContext<X extends Event> {

	void accept(X event);
}
