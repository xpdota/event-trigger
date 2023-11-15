package gg.xp.xivsupport.events.actlines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
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
	}
}
