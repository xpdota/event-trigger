package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.List;

// TODO: Not really abstract anymore...
public class AbstractACTLineTest<X extends Event> {

	private static final Logger log = LoggerFactory.getLogger(AbstractACTLineTest.class);

	private final Class<X> eventClass;

	public AbstractACTLineTest(Class<X> eventClass) {
		this.eventClass = eventClass;
		log.info("AbstractACTLineTest for {}", eventClass.getSimpleName());
	}

	private TestEventCollector submitLine(String line) {
		// TODO: really need a way to assert there were no background errors in tests
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		ACTLogLineEvent event = new ACTLogLineEvent(line);
		dist.acceptEvent(event);
		return coll;
	}

	public X expectEvent(String line) {
		TestEventCollector coll = submitLine(line);
		List<X> events = coll.getEventsOf(eventClass);
		Assert.assertEquals(events.size(), 1, "Expected exactly one event");
		return events.get(0);
	}

	public void assertNoEvent(String line) {
		TestEventCollector coll = submitLine(line);
		Assert.assertTrue(coll.getEventsOf(eventClass).isEmpty(), "Expected to not find an event, but found one");
	}


}
