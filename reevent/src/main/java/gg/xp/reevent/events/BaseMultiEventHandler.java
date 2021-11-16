package gg.xp.reevent.events;

public class BaseMultiEventHandler {

	/*
	TODO
	Idea for this: when we have a situation where we want to wait for multiple events, be able to do
	something like this:
	// Handle in another thread
	@MultiEventHandler
	// Initial event is optional - you could omit it if, for example, if your initial condition
	// is multiple events
	void myMethod(SomeEvent initialEvent) {
		// wait fixed time
		sleep(5000);
		// Wait for another event
		OtherEvent secondEvent = waitEvent(
				// Event class
				OtherEvent.class,
				// Event condition
				e -> e.getSomething() == 5);
		// Wait again
		sleep(2000);
		List<FooEvent> otherEvents = waitEvents(
				// Event class
				FooEvent.class,
				// Event condition
				e -> e.isNice(),
				// How many of the event we'd like
				count -> count,
				// minimum timeout - will continue even if we haven't hit the minimum number
				5000
				// Maxmimum timeout - will abort if we hit this time even if we haven't hit the desired number
		queueEvent(new StuffEvent(otherEvents));
	}

	 */

}
