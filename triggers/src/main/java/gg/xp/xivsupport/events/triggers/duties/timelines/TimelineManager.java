package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.XivZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TimelineManager {

	private static final Logger log = LoggerFactory.getLogger(TimelineManager.class);
	private static final Map<Long, String> zoneIdToTimelineFile = new HashMap<>();

	static {
		zoneIdToTimelineFile.put(332L, "cape_westwind.txt");
	}

	private TimelineProcessor currentTimeline;

	@HandleEvents
	public void changeZone(EventContext context, ZoneChangeEvent zoneChangeEvent) {
		XivZone zone = zoneChangeEvent.getZone();
		doZoneChange(zone);
	}

	@HandleEvents(order = 40_000)
	public void actLine(EventContext context, ACTLogLineEvent event) {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.processActLine(event);
		}
	}

	private void doZoneChange(XivZone zoneId) {
		String filename = zoneIdToTimelineFile.get(zoneId.getId());
		if (filename == null) {
			log.info("No timeline found for new zone {}", zoneId);
			currentTimeline = null;
			return;
		}
		URL resource = TimelineManager.class.getResource('/' + filename);
		if (resource == null) {
			log.info("Timeline file '{}' for zone '{}' is missing", filename, zoneId);
			currentTimeline = null;
			return;
		}
		try {
			currentTimeline = TimelineProcessor.of(new File(resource.toURI()));
		}
		catch (Throwable e) {
			log.error("Error loading timeline file", e);
			currentTimeline = null;
			return;
		}
		log.info("Loaded timeline for zone '{}', {} timeline entries", zoneId, currentTimeline.getEntries().size());
	}


}
