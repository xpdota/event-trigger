package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.gui.overlay.FlyingTextOverlay;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

public class CalloutExpiryTest {

	private final ModifiableCallout<AbilityCastStart> myCall = new ModifiableCallout<>("Test Call", "foobar", 3200);
	private EventDistributor dist;
	private FlyingTextOverlay fto;
	private Field ccField;
	private TestEventCollector tec;

	@BeforeTest
	void setup() throws NoSuchFieldException {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		dist = pico.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		fto = pico.getComponent(FlyingTextOverlay.class);
		ccField = FlyingTextOverlay.class.getDeclaredField("currentCallouts");
		ccField.setAccessible(true);
		tec = new TestEventCollector();
		dist.registerHandler(tec);
	}

	List<?> getCurrentVisualCalls() throws Throwable {
		return (List<?>) ccField.get(fto);
	}


	@Test
	void testCalloutExpiryFixedNoEvent() throws Throwable {
		{
			dist.acceptEvent(myCall.getModified());
			ProcessedCalloutEvent call = tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(3000);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(500);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.empty());
		}

		// Make sure re-use doesn't break anything
		tec.clear();
		{
			dist.acceptEvent(myCall.getModified());
			ProcessedCalloutEvent call = tec.getEventsOf(ProcessedCalloutEvent.class).stream().findAny().orElseThrow(() -> new RuntimeException("No callout!"));

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(3000);

			Assert.assertFalse(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.hasSize(1));

			Thread.sleep(500);

			Assert.assertTrue(call.isExpired());
			MatcherAssert.assertThat(getCurrentVisualCalls(), Matchers.empty());
		}
	}

}
