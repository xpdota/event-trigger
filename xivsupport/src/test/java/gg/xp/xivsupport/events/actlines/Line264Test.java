package gg.xp.xivsupport.events.actlines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.SnapshotLocationDataEvent;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class Line264Test {

	@Test
	public void positiveHeadingOnlyTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|0.000|0.000|0.000|1.570|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertNull(event.getAnimationTarget());
	}

	@Test
	public void positiveHeadingOnly2Test() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|0||||1.570|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|0||||-1.589|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|0||||3.141|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertNull(event.getAnimationTarget());
	}

	@Test
	public void outOfOrderTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|0.000|0.000|0.000|1.570|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|711909381311a052";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertNull(event.getAnimationTarget());
	}

	@Test
	public void positionTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|1.000|2.000|3.000|1.570|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertEquals(event.getPos(), new Position(1.0, 2.0, 3.0, 1.570));
		Assert.assertNull(event.getHeadingOnly());
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertNull(event.getAnimationTarget());
	}

	@Test
	public void positiveHeadingOnlyNewTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|0.000|0.000|0.000|1.570|1234ABCD|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|1234ABCD|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|1234ABCD|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertEquals(event.getAnimationTarget().getId(), 0x1234ABCDL);
	}
	@Test
	public void positiveHeadingOnly2NewTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|0||||1.570|1234ABCD|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|0||||-1.589|1234ABCD|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|0||||3.141|1234ABCD|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertEquals(event.getAnimationTarget().getId(), 0x1234ABCDL);
	}

	@Test
	public void outOfOrderNewTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|0.000|0.000|0.000|1.570|1234ABCD|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|1234ABCD|711909381311a052";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|1234ABCD|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertNull(event.getPos());
		Assert.assertEquals(event.getHeadingOnly(), 1.570);
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertEquals(event.getAnimationTarget().getId(), 0x1234ABCDL);
	}

	@Test
	public void positionNewTest() {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		String castLine1 = "21|2023-11-04T17:53:45.2940000+01:00|4000619D|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-16.15|125.01|-10.00|1.57|00000662|0|0|b13a891c224580d2";
		String locationLine1 = "264|2023-11-04T17:53:45.2940000+01:00|4000619D|8BC0|00000662|1|1.000|2.000|3.000|1.570|1234ABCD|7e82907c710ee781";
		String castLine2 = "21|2023-11-04T17:53:45.2940000+01:00|4000619E|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||16.42|75.08|-10.00|-1.59|00000663|0|0|21ca6acb29c4082f";
		String locationLine2 = "264|2023-11-04T17:53:45.2940000+01:00|4000619E|8BC0|00000663|1|0.000|0.000|0.000|-1.589|1234ABCD|711909381311a052";
		String castLine3 = "21|2023-11-04T17:53:45.2940000+01:00|4000619F|Twister|8BC0|unknown_8bc0|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||-13.13|103.81|-10.00|3.14|00000664|0|0|3dd4dd4e468cd0a2";
		String locationLine3 = "264|2023-11-04T17:53:45.2940000+01:00|4000619F|8BC0|00000664|1|0.000|0.000|0.000|3.141|1234ABCD|fc680d77fb5897f1";
		// Intentionally submit out-of-order
		dist.acceptEvent(new ACTLogLineEvent(castLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine1));
		dist.acceptEvent(new ACTLogLineEvent(castLine3));
		dist.acceptEvent(new ACTLogLineEvent(locationLine2));
		dist.acceptEvent(new ACTLogLineEvent(locationLine3));
		List<SnapshotLocationDataEvent> events = coll.getEventsOf(SnapshotLocationDataEvent.class);
		SnapshotLocationDataEvent event = events.get(0);
		Assert.assertEquals(event.getSource().getId(), 0x4000619DL);
		Assert.assertEquals(event.getPos(), new Position(1.0, 2.0, 3.0, 1.570));
		Assert.assertNull(event.getHeadingOnly());
		Assert.assertEquals(event.getBestHeading(), 1.570);
		Assert.assertEquals(event.getAnimationTarget().getId(), 0x1234ABCDL);
	}
}
