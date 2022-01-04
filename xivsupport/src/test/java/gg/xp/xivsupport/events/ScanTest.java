package gg.xp.xivsupport.events;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.List;

public class ScanTest {
	@Test
	public void testAutoScan() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Some Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		Assert.assertEquals(collector.getEventsOf(AbilityUsedEvent.class).size(), 1);
	}

	@Test
	@Ignore // Manual test - listen for TTS
	public void testTTS() {
		EventDistributor dist = XivMain.testingMasterInit().getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		dist.acceptEvent(new CalloutEvent("Foo Bar"));
		collector.getEvents();
	}

	@Test
	public void testMultiMethod() {
		EventDistributor dist = XivMain.testingMasterInit().getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		dist.acceptEvent(new ACTLogLineEvent("123|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan"));

		Assert.assertEquals(collector.getEvents().size(), 3);
		List<DiagEvent> events = collector.getEventsOf(List.of(DiagEvent.class));
		Assert.assertEquals(events.size(), 2);
		Assert.assertEquals(events.get(0).getSource(), events.get(1).getSource());
	}
}
