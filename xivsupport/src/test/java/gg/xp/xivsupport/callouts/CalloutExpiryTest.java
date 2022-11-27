package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.gui.overlay.FlyingTextOverlay;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

public class CalloutExpiryTest {

	// TODO: Finish the rest of these test cases

	private static final class Data {
		private final ModifiableCallout<DebugCommand> myCall = new ModifiableCallout<>("Test Call", "foobar", 1200);
		private final EventDistributor dist;
		private final FlyingTextOverlay fto;
		private final Field ccField;
		private final TestEventCollector tec;

		private Data() {
			MutablePicoContainer pico = XivMain.testingMasterInit();
			dist = pico.getComponent(EventDistributor.class);
			dist.acceptEvent(new InitEvent());
			fto = pico.getComponent(FlyingTextOverlay.class);
			try {
				ccField = FlyingTextOverlay.class.getDeclaredField("currentCallouts");
				ccField.setAccessible(true);
				tec = new TestEventCollector();
				dist.registerHandler(tec);
				// Invoke bug condition
				Thread.sleep(2000);
			}
			catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		// Sync is purely for thread-safety
		synchronized List<?> getCurrentVisualCalls() throws Throwable {
			return (List<?>) ccField.get(fto);
		}
	}


	@Test
	void testCalloutExpiryFixedNoEvent() throws Throwable {
		Data data = new Data();
		{
			data.dist.acceptEvent(data.myCall.getModified());
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(800);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(600);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}

		// Make sure re-use doesn't break anything
		data.tec.clear();
		{
			data.dist.acceptEvent(data.myCall.getModified());
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(800);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(600);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}
	}

	@Test
	void testCalloutExpiryFixedWithEvent() throws Throwable {
		Data data = new Data();
		{
			DebugCommand fooEvent = new DebugCommand("foo");
			data.dist.acceptEvent(fooEvent);
			data.dist.acceptEvent(data.myCall.getModified(fooEvent));
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(800);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(600);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}

		// Make sure re-use doesn't break anything
		data.tec.clear();
		{
			DebugCommand fooEvent = new DebugCommand("foo");
			data.dist.acceptEvent(data.myCall.getModified(fooEvent));
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(800);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(600);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}
	}

	@Test
	void testCalloutExpiryFixedWithFakeTimeSource() throws Throwable {
		Data data = new Data();
		FakeTimeSource fakeTimeSource = new FakeTimeSource();
		{
			DebugCommand fooEvent = new DebugCommand("foo");
			Instant basis = Instant.now();
			fakeTimeSource.setNewTime(basis);
			fooEvent.setTimeSource(fakeTimeSource);
			data.dist.acceptEvent(fooEvent);
			RawModifiedCallout<DebugCommand> rmc = data.myCall.getModified(fooEvent);
			rmc.setParent(fooEvent);
			data.dist.acceptEvent(rmc);
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			fakeTimeSource.setNewTime(basis.plusMillis(1000));
			// Give it time to react
			Thread.sleep(100);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			fakeTimeSource.setNewTime(basis.plusMillis(1500));
			Thread.sleep(100);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}

		// Make sure re-use doesn't break anything
		data.tec.clear();
		{
			DebugCommand fooEvent = new DebugCommand("foo");
			Instant basis = Instant.now();
			fakeTimeSource.setNewTime(basis);
			fooEvent.setTimeSource(fakeTimeSource);
			data.dist.acceptEvent(fooEvent);
			RawModifiedCallout<DebugCommand> rmc = data.myCall.getModified(fooEvent);
			rmc.setParent(fooEvent);
			data.dist.acceptEvent(rmc);
			ProcessedCalloutEvent call = data.tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			fakeTimeSource.setNewTime(basis.plusMillis(1000));
			// This time, let's try something else. Let's sleep for a while to make sure the fake time source is respected.
			Thread.sleep(5000);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.hasSize(1));

			fakeTimeSource.setNewTime(basis.plusMillis(1500));
			Thread.sleep(100);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(data.getCurrentVisualCalls(), Matchers.empty());
		}
	}

}
