package gg.xp.events.delaytest;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.debug.DebugCommand;
import gg.xp.events.misc.EchoEvent;
import gg.xp.scan.HandleEvents;
import gg.xp.speech.TtsCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for how to make a "after A, wait X seconds then do Y, unless Z happened in the meantime" kind of trigger
 *
 * This implementation supports the typical usage case for a DoT tracker, though a proper one would track per-enemy.
 * It will do the callout 5 seconds after the most recent invocation, unless cancelled since then.
 * i.e. refreshing the dot will reset the countdown. Cancelling with stop the countdown.
 *
 * The way this is accomplished is by keeping track of our most-recently-submitted DelayedEvent, and simply checking
 * the incoming event against that one. It would probably be more forward-looking to use a field on the event for this,
 * but this example uses == for simplicity. The 'cancel' command simply nulls out the field, so the incoming event
 * will never pass the check.
 */
public class DelayedTest {

	private static final Logger log = LoggerFactory.getLogger(DelayedTest.class);

	private static final class DelayedTestEvent extends BaseDelayedEvent {
		private DelayedTestEvent(long delay) {
			super(delay);
		}
	}

	private volatile DelayedTestEvent pending;

	@HandleEvents
	public void handleStart(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("delaystart")) {
			log.info("Delay test start");
			DelayedTestEvent outgoingEvent = new DelayedTestEvent(5000);
			context.enqueue(outgoingEvent);
			pending = outgoingEvent;
		}
	}

	@HandleEvents
	public void handleEnd(EventContext<Event> context, DelayedTestEvent event) {
		log.info("Delay test end");
		if (pending == event) {
			context.accept(new TtsCall("Foo"));
		}
	}

	@HandleEvents
	public void handleCancel(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("delaycancel")) {
			log.info("Delay test cancel");
			pending = null;
		}
	}


}
