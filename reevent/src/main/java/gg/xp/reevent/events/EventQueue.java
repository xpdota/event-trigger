package gg.xp.reevent.events;

public interface EventQueue {

	void push(Event event);

	Event pull();

	int pendingSize();

	// Should only be used for testing, or maybe hot reloads
	// TODO: problem here is that it waits for queue to be empty, but doesn't wait for current
	// event to be fully processed. This probably needs to be on EventMaster.
	void waitDrain();
}
