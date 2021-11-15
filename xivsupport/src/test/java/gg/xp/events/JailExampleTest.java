package gg.xp.events;

import gg.xp.events.actlines.events.AbilityUsedEvent;
import gg.xp.events.actlines.parsers.Line21Parser;
import gg.xp.events.jails.FinalTitanJailsSolvedEvent;
import gg.xp.events.jails.JailSolver;
import gg.xp.events.jails.UnsortedTitanJailsSolvedEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
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
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

//		// Register plugins
//		dist.registerHandler(ACTLogLineEvent.class, new Line21Parser()::handle);
//		JailSolver jailSolver = new JailSolver();
//		dist.registerHandler(AbilityUsedEvent.class, jailSolver
		List<Class<? extends BaseEvent>> eventsWeCareAbout = List.of(
				ACTLogLineEvent.class,
				AbilityUsedEvent.class,
				UnsortedTitanJailsSolvedEvent.class,
				FinalTitanJailsSolvedEvent.class
		);

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Some Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Other Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Third Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		finalEvents.forEach(e -> log.info("Seen event: {}", e));

		List<Class<?>> actualEventClasses = finalEvents.stream().map(Event::getClass).collect(Collectors.toList());
		Assert.assertEquals(actualEventClasses, List.of(
				// Raw log line + parsed into AbilityUsedEvent
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// Same
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// Same
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// This represents the three players that got jailed, in no particular order
				UnsortedTitanJailsSolvedEvent.class,
				// Finally, we have the three players that got jailed, but sorted in whatever order (currently just alphabetical)
				FinalTitanJailsSolvedEvent.class
		));
		// For debugging purposes (or maybe even production purposes, who knows), every synthetic event also has its
		// parent tagged onto it.
		List<ACTLogLineEvent> logLines = collector.getEventsOf(ACTLogLineEvent.class);
		List<AbilityUsedEvent> abilityUsedEvents = collector.getEventsOf(AbilityUsedEvent.class);
		MatcherAssert.assertThat(
				abilityUsedEvents.stream().map(Event::getParent).collect(Collectors.toList()),
				Matchers.equalTo(logLines));

		// Chain of provenance continues down the rest of the events
		UnsortedTitanJailsSolvedEvent unsortedEvent = collector.getEventsOf(UnsortedTitanJailsSolvedEvent.class).get(0);
		Assert.assertEquals(unsortedEvent.getParent(), abilityUsedEvents.get(2));
		FinalTitanJailsSolvedEvent sortedEvent = collector.getEventsOf(FinalTitanJailsSolvedEvent.class).get(0);
		Assert.assertEquals(sortedEvent.getParent(), unsortedEvent);


		// Last event should be sorted jails
		List<? extends XivEntity> jailedPlayers = sortedEvent.getJailedPlayers();
		// Assert correct sort order
		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Other Player", "Some Player", "Third Player"));
	}
}
