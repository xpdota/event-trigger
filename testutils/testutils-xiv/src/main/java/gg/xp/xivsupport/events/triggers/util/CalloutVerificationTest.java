package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class CalloutVerificationTest {

	protected abstract String getFileName();

	protected CalloutInitialValues call(long when, String tts, String text) {
		return new CalloutInitialValues(when, tts, text, null);
	}

	protected CalloutInitialValues callAppend(long when, String tts, String appendText) {
		return new CalloutInitialValues(when, tts, tts + ' ' + appendText, null);
	}

	protected CalloutInitialValues call(long when, String both) {
		return new CalloutInitialValues(when, both, both, null);
	}

	@Test
	void doTheTest() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		String fileName = getFileName();
		ReplayController replayController = new ReplayController(pico.getComponent(EventMaster.class), EventReader.readActLogResource(fileName), false);
		pico.addComponent(replayController);
		pico.addComponent(FakeACTTimeSource.class);
		FakeACTTimeSource timeSource = pico.getComponent(FakeACTTimeSource.class);

		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);
		pico.addComponent(RawEventStorage.class);
		RawEventStorage rawStorage = pico.getComponent(RawEventStorage.class);
		rawStorage.getMaxEventsStoredSetting().set(2_000_000);


//		pico.addComponent(coll);


		List<CalloutInitialValues> actualCalls = new ArrayList<>();

		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(Event.class, new EventHandler<>() {
			@Override
			public void handle(EventContext ctx, Event e) {
				// This is a really ugly hack, but the alternative is reducing real world performance for
				// the sake of tests.
				if (e instanceof BaseDelayedEvent bde) {
					Event parent = bde.getParent();
					// This doesn't happen normally because the event is queued rather than accepted
					if (parent != null) {
						bde.setHappenedAt(parent.getHappenedAt());
					}
				}
				else if (e instanceof XivStateRecalculatedEvent ev) {
				/*
				The issue with these is more or less this:

				Normally, it works like this:
				Sequential trigger sees real event with proper time source
				SQ starts waiting for 100ms
				Recalc event happens very quickly after real event, and is ignored because we're still in the 100ms wait
				SQ finishes waiting, picks up next event (perhaps it's 250ms after the first event)

				But sometimes, what happens is:
				Sequential trigger sees real event with proper time source
				SQ starts waiting for 100ms
				Recalc event is slow
				SQ finishes waiting, receives the recalc event

				Funny how a *slowdown* ends up causing a *speedup*

				*/
				/*
				New hypothesis since that didn't do the trick: The DelayedSqtEvent might be the problem. Can this
				event just be completely discarded in tests?
				*/
					ev.setTimeSource(timeSource);
					ev.setHappenedAt(timeSource.now());
				}
			}

			@Override
			public int getOrder() {
				return -5000;
			}
		});
		dist.registerHandler(CalloutEvent.class, (ctx, e) -> {
			PullTracker pulls = pico.getComponent(PullTracker.class);
			final long msDelta;
			Pull currentPull = pulls.getCurrentPull();
			if (currentPull == null) {
				return;
			}
			else {
				Event combatStart = currentPull.getCombatStart();
				if (combatStart == null) {
					return;
				}
				else {
					Instant happenedAt;
					if (e instanceof XivStateRecalculatedEvent) {
						happenedAt = timeSource.now();
					}
					else {
						happenedAt = e.getEffectiveHappenedAt();
					}
					msDelta = Duration.between(combatStart.getHappenedAt(), happenedAt).toMillis();
				}
			}
			Event parent = e.getParent();
			if (parent instanceof RawModifiedCallout<?>) {
				parent = parent.getParent();
			}
			if (msDelta > 1_000_000_000) {
				int foo = 5 + 1;
			}
			actualCalls.add(new CalloutInitialValues(msDelta, e.getCallText(), e.getVisualText(), parent));
		});


		// TODO: This causes issues with timing, since enqueued events don't have the proper timestamp
//		replayController.advanceByAsyncWhile(() -> true).get();
		replayController.advanceBy(Integer.MAX_VALUE);

		pico.getComponent(EventMaster.class).getQueue().waitDrain();


		compareLists(rawStorage, actualCalls, getExpectedCalls());


	}

	protected abstract List<CalloutInitialValues> getExpectedCalls();

	private static void compareLists(RawEventStorage rawStorage, List<CalloutInitialValues> actual, List<CalloutInitialValues> expected) {
		int actSize = actual.size();
		int expSize = expected.size();
		int iterationSize = Math.max(actSize, expSize);
		boolean anyFailure = false;
		int firstFailureIndex = 0;
		int i;
		Event failureAdjacentEvent = null;
		for (i = 0; i < iterationSize; i++) {
			boolean equals;
			if (i >= actSize) {
				equals = false;
			}
			else if (i >= expSize) {
				equals = false;
			}
			else {
				equals = Objects.equals(actual.get(i), expected.get(i));
			}
			if (!equals) {
				anyFailure = true;
				firstFailureIndex = i;
				CalloutInitialValues item = actual.get(i);
				failureAdjacentEvent = item.event();
				break;
			}
		}
		StringBuilder sb = new StringBuilder("Data did not match, starting at index ").append(firstFailureIndex).append("\n\n").append("| Expected | Actual |").append('\n').append("---------------------").append('\n');
		for (; i < iterationSize; i++) {
			final String actualString;
			final String expectedString;
			if (i >= actSize) {
				actualString = "-- No Item --";
			}
			else {
				Object actualItem = actual.get(i);
				actualString = actualItem == null ? "-- null --" : actualItem.toString();
			}
			if (i >= expSize) {
				expectedString = "-- No Item --";
			}
			else {
				Object expectedItem = expected.get(i);
				expectedString = expectedItem == null ? "-- null --" : expectedItem.toString();
			}
			sb.append("| ").append(expectedString).append(" | ").append(actualString).append(" |\n");
		}
		if (failureAdjacentEvent != null) {
			sb.append("\n\n");
			List<Event> allEvents = rawStorage.getEvents();
			Instant timeBasis = failureAdjacentEvent.getEffectiveHappenedAt();
			int index = allEvents.indexOf(failureAdjacentEvent);
			int range = 20;
			int start = Math.max(0, index - range);
			int end = Math.min(allEvents.size(), index + range);
			List<Event> relevantEvents = allEvents.subList(start, end);
			Event failureEvent = failureAdjacentEvent;
			relevantEvents.forEach(event -> {
				Duration delta = Duration.between(timeBasis, event.getEffectiveHappenedAt());
				sb.append(delta.toMillis()).append(": ");
				if (event == failureEvent) {
					sb.append("****");
				}
				sb.append(event).append('\n');
			});

		}
		if (anyFailure) {
			throw new AssertionError(sb.toString());
		}
	}
}
