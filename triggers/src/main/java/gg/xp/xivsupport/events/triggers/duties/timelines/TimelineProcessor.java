package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TimelineProcessor {

	private static final Logger log = LoggerFactory.getLogger(TimelineProcessor.class);
	private final List<RawTimelineEntry> entries;
	private TimelineSync lastSync;

	private record TimelineSync(ACTLogLineEvent line, double lastSyncTime) {}

	private TimelineProcessor(List<RawTimelineEntry> entries) {
		this.entries = entries;
	}

	public static TimelineProcessor of(File file) {
		List<RawTimelineEntry> timelineEntries;
		try {
			timelineEntries = TimelineParser.parseMultiple(Files.readAllLines(file.toPath()));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new TimelineProcessor(timelineEntries);
	}

	public double getEffectiveTime() {
		if (lastSync == null) {
			return 0;
		}
		long millisSinceEvent = lastSync.line.getEffectiveTimeSince().toMillis();
		return lastSync.lastSyncTime + (millisSinceEvent / 1000.0d);
	}


	public void processActLine(ACTLogLineEvent event) {
		String emulatedActLogLine = event.getEmulatedActLogLine();
		Optional<RawTimelineEntry> newSync = entries.stream().filter(entry -> entry.shouldSync(getEffectiveTime(), emulatedActLogLine)).findFirst();
		newSync.ifPresent(rawTimelineEntry -> {
			lastSync = new TimelineSync(event, rawTimelineEntry.time());
			log.info("New Sync: {} {}", rawTimelineEntry, emulatedActLogLine);
		});
	}

	public List<RawTimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}
}
