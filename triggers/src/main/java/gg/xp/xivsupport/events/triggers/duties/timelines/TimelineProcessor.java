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
import java.util.stream.Collectors;

public class TimelineProcessor {

	private static final Logger log = LoggerFactory.getLogger(TimelineProcessor.class);
	private final List<RawTimelineEntry> entries;
	private TimelineSync lastSync;

	private record TimelineSync(ACTLogLineEvent line, double lastSyncTime, RawTimelineEntry original) {}

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
			double timeToSyncTo = rawTimelineEntry.getSyncToTime();
			lastSync = new TimelineSync(event, timeToSyncTo, rawTimelineEntry);
			log.info("New Sync: {} -> {} ({})", rawTimelineEntry, timeToSyncTo, emulatedActLogLine);
		});
	}

	public List<RawTimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public List<VisualTimelineEntry> getCurrentTimelineEntries() {
		double effectiveLastSyncTime;
		if (lastSync == null) {
			effectiveLastSyncTime = 0.0d;
		}
		else {
			effectiveLastSyncTime = lastSync.lastSyncTime + lastSync.line.getEffectiveTimeSince().toMillis() / 1000.0;
		}
		// TODO: make these settings
		return entries.stream()
				.filter(entry -> isLastSync(entry) || (entry.time() > (effectiveLastSyncTime - 10) && entry.time() < (effectiveLastSyncTime + 30)))
				.map(entry -> {
					return new VisualTimelineEntry(entry, isLastSync(entry), entry.time() - effectiveLastSyncTime);
				})
				.collect(Collectors.toList());
	}

	private boolean isLastSync(RawTimelineEntry entry) {
		return lastSync != null && lastSync.original == entry;
	}
}
