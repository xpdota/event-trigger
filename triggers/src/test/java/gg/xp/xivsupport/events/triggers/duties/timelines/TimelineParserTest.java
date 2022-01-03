package gg.xp.xivsupport.events.triggers.duties.timelines;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TimelineParserTest {

	@Test
	void timelineMinimal() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("458.7 \"Firebomb\"");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 458.7);
		Assert.assertEquals(rawTimelineEntry.name(), "Firebomb");
		Assert.assertNull(rawTimelineEntry.sync());
		Assert.assertNull(rawTimelineEntry.duration());
		Assert.assertNull(rawTimelineEntry.timelineWindow());
		Assert.assertNull(rawTimelineEntry.jump());
	}

	@Test
	void timelineWithSync() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("2.0 \"Shield Skewer\" sync / 1[56]:[^:]*:Rhitahtyn sas Arvina:471:/");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 2.0d);
		Assert.assertEquals(rawTimelineEntry.name(), "Shield Skewer");
		Assert.assertEquals(rawTimelineEntry.sync().pattern(), " 1[56]:[^:]*:Rhitahtyn sas Arvina:471:");
		Assert.assertNull(rawTimelineEntry.duration());
		Assert.assertNull(rawTimelineEntry.timelineWindow());
		Assert.assertNull(rawTimelineEntry.jump());
	}

	@Test
	void timelineWithSyncAndWindow() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("413.2 \"Shrapnel Shell\" sync / 1[56]:[^:]*:Rhitahtyn sas Arvina:474:/ window 20,20");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 413.2d);
		Assert.assertEquals(rawTimelineEntry.name(), "Shrapnel Shell");
		Assert.assertEquals(rawTimelineEntry.sync().pattern(), " 1[56]:[^:]*:Rhitahtyn sas Arvina:474:");
		Assert.assertNull(rawTimelineEntry.duration());
		Assert.assertNotNull(rawTimelineEntry.timelineWindow());
		Assert.assertEquals(rawTimelineEntry.timelineWindow().start(), 20.0d);
		Assert.assertEquals(rawTimelineEntry.timelineWindow().end(), 20.0d);
		Assert.assertNull(rawTimelineEntry.jump());
	}

	@Test
	void timelineWithSyncWindowAndJump() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("449.5 \"Shrapnel Shell\" sync / 1[56]:[^:]*:Rhitahtyn sas Arvina:474:/ window 20,100 jump 413.2");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 449.5d);
		Assert.assertEquals(rawTimelineEntry.name(), "Shrapnel Shell");
		Assert.assertEquals(rawTimelineEntry.sync().pattern(), " 1[56]:[^:]*:Rhitahtyn sas Arvina:474:");
		Assert.assertNull(rawTimelineEntry.duration());
		Assert.assertNotNull(rawTimelineEntry.timelineWindow());
		Assert.assertEquals(rawTimelineEntry.timelineWindow().start(), 20.0d);
		Assert.assertEquals(rawTimelineEntry.timelineWindow().end(), 100.0d);
		Assert.assertEquals(rawTimelineEntry.jump(), (Double) 413.2d);
	}

	@Test
	void timelineWithSyncAndDuration() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("705.7 \"J Storm + Waves x16\" sync / 1[56]:[^:]*:Brute Justice:4876:/ duration 50");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 705.7d);
		Assert.assertEquals(rawTimelineEntry.name(), "J Storm + Waves x16");
		Assert.assertEquals(rawTimelineEntry.sync().pattern(), " 1[56]:[^:]*:Brute Justice:4876:");
		Assert.assertEquals(rawTimelineEntry.duration(), (Double) 50.0d);
		Assert.assertNull(rawTimelineEntry.timelineWindow());
		Assert.assertNull(rawTimelineEntry.jump());
	}

	@Test
	void timelineSyncOnly() {
		RawTimelineEntry rawTimelineEntry = TimelineParser.parseRaw("584.3 \"--sync--\" sync / 00:0044:[^:]*:Your defeat will bring/ window 600,0");
		Assert.assertNotNull(rawTimelineEntry);
		Assert.assertEquals(rawTimelineEntry.time(), 584.3d);
		Assert.assertNull(rawTimelineEntry.name());
		Assert.assertEquals(rawTimelineEntry.sync().pattern(), " 00:0044:[^:]*:Your defeat will bring");
		Assert.assertNull(rawTimelineEntry.duration());
		Assert.assertNotNull(rawTimelineEntry.timelineWindow());
		Assert.assertEquals(rawTimelineEntry.timelineWindow().start(), 600.0d);
		Assert.assertEquals(rawTimelineEntry.timelineWindow().end(), 0.0d);
		Assert.assertNull(rawTimelineEntry.jump());

	}

}