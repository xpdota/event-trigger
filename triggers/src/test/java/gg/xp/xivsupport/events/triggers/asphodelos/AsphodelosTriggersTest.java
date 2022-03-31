package gg.xp.xivsupport.events.triggers.asphodelos;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
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

public class AsphodelosTriggersTest {

	private record CalloutInitialValues(long ms, String tts, String text) {
	}

	@Test
	void testAsphodelosTriggers1() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		ReplayController replayController = new ReplayController(pico.getComponent(EventMaster.class), EventReader.readActLogResource("/asphodelos1.log"), false);
		pico.addComponent(replayController);
		pico.addComponent(FakeACTTimeSource.class);

		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);

//		pico.addComponent(coll);


		List<CalloutInitialValues> actualCalls = new ArrayList<>();

		pico.getComponent(EventDistributor.class).registerHandler(CalloutEvent.class, (ctx, e) -> {
			PullTracker pulls = pico.getComponent(PullTracker.class);
			long msDelta;
			Event combatStart = pulls.getCurrentPull().getCombatStart();
			if (combatStart == null) {
				msDelta = -1;
			}
			else {
				msDelta = Duration.between(combatStart.getHappenedAt(), ((BaseEvent) e).getEffectiveHappenedAt()).toMillis();
			}
			actualCalls.add(new CalloutInitialValues(msDelta, e.getCallText(), e.getVisualText()));
		});


		replayController.advanceBy(Integer.MAX_VALUE);
//		replayController.advanceBy(50_000);

		pico.getComponent(EventMaster.class).getQueue().waitDrain();


		compareLists(actualCalls, expectedCalls);

	}

	private CalloutInitialValues call(long when, String tts, String text) {
		return new CalloutInitialValues(when, tts, text);
	}

	private CalloutInitialValues callAppend(long when, String tts, String appendText) {
		return new CalloutInitialValues(when, tts, tts + ' ' + appendText);
	}

	private CalloutInitialValues call(long when, String both) {
		return new CalloutInitialValues(when, both, both);
	}

	private final List<CalloutInitialValues> expectedCalls = List.of(
			// P1S
			callAppend(8_957, "Tankbuster", "(4.7)"),
			callAppend(19_925, "Shackles", "(2.7)"),
			callAppend(23_750, "Out", "(13.0)"),
			callAppend(27_050, "Raidwide", "(4.7)"),
			callAppend(46_354, "Out then In", "(11.2)"),
			callAppend(67_166, "Buster, Knockback, Flare", "(4.7)"),
			callAppend(79_684, "Left to Right", "(11.2)"),
			callAppend(100_453, "Raidwide", "(4.7)"),
			callAppend(124_569, "Bottom First", "(9.7)"),
			callAppend(136_742, "Raidwide", "(4.7)"),
			callAppend(146_901, "Raidwide", "(4.7)"),
			callAppend(163_110, "Buster, Knockback, Stack", "(4.7)"),
			callAppend(181_833, "Raidwide", "(6.7)"),
			call(197_697, "Fire Safe"),
			callAppend(219_584, "Buster, Knockback, Stack", "(4.7)"),
			call(232_993, "Light Safe"),
			callAppend(256_561, "Stack with Party", "(14.0)"),
			callAppend(260_880, "Tankbuster", "(4.7)"),
			callAppend(272_092, "Raidwide", "(6.7)"),
			callAppend(295_753, "Out 4", "(18.0)"),
			callAppend(324_083, "Raidwide", "(4.7)"),
			callAppend(348_047, "Top First", "(9.7)"),
			callAppend(362_214, "Out then In", "(11.2)"),
			callAppend(384_988, "Raidwide", "(4.7)"),
			callAppend(401_211, "Raidwide", "(6.7)"),
			callAppend(419_259, "Stack with Party", "(14.0)"),
			callAppend(420_595, "Buster, Knockback, Flare", "(4.7)"),


			// P2S
			callAppend(10_867, "Raidwide", "(4.7)"),
			callAppend(33_592, "Raidwide", "(4.7)"),
			callAppend(53558, "Go Behind Head", "(7.7)"),
			callAppend(92988, "Raidwide", "(4.7)"),
			callAppend(134_744, "Stack on YOU", "(23.0)"),
			callAppend(143_256, "Go In Front of Head", "(7.7)"),
			callAppend(171_953, "Short East Push", "(13.0)"),
			callAppend(206_354, "Raidwide", "(4.7)"),
			callAppend(223_170, "Raidwide", "(4.7)"),
			call(250_120, "Blue 3"),
			callAppend(285_314, "Raidwide", "(4.7)"),
			callAppend(304_875, "Short West Push", "(13.0)"),
			callAppend(328_532, "Spread", "(4.7)"),
			callAppend(336682, "Go Behind Head", "(7.7)"),
			callAppend(364855, "Stack on WTM Main", "(23.0)"),
			callAppend(380279, "Stack on WTM Main", "(23.0)"),
	);

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
