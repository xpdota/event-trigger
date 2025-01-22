package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.timelines.icon.ActionTimelineIcon;
import gg.xp.xivsupport.timelines.icon.IconIdTimelineIcon;
import gg.xp.xivsupport.timelines.icon.StatusTimelineIcon;
import gg.xp.xivsupport.timelines.icon.TimelineIcon;
import gg.xp.xivsupport.timelines.icon.UrlTimelineIcon;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
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
		entry.iconSpec = new ActionTimelineIcon(80);
		entry.duration = 5.0;
		entry.jump = 123.5;
		// no jump, no duration
		TimelineCustomizations cust = new TimelineCustomizations();
		cust.setEntries(Collections.singletonList(entry));

		pers.save("My Timeline", cust);

		String asString = pers.get("My Timeline", String.class, "");

		Assert.assertEquals(asString, "{\"enabled\":true,\"entries\":[{\"time\":123.4,\"name\":\"Tankbuster\",\"sync\":\"21:.*FooBar\",\"duration\":5.0,\"timelineWindow\":{\"start\":5.6,\"end\":8.7},\"jump\":123.5,\"iconSpec\":{\"type\":\"action\",\"id\":80}}]}");

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

	@Test
	void testTimelineUrlIconSerDes() throws MalformedURLException {
		var mapper = new ObjectMapper();
		String theUrl = "https://foo.bar/asdf.png";
		TimelineIcon before = new UrlTimelineIcon(new URL(theUrl));
		MatcherAssert.assertThat(before.getIconUrl(), Matchers.equalTo(new URL(theUrl)));
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("url"));
		MatcherAssert.assertThat(asMap.get("url"), Matchers.equalTo(theUrl));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl(), Matchers.equalTo(new URL(theUrl)));
	}

	@Test
	void testTimelineActionIconSerDes() {
		var mapper = new ObjectMapper();
		// Teleport
		TimelineIcon before = new ActionTimelineIcon(5);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/000111_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("action"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(5L));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineActionIconSerDesInvalidId() {
		var mapper = new ObjectMapper();
		// Mount
		TimelineIcon before = new ActionTimelineIcon(4);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/000405_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("action"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(4L));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineStatusIconSerDesZeroStackCount() {
		var mapper = new ObjectMapper();
		// Berserk, up to 3 stacks
		TimelineIcon before = new StatusTimelineIcon(86, 0);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/217217_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("status"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(86L));
		MatcherAssert.assertThat(asMap.get("stacks"), Matchers.equalTo(0));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineStatusIconSerDesValidStackCount() {
		var mapper = new ObjectMapper();
		// Berserk, up to 3 stacks
		TimelineIcon before = new StatusTimelineIcon(86, 2);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/217218_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("status"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(86L));
		MatcherAssert.assertThat(asMap.get("stacks"), Matchers.equalTo(2));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineStatusIconSerDesInvalidStackCount() {
		var mapper = new ObjectMapper();
		// Berserk, up to 3 stacks
		TimelineIcon before = new StatusTimelineIcon(86, 5);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/217219_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("status"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(86L));
		MatcherAssert.assertThat(asMap.get("stacks"), Matchers.equalTo(5));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineStatusIconSerDesInvalidId() {
		var mapper = new ObjectMapper();
		// Mount
		TimelineIcon before = new StatusTimelineIcon(1473, 0);
		MatcherAssert.assertThat(before.getIconUrl(), Matchers.nullValue());
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("status"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(1473L));
		MatcherAssert.assertThat(asMap.get("stacks"), Matchers.equalTo(0));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl(), Matchers.nullValue());
	}

	@Test
	void testTimelineIconIdSerDesLocal() {
		var mapper = new ObjectMapper();
		// Berserk, up to 3 stacks
		TimelineIcon before = new IconIdTimelineIcon(103);
		Matcher<String> urlEnd = Matchers.endsWith("/xiv/icon/000103_hr1.png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("iconId"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(103));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}

	@Test
	void testTimelineIconIdSerDesXivApi() {
		var mapper = new ObjectMapper();
		// Berserk, up to 3 stacks
		TimelineIcon before = new IconIdTimelineIcon(51234);
		Matcher<String> urlEnd = Matchers.endsWith("asset/ui/icon/051000/051234_hr1.tex?format=png");
		MatcherAssert.assertThat(before.getIconUrl().toString(), urlEnd);
		Map<String, Object> asMap = mapper.convertValue(before, new TypeReference<>() {
		});
		MatcherAssert.assertThat(asMap.get("type"), Matchers.equalTo("iconId"));
		MatcherAssert.assertThat(asMap.get("id"), Matchers.equalTo(51234));
		TimelineIcon after = mapper.convertValue(asMap, new TypeReference<>() {
		});
		MatcherAssert.assertThat(after, Matchers.equalTo(before));
		MatcherAssert.assertThat(after.getIconUrl().toString(), urlEnd);
	}
}