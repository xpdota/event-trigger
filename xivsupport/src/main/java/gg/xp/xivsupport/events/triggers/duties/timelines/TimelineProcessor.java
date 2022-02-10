package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class TimelineProcessor {

	private static final Logger log = LoggerFactory.getLogger(TimelineProcessor.class);
	private final List<TimelineEntry> entries;
	private final IntSetting secondsFuture;
	private final IntSetting secondsPast;
	private final BooleanSetting debugMode;
	private @Nullable TimelineSync lastSync;

	record TimelineSync(ACTLogLineEvent line, double lastSyncTime, TimelineEntry original) {}

	private TimelineProcessor(TimelineManager manager, List<TimelineEntry> entries) {
		this.entries = entries;
		secondsFuture = manager.getSecondsFuture();
		secondsPast = manager.getSecondsPast();
		debugMode = manager.getDebugMode();
	}

	public static TimelineProcessor of(TimelineManager manager, InputStream file, List<? extends TimelineEntry> extra) {
		List<TextFileTimelineEntry> timelineEntries;
		try {
			timelineEntries = TimelineParser.parseMultiple(IOUtils.readLines(file, StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<TimelineEntry> all = new ArrayList<>(timelineEntries);
		all.addAll(extra);
		all.sort(Comparator.naturalOrder());
		return new TimelineProcessor(manager, all);
	}

	public double getEffectiveTime() {
		if (lastSync == null) {
			return 0;
		}
		long millisSinceEvent = lastSync.line.getEffectiveTimeSince().toMillis();
		return lastSync.lastSyncTime + (millisSinceEvent / 1000.0d);
	}

	public @Nullable TimelineSync getLastSync() {
		return lastSync;
	}

	public void setLastSync(@Nullable TimelineSync lastSync) {
		this.lastSync = lastSync;
	}

	public void processActLine(ACTLogLineEvent event) {
		String emulatedActLogLine = event.getEmulatedActLogLine();
		Optional<TimelineEntry> newSync = entries.stream().filter(entry -> entry.shouldSync(getEffectiveTime(), emulatedActLogLine)).findFirst();
		newSync.ifPresent(rawTimelineEntry -> {
			double timeToSyncTo = rawTimelineEntry.getSyncToTime();
			lastSync = new TimelineSync(event, timeToSyncTo, rawTimelineEntry);
			log.info("New Sync: {} -> {} ({})", rawTimelineEntry, timeToSyncTo, emulatedActLogLine);
		});
	}

	public List<TimelineEntry> getEntries() {
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
				.filter(entry -> isLastSync(entry) && debugMode.get()
						|| (entry.time() > (effectiveLastSyncTime - secondsPast.get())
						&& entry.time() < (effectiveLastSyncTime + secondsFuture.get())))
				.map(entry -> new VisualTimelineEntry(entry, isLastSync(entry), entry.time() - effectiveLastSyncTime))
				.collect(Collectors.toList());
	}

	private boolean isLastSync(TimelineEntry entry) {
		return lastSync != null && lastSync.original == entry;
	}

	public void reset() {
		lastSync = null;
	}

}
