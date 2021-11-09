package gg.xp.events.actlines;

import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.TestEventCollector;
import gg.xp.scan.AutoHandlerScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.List;

public class AbstractACTLineTest<X extends Event> {

	private static final Logger log = LoggerFactory.getLogger(AbstractACTLineTest.class);

	private final Class<X> eventClass;

	protected AbstractACTLineTest(Class<X> eventClass) {
		this.eventClass = eventClass;
		log.info("AbstractACTLineTest for {}", eventClass.getSimpleName());
	}

	private TestEventCollector submitLine(String line) {
		final EventDistributor dist;
		final TestEventCollector coll;
		dist = AutoHandlerScan.create();
		coll = new TestEventCollector();
		dist.registerHandler(coll);
		ACTLogLineEvent event = new ACTLogLineEvent(line);
		dist.acceptEvent(event);
		return coll;
	}

	protected X expectEvent(String line) {
		TestEventCollector coll = submitLine(line);
		List<X> events = coll.getEventsOf(eventClass);
		Assert.assertEquals(events.size(), 1);
		return events.get(0);
	}

	protected void assertNoEvent(String line) {
		TestEventCollector coll = submitLine(line);
		Assert.assertTrue(coll.getEventsOf(eventClass).isEmpty(), "Expected to not find an event, but found one");
	}


}
