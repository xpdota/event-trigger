package gg.xp.reevent.events;

public interface EventQueue {

	void push(Event event);

	Event pull();

	int pendingSize();
}
