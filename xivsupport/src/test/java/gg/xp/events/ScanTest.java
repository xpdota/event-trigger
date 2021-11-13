package gg.xp.events;

import gg.xp.events.jails.FinalTitanJailsSolvedEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.scan.AutoHandlerScan;
import gg.xp.speech.CalloutEvent;
import gg.xp.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

public class ScanTest {
	@Test
	public void testAutoScan() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Some Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
//		Assert.assertEquals(collector.getEvents().size(), 2);
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Other Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
//		Assert.assertEquals(collector.getEvents().size(), 4);
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Third Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));

		List<Event> finalEvents = collector.getEvents();
		List<FinalTitanJailsSolvedEvent> collect = finalEvents.stream()
				.filter(FinalTitanJailsSolvedEvent.class::isInstance)
				.map(FinalTitanJailsSolvedEvent.class::cast)
				.collect(Collectors.toList());
		Assert.assertEquals(collect.size(), 1);
		List<XivEntity> jailedPlayers = collect.get(0).getJailedPlayers();

		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Other Player", "Some Player", "Third Player"));
	}

	@Test
	@Ignore // Manual test - listen for TTS
	public void testTTS() {
		EventDistributor dist = AutoHandlerScan.create();
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		dist.acceptEvent(new CalloutEvent("Foo Bar"));
		collector.getEvents();
	}

	@Test
	public void testMultiMethod() {
		EventDistributor dist = AutoHandlerScan.create();
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);
		dist.acceptEvent(new ACTLogLineEvent("Stuff"));

		Assert.assertEquals(collector.getEvents().size(), 3);
		List<DiagEvent> events = collector.getEventsOf(List.of(DiagEvent.class));
		Assert.assertEquals(events.size(), 2);
		Assert.assertEquals(events.get(0).getSource(), events.get(1).getSource());
	}
}
