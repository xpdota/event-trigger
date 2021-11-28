package gg.xp.xivsupport.eventstorage;

import gg.xp.reevent.events.Event;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.List;

public class EventSerializationTests {


	@Ignore // TODO fix these - event changes break them
	@Test
	public void readSampleFile() {
		List<Event> events = EventReader.readEventsFromResource("/testsession1.oos");
		Assert.assertEquals(events.size(), 30);
	}

	@Ignore // TODO fix these - event changes break them
	@Test
	public void readBigSampleFile() {
		List<Event> events = EventReader.readEventsFromResource("/testsession2.oos");
		Assert.assertEquals(events.size(), 644);
	}

}
