package gg.xp.events;

public interface EventQueue {

	void push(Event event);

	Event pull();

	int pendingSize();
}
