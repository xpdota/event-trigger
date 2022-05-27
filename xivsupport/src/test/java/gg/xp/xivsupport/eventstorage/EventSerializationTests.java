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

	@Ignore // This file had the compression bug
	@Test
	public void readCompressedSampleFile3() {
		List<Event> events = EventReader.readEventsFromResource("/testsession3.oos.gz");
		Assert.assertEquals(events.size(), 25790);
	}

	@Ignore
	@Test
	public void readCompressedSampleFile4() {
		List<Event> events = EventReader.readEventsFromResource("/testsession4.oos.gz");
		Assert.assertEquals(events.size(), 498);
	}

	@Ignore
	@Test
	public void readCompressedSampleFile5() {
		List<Event> events = EventReader.readEventsFromResource("/testsession5.oos.gz");
		Assert.assertEquals(events.size(), 13232);
	}

}
