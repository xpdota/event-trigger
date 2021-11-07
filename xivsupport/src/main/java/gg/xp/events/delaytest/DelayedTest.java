package gg.xp.events.delaytest;

import gg.xp.events.BaseEvent;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
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

	private static final class DelayedEvent extends BaseEvent {
		private final long runAt;

		private DelayedEvent(long runAt) {
			this.runAt = runAt;
		}

		@Override
		public long delayedEnqueueAt() {
			return this.runAt;
		}

		@Override
		public boolean delayedEnqueueAtFront() {
			return true;
		}
	}

	private volatile DelayedEvent pending;

	@HandleEvents
	public void handleStart(EventContext<Event> context, EchoEvent event) {
		if (event.getLine().equals("delaystart")) {
			log.info("Delay test start");
			DelayedEvent outgoingEvent = new DelayedEvent(System.currentTimeMillis() + 5000);
			context.enqueue(outgoingEvent);
			pending = outgoingEvent;
		}
	}

	@HandleEvents
	public void handleEnd(EventContext<Event> context, DelayedEvent event) {
		log.info("Delay test end");
		if (pending == event) {
			context.accept(new TtsCall("Foo"));
		}
	}

	@HandleEvents
	public void handleCancel(EventContext<Event> context, EchoEvent event) {
		if (event.getLine().equals("delaycancel")) {
			log.info("Delay test cancel");
			pending = null;
		}
	}


}
