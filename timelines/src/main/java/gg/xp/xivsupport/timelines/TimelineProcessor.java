package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class TimelineProcessor {

	// TODO: we can use in/out of combat now

	private static final Logger log = LoggerFactory.getLogger(TimelineProcessor.class);
	private final List<TimelineEntry> entries;
	private final TimelineManager manager;
	private final List<TimelineEntry> rawEntries;
	private final IntSetting secondsFuture;
	private final IntSetting secondsPast;
	private final BooleanSetting debugMode;
	private final BooleanSetting showPrePull;
	private final RefreshLoop<TimelineProcessor> refresher;
	private @Nullable TimelineSync lastSync;

	record TimelineSync(ACTLogLineEvent line, double lastSyncTime, TimelineEntry original) {
	}

	private TimelineProcessor(TimelineManager manager, List<TimelineEntry> entries) {
		this.manager = manager;
		this.rawEntries = entries;
		this.entries = entries.stream().filter(TimelineEntry::enabled).collect(Collectors.toList());
		secondsFuture = manager.getSecondsFuture();
		secondsPast = manager.getSecondsPast();
		debugMode = manager.getDebugMode();
		showPrePull = manager.getPrePullSetting();
		refresher = new RefreshLoop<>("TimelineRefresher", this, TimelineProcessor::handleTriggers, i -> 200L);
		refresher.start();
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
		// Remove things that have been overridden
		for (TimelineEntry customEntry : extra) {
			all.removeIf(customEntry::shouldSupersede);
		}
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
		// Skip spammy syncs
		if (lastSync != null && lastSync.line.getEffectiveTimeSince().toMillis() < 10) {
			return;
		}
		String emulatedActLogLine = event.getEmulatedActLogLine();
		Optional<TimelineEntry> newSync = entries.stream().filter(entry -> entry.shouldSync(getEffectiveTime(), emulatedActLogLine)).findFirst();
		newSync.ifPresent(rawTimelineEntry -> {
			double timeToSyncTo = rawTimelineEntry.getSyncToTime();
			double effectiveTimeBefore = getEffectiveTime();
			boolean firstSync = lastSync == null;
			lastSync = new TimelineSync(event, timeToSyncTo, rawTimelineEntry);
			log.trace("New Sync: {} -> {} ({})", rawTimelineEntry, timeToSyncTo, emulatedActLogLine);
			double effectiveTimeAfter = getEffectiveTime();

			double delta = effectiveTimeAfter - effectiveTimeBefore;
			log.trace("Timeline jumped by {} ({} -> {})", delta, effectiveTimeBefore, effectiveTimeAfter);
			// Only reprocess timeline triggers if the sync changed our timing by more than a couple seconds (i.e.
			// we want to know whether the sync was actually a jump/phase change/whatever, not just time skew).
			if (firstSync || Math.abs(delta) > 4.0) {
				reprocessTriggers();
			}
		});
	}

	public List<TimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public List<TimelineEntry> getRawEntries() {
		return Collections.unmodifiableList(rawEntries);
	}

	private double getEffectiveLastSyncTime() {
		if (lastSync == null) {
			return 0.0d;
		}
		else {
			return lastSync.lastSyncTime + lastSync.line.getEffectiveTimeSince().toMillis() / 1000.0;
		}
	}

	public List<VisualTimelineEntry> getCurrentTimelineEntries() {
		if (lastSync == null && !showPrePull.get()) {
			return Collections.emptyList();
		}
		double effectiveLastSyncTime = getEffectiveLastSyncTime();
		boolean debug = debugMode.get();
		return entries.stream()
				.filter(entry -> isLastSync(entry) && debug
						// TODO: this doesn't show 'active' timeline entries
						|| (entry.time() + (entry.duration() == null ? 0 : entry.duration()) > (effectiveLastSyncTime - secondsPast.get())
						&& entry.time() < (effectiveLastSyncTime + secondsFuture.get())
						&& (entry.name() != null || debug)))
				.map(entry -> new VisualTimelineEntry(entry, isLastSync(entry), entry.time() - effectiveLastSyncTime))
				.collect(Collectors.toList());
	}

	private List<UpcomingCall> upcomingTriggers = Collections.emptyList();

	public class UpcomingCall implements HasDuration, Serializable {

		@Serial
		private static final long serialVersionUID = 4627297693366126838L;
		private final double timelineTime;
		private final double callTime;
		private final Duration effectiveDuration;
		private final boolean isPreCall;
		private final TimelineEntry entry;

		UpcomingCall(TimelineEntry entry) {
			this.entry = entry;
			timelineTime = entry.time();
			callTime = entry.effectiveCalloutTime();
			if (Math.abs(timelineTime - callTime) < 0.1) {
				// Use some kind of sane default, this doesn't really matter
				effectiveDuration = Duration.ofSeconds(10);
				isPreCall = false;
			}
			else {
				effectiveDuration = Duration.ofMillis((long) ((timelineTime - callTime) * 1000));
				isPreCall = true;
			}
		}

		public boolean isPreCall() {
			return isPreCall;
		}

		public double timeUntilCall() {
			return callTime - getEffectiveTime();
		}

		public double timeUntilTimelineEntry() {
			return timelineTime - getEffectiveTime();
		}

		@Override
		public Duration getEstimatedRemainingDuration() {
			return Duration.ofMillis(Math.max(0, (long) (timeUntilTimelineEntry() * 1000)));
		}

		@Override
		public Duration getEstimatedTimeSinceExpiry() {
			return Duration.ofMillis((long) (timeUntilTimelineEntry() * -1000));
		}

		@Override
		public Duration getInitialDuration() {
			return effectiveDuration;
		}

		@Override
		public Duration getEffectiveTimeSince() {
			return Duration.ofMillis((long) (timeUntilCall() * 1000)).minus(getEstimatedRemainingDuration());
		}

		public TimelineEntry getEntry() {
			return entry;
		}
	}

	private void reprocessTriggers() {
		if (lastSync == null) {
			upcomingTriggers = Collections.emptyList();
			return;
		}
		List<UpcomingCall> out = new ArrayList<>();

		double effectiveLastSyncTime = getEffectiveLastSyncTime();

		for (TimelineEntry entry : entries) {
			if (!entry.callout()) {
				continue;
			}
			double timeUntilCall = entry.effectiveCalloutTime() - effectiveLastSyncTime;
			if (timeUntilCall > -0.1 || entry == lastSync.original || entry.time() > lastSync.lastSyncTime) {
				out.add(new UpcomingCall(entry));
			}
		}
		upcomingTriggers = out;
		refresher.refreshNow();
	}

	private void handleTriggers() {
		if (upcomingTriggers.isEmpty()) {
			return;
		}
		Iterator<UpcomingCall> iter = upcomingTriggers.iterator();
		while (iter.hasNext()) {
			UpcomingCall next = iter.next();
			if (next.timeUntilCall() <= 0) {
				manager.doTriggerCall(next);
				iter.remove();
			}
		}
	}

	private boolean isLastSync(TimelineEntry entry) {
		return lastSync != null && lastSync.original == entry;
	}

	public void reset() {
		lastSync = null;
	}

}
