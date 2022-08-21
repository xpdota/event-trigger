package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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
		pico.addComponent(FakeTimeSource.class);

		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);

//		pico.addComponent(coll);


		List<CalloutInitialValues> actualCalls = new ArrayList<>();

		pico.getComponent(EventDistributor.class).registerHandler(CalloutEvent.class, (ctx, e) -> {
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
					msDelta = Duration.between(combatStart.getHappenedAt(), ((BaseEvent) e).getEffectiveHappenedAt()).toMillis();
				}
			}
			Event parent = e.getParent();
			if (parent instanceof RawModifiedCallout<?>) {
				parent = parent.getParent();
			}
			actualCalls.add(new CalloutInitialValues(msDelta, e.getCallText(), e.getVisualText(), parent));
		});


		// TODO: This causes issues with timing, since enqueued events don't have the proper timestamp
//		replayController.advanceByAsyncWhile(() -> true).get();
		replayController.advanceBy(Integer.MAX_VALUE);

		pico.getComponent(EventMaster.class).getQueue().waitDrain();


		compareLists(actualCalls, getExpectedCalls());


	}

	protected abstract List<CalloutInitialValues> getExpectedCalls();

	private static void compareLists(List<?> actual, List<?> expected) {
		int actSize = actual.size();
		int expSize = expected.size();
		int iterationSize = Math.max(actSize, expSize);
		boolean anyFailure = false;
		int firstFailureIndex = 0;
		int i;
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
		if (anyFailure) {
			throw new AssertionError(sb.toString());
		}
	}
}
