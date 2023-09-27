package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.regex.Pattern;


public class CustomTimelineEntrySerTest {

	@Test
	void testSerDes() {
		PersistenceProvider pers = new InMemoryMapPersistenceProvider();

		CustomTimelineEntry entry = new CustomTimelineEntry();
		entry.windowStart = 5.6;
		entry.windowEnd = 8.7;
		entry.time = 123.4;
		entry.name = "Tankbuster";
		entry.sync = Pattern.compile("21:.*FooBar");
		entry.jump = 135.7;
		// no jump, no duration
		TimelineCustomizations cust = new TimelineCustomizations();
		cust.setEntries(Collections.singletonList(entry));

		pers.save("My Timeline", cust);

		String asString = pers.get("My Timeline", String.class, "");

		Assert.assertEquals(asString, "{\"enabled\":true,\"entries\":[{\"time\":123.4,\"name\":\"Tankbuster\",\"sync\":\"21:.*FooBar\",\"timelineWindow\":{\"start\":5.6,\"end\":8.7},\"jump\":135.7}]}");

		TimelineCustomizations myTimeline = pers.get("My Timeline", TimelineCustomizations.class, null);
		CustomTimelineItem entrySaved = myTimeline.getEntries().get(0);

		Assert.assertEquals(entry, entrySaved);
	}

	@Test
	void testSerDesNewFields() {
		PersistenceProvider pers = new InMemoryMapPersistenceProvider();

		CustomTimelineEntry entry = new CustomTimelineEntry();
		entry.windowStart = 5.6;
		entry.windowEnd = 8.7;
		entry.time = 123.4;
		entry.name = "Tankbuster";
		entry.sync = Pattern.compile("21:.*FooBar");
		entry.jumpLabel = "foo";
		entry.forceJump = true;
		// no jump, no duration
		TimelineCustomizations cust = new TimelineCustomizations();
		cust.setEntries(Collections.singletonList(entry));

		pers.save("My Timeline", cust);

		String asString = pers.get("My Timeline", String.class, "");

		Assert.assertEquals(asString, "{\"enabled\":true,\"entries\":[{\"time\":123.4,\"name\":\"Tankbuster\",\"sync\":\"21:.*FooBar\",\"timelineWindow\":{\"start\":5.6,\"end\":8.7},\"jumpLabel\":\"foo\",\"forceJump\":true}]}");

		TimelineCustomizations myTimeline = pers.get("My Timeline", TimelineCustomizations.class, null);
		CustomTimelineItem entrySaved = myTimeline.getEntries().get(0);

		Assert.assertEquals(entry, entrySaved);
	}

	@Test
	void testSerDesWithUrl() throws MalformedURLException {
		PersistenceProvider pers = new InMemoryMapPersistenceProvider();

		CustomTimelineEntry entry = new CustomTimelineEntry();
		entry.windowStart = 5.6;
		entry.windowEnd = 8.7;
		entry.time = 123.4;
		entry.name = "Tankbuster";
		entry.sync = Pattern.compile("21:.*FooBar");
		entry.icon = new URL("http://foo.bar.com/baz.png");
		entry.duration = 5.0;
		entry.jump = 123.5;
		// no jump, no duration
		TimelineCustomizations cust = new TimelineCustomizations();
		cust.setEntries(Collections.singletonList(entry));

		pers.save("My Timeline", cust);

		String asString = pers.get("My Timeline", String.class, "");

		Assert.assertEquals(asString, "{\"enabled\":true,\"entries\":[{\"time\":123.4,\"name\":\"Tankbuster\",\"sync\":\"21:.*FooBar\",\"duration\":5.0,\"timelineWindow\":{\"start\":5.6,\"end\":8.7},\"jump\":123.5,\"icon\":\"http://foo.bar.com/baz.png\"}]}");

		TimelineCustomizations myTimeline = pers.get("My Timeline", TimelineCustomizations.class, null);
		CustomTimelineItem entrySaved = myTimeline.getEntries().get(0);

		Assert.assertEquals(entry, entrySaved);
	}


	@Test
	void testSerDesWithLabel() {
		PersistenceProvider pers = new InMemoryMapPersistenceProvider();

		CustomTimelineLabel entry = new CustomTimelineLabel();
		entry.time = 456.7;
		entry.name = "Phase 2";
		TimelineCustomizations cust = new TimelineCustomizations();
		cust.setEntries(Collections.singletonList(entry));

		pers.save("My Timeline", cust);

		String asString = pers.get("My Timeline", String.class, "");

		Assert.assertEquals(asString, "{\"enabled\":true,\"entries\":[{\"time\":456.7,\"name\":\"Phase 2\",\"label\":true}]}");

		TimelineCustomizations myTimeline = pers.get("My Timeline", TimelineCustomizations.class, null);
		CustomTimelineItem entrySaved = myTimeline.getEntries().get(0);

		Assert.assertEquals(entry, entrySaved);
	}
}