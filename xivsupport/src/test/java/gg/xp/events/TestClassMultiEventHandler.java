package gg.xp.events;

import gg.xp.scan.HandleEvents;

public class TestClassMultiEventHandler {

	private final int source = System.identityHashCode(this);

	@HandleEvents
	public void firstHandler(EventContext<Event> context, ACTLogLineEvent logLineEvent) {
		context.accept(new DiagEvent("Foo", source));
	}

	@HandleEvents
	public void secondHandler(EventContext<Event> context, ACTLogLineEvent logLineEvent) {
		context.accept(new DiagEvent("Bar", source));
	}
}
