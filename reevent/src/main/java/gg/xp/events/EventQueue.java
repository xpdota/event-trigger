package gg.xp.events;

public interface EventQueue<X extends Event> {

	void push(X event);

	X pull();

	int pendingSize();
}
