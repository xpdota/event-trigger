package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.ModifiedCalloutRepository;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.overlay.FlyingTextOverlay;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CalloutVerificationTest is an all-in-one base class for testing duty callouts. To use this, you need a log file.
 * The log file should be in the resources, and you specify the path in the {@link #getFileName()} method.
 * <p>
 * You also need to implement the {@link #getExpectedCalls()} method. If you have this method return an empty list,
 * and then run the test, the log output will contain the actual calls, formatted in the exact form that you would need
 * to specify the expected calls in. Thus, after checking to make sure they are correct, you can simply copy and paste.
 * <p>
 * There are also some optional methods to control some extra validations:
 * <ul>
 *     <li>{@link #getExpectedAms()} lets you specify expected automarker placements.</li>
 *     <li>{@link #configure(MutablePicoContainer)} lets you poke around in the DI container,
 *     in case you want to test alternative configurations or enable triggers which are disabled by default.</li>
 *     <li>{@link #failOnCalloutErrors()} can be overridden to return 'false' to disable the behavior of flagging any
 *     callout containing the string "Error" as being a failure.</li>
 *     <li>{@link #minimumMsBetweenCalls()} defaults to 1000 (ms) but can be overridden to a lower value. This value
 *     will flag callouts which happen in quick succession as failures, as this would often indicate callouts which are
 *     talking over one another.</li>
 * </ul>
 */
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

	protected AmVerificationValues mark(long when, MarkerSign marker, Job job) {
		return new AmVerificationValues(when, marker, job, null);
	}

	protected AmVerificationValues clearAll(long when) {
		return new AmVerificationValues(when, MarkerSign.CLEAR, Job.ADV, null);
	}

	protected void configure(MutablePicoContainer pico) {
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
		List<AmVerificationValues> actualMarks = new ArrayList<>();

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
					bde.setTimeSource(timeSource);
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
			if (StringUtils.isEmpty(e.getCallText()) && StringUtils.isBlank(e.getVisualText())) {
				return;
			}
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
					Instant happenedAt = timeSource.now();
					msDelta = Duration.between(combatStart.getHappenedAt(), happenedAt).toMillis();
				}
			}
			Event parent = e.getParent();
			if (parent instanceof RawModifiedCallout<?>) {
				parent = parent.getParent();
			}
			// Place where you can throw a breakpoint
//			if (msDelta > 1_000_000_000) {
//				int foo = 5 + 1;
//			}
			actualCalls.add(new CalloutInitialValues(msDelta, e.getCallText(), e.getVisualText(), parent));
		});
		dist.registerHandler(SpecificAutoMarkRequest.class, (ctx, e) -> {
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
					Instant happenedAt = timeSource.now();
					msDelta = Duration.between(combatStart.getHappenedAt(), happenedAt).toMillis();
				}
			}
			Event parent = e.getParent();
			actualMarks.add(new AmVerificationValues(msDelta, e.getMarker(), ((XivPlayerCharacter) e.getTarget()).getJob(), parent));
		});
		dist.registerHandler(AutoMarkRequest.class, (ctx, e) -> {
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
					Instant happenedAt = timeSource.now();
					msDelta = Duration.between(combatStart.getHappenedAt(), happenedAt).toMillis();
				}
			}
			Event parent = e.getParent();
			actualMarks.add(new AmVerificationValues(msDelta, MarkerSign.ATTACK1, ((XivPlayerCharacter) e.getTarget()).getJob(), parent));
		});
		dist.registerHandler(ClearAutoMarkRequest.class, (ctx, e) -> {
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
					Instant happenedAt = timeSource.now();
					msDelta = Duration.between(combatStart.getHappenedAt(), happenedAt).toMillis();
				}
			}
			Event parent = e.getParent();
			actualMarks.add(new AmVerificationValues(msDelta, MarkerSign.CLEAR, Job.ADV, parent));
		});

		// Make sure everything is initialized
		dist.acceptEvent(new InitEvent());
		// We have to "enable" the callout overlay or the callout processor won't bother with processing text calls
		// to save CPU.
		pico.getComponent(FlyingTextOverlay.class).getEnabled().set(true);
		// Force every callout enabled. Later, this could be amended to only enable needed stuff.
		pico.getComponent(ModifiedCalloutRepository.class)
				.getAllCallouts()
				.stream()
				.flatMap(group -> group.getCallouts().stream())
				.forEach(this::modifyCalloutSettings);

		replayController.advanceBy(1);
		configure(pico);
		replayController.advanceBy(Integer.MAX_VALUE);

		pico.getComponent(EventMaster.class).getQueue().waitDrain();


		List<CalloutInitialValues> expectedCalls = getExpectedCalls();
		if (expectedCalls.isEmpty()) {
			dump(actualCalls);
		}

		compareLists(rawStorage, actualCalls, expectedCalls);


		List<AmVerificationValues> expectedAMs = getExpectedAms();
		if (!actualMarks.isEmpty() || !expectedAMs.isEmpty()) {
			compareLists(rawStorage, actualMarks, expectedAMs);
		}


		List<String> assortedFailures = new ArrayList<>();

		for (CalloutInitialValues actualCall : actualCalls) {
			if (actualCall.text().endsWith("(NOW)")) {
				assortedFailures.add("Call [%s] ends with '(NOW)', indicating possible wrong event or late call".formatted(actualCall.toStringShort()));
			}
		}

		CalloutInitialValues last = null;
		long minDelta = minimumMsBetweenCalls();
		if (minDelta > 0) {

			for (CalloutInitialValues actualCall : actualCalls) {
				if (last != null) {
					long delta = actualCall.ms() - last.ms();
					// Negative delta happens for logs with multiple pulls, since the time resets to zero
					if (delta >= 0 && delta < minDelta) {
						assortedFailures.add("Call [%s] was too close (%dms) to call [%s]".formatted(actualCall.toStringShort(), delta, last.toStringShort()));
					}
				}
				last = actualCall;
			}
		}
		if (failOnCalloutErrors()) {
			for (CalloutInitialValues actualCall : actualCalls) {
				if (actualCall.tts().contains("Error") || actualCall.text().contains("Error")) {
					assortedFailures.add("Call [%s, %s] had an error".formatted(actualCall.tts(), actualCall.text()));
				}
			}
		}
		if (!assortedFailures.isEmpty()) {
			throw new AssertionError("Issues with callouts:\n" + String.join("\n", assortedFailures));
		}
	}

	protected long minimumMsBetweenCalls() {
		return 1000;
	}

	protected void modifyCalloutSettings(ModifiedCalloutHandle callout) {
		callout.getEnable().set(true);
	}

	protected abstract List<CalloutInitialValues> getExpectedCalls();

	protected List<AmVerificationValues> getExpectedAms() {
		return List.of();
	}

	protected boolean failOnCalloutErrors() {
		return true;
	}

	private static <X extends HasEvent> void compareLists(RawEventStorage rawStorage, List<X> actual, List<X> expected) {
		if (actual.isEmpty()) {
			throw new RuntimeException("Actual list was empty!");
		}
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
				try {
					X item = actual.get(i);
					failureAdjacentEvent = item.event();
				}
				catch (IndexOutOfBoundsException e) {
					// ignored
					firstFailureIndex = Math.min(actSize, expSize);
				}
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
			sb.append("\n\n").append("Events surrounding the failure (millisecond delta):\n\n");
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

	private static void dump(List<CalloutInitialValues> actualCalls) {
		throw new AssertionError("No expected calls were provided. Dumping actual calls instead.\n" + actualCalls.stream().map(Object::toString).map(s -> {
			String[] split = s.split("//");
			return split[0].trim();
		}).collect(Collectors.joining(",\n")));
	}
}
