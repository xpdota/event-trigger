package gg.xp.xivsupport.events.actlines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class Line263Test {

	@Test
	public void positiveTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "20|2023-11-04T17:53:44.8030000+01:00|4000619D|Twister|8BC0|unknown_8bc0|4000619D|Twister|0.200|-16.15|125.01|-10.00|1.57|ff3a8b7791718a94";
		String locationLine1 = "263|2023-11-04T17:53:44.8030000+01:00|4000619D|8BC0|-16.144|125.004|-10.010|1.570|268cde55a79a6001";
		String castLine2 = "20|2023-11-04T17:53:44.8030000+01:00|4000619E|Twister|8BC0|unknown_8bc0|4000619E|Twister|0.200|16.42|75.08|-10.00|-1.59|4df497ea3bba15e8";
		String locationLine2 = "263|2023-11-04T17:53:44.8030000+01:00|4000619E|8BC0|16.419|75.076|-10.010|-1.589|5c45536918449536";
		String castLine3 = "20|2023-11-04T17:53:44.8030000+01:00|4000619F|Twister|8BC0|unknown_8bc0|4000619F|Twister|0.200|-13.13|103.81|-10.00|3.14|0a50abb3d1380278";
		String locationLine3 = "263|2023-11-04T17:53:44.8030000+01:00|4000619F|8BC0|-13.123|103.794|-10.010|3.141|cb9e27d30e6a732f";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<CastLocationDataEvent> events = coll.getEventsOf(CastLocationDataEvent.class);
		CastLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertEquals(event.getPos().x(), -16.144);
		Assert.assertEquals(event.getPos().y(), 125.004);
		Assert.assertEquals(event.getPos().z(), -10.010);
		Assert.assertEquals(event.getPos().heading(), 1.57);
		Assert.assertNull(event.getHeadingOnly());
		// TODO: finish this
	}

	@Test
	public void testOutOfOrder() {
		// These lines can come out-of-order in this sense:
		// 1. Cast A
		// 2. Cast B
		// 3. Extra A
		// 4. Extra B
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String lines = """
				20|2024-07-31T17:18:09.3820000-05:00|400066A6|Brute Distortion|9B34|Lariat Combo|400066A6|Brute Distortion|5.800|100.00|85.00|0.00|0.00|e4ec9c3ed023ed70
				20|2024-07-31T17:18:09.3820000-05:00|400066A8|Brute Distortion|9B34|Lariat Combo|400066A8|Brute Distortion|5.800|85.00|100.00|0.00|1.57|930830aa0fddcd3f
				263|2024-07-31T17:18:09.3820000-05:00|400066A6|9B34|88.015|65.004|0.000|0.000|4fda2cc7f09b31e2
				263|2024-07-31T17:18:09.3820000-05:00|400066A8|9B34|65.004|112.003|0.000|1.571|3c597a02894ae464
				""";
		lines.lines().filter(s -> !s.isBlank()).forEach(line -> dist.acceptEvent(new ACTLogLineEvent(line)));
		List<AbilityCastStart> events = coll.getEventsOf(AbilityCastStart.class);
		var firstEvent = events.get(0);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().x(), 88.015);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().y(), 65.004);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().z(), 0.0);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().heading(), 0.0);
		var secondEvent = events.get(1);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().x(), 65.004);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().y(), 112.003);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().z(), 0.0);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().heading(), 1.571);

	}

	@Test
	public void testOutOfOrderMore() {
		// Turns out they can be even further out-of-order:
		// 1. Cast A
		// 2. Cast B
		// 3. Extra B
		// 4. Extra A
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String lines = """
				20|2024-07-31T17:18:09.3820000-05:00|400066A6|Brute Distortion|9B34|Lariat Combo|400066A6|Brute Distortion|5.800|100.00|85.00|0.00|0.00|e4ec9c3ed023ed70
				20|2024-07-31T17:18:09.3820000-05:00|400066A8|Brute Distortion|9B34|Lariat Combo|400066A8|Brute Distortion|5.800|85.00|100.00|0.00|1.57|930830aa0fddcd3f
				263|2024-07-31T17:18:09.3820000-05:00|400066A8|9B34|65.004|112.003|0.000|1.571|3c597a02894ae464
				263|2024-07-31T17:18:09.3820000-05:00|400066A6|9B34|88.015|65.004|0.000|0.000|4fda2cc7f09b31e2
				""";
		lines.lines().filter(s -> !s.isBlank()).forEach(line -> dist.acceptEvent(new ACTLogLineEvent(line)));
		List<AbilityCastStart> events = coll.getEventsOf(AbilityCastStart.class);
		var firstEvent = events.get(0);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().x(), 88.015);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().y(), 65.004);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().z(), 0.0);
		Assert.assertEquals(firstEvent.getLocationInfo().getPos().heading(), 0.0);
		var secondEvent = events.get(1);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().x(), 65.004);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().y(), 112.003);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().z(), 0.0);
		Assert.assertEquals(secondEvent.getLocationInfo().getPos().heading(), 1.571);

	}
}
