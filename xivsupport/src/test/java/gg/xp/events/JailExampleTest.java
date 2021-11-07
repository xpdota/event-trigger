package gg.xp.events;

import gg.xp.events.actlines.AbilityUsedEvent;
import gg.xp.events.jails.JailCollector;
import gg.xp.events.jails.JailSorter;
import gg.xp.events.jails.FinalTitanJailsSolvedEvent;
import gg.xp.events.jails.UnsortedTitanJailsSolvedEvent;
import gg.xp.events.models.XivEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

public class JailExampleTest {

	private static final Logger log = LoggerFactory.getLogger(JailExampleTest.class);

	/**
	 * End to end example for titan jails
	 */
	@Test
	public void jailTest() {
		// Test setup
		EventDistributor<Event> dist = new BasicEventDistributor();
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		// Register plugins
		dist.registerHandler(ACTLogLineEvent.class, new ACTLogLineParser());
		dist.registerHandler(AbilityUsedEvent.class, new JailCollector());
		dist.registerHandler(UnsortedTitanJailsSolvedEvent.class, new JailSorter());

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Some Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		Assert.assertEquals(collector.getEvents().size(), 2);
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Other Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		Assert.assertEquals(collector.getEvents().size(), 4);
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Third Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));

		List<Event> finalEvents = collector.getEvents();
		finalEvents.forEach(e -> log.info("Seen event: {}", e));

		List<Class<?>> actualEventClasses = finalEvents.stream().map(Event::getClass).collect(Collectors.toList());
		Assert.assertEquals(actualEventClasses, List.of(
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				UnsortedTitanJailsSolvedEvent.class,
				FinalTitanJailsSolvedEvent.class
		));
		// Last event should be sorted jails
		List<XivEntity> jailedPlayers = ((FinalTitanJailsSolvedEvent) finalEvents.get(finalEvents.size() - 1)).getJailedPlayers();
		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Other Player", "Some Player", "Third Player"));
	}
}
