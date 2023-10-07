package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.CurrentTimeSource;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TimelineProcessor {

	// TODO: we can use in/out of combat now

	private static final Logger log = LoggerFactory.getLogger(TimelineProcessor.class);
	private final List<TimelineEntry> entries;
	private final TimelineManager manager;
	private final List<TimelineEntry> rawEntries;
	private final Map<String, Double> labels = new HashMap<>();
	private final IntSetting secondsFuture;
	private final IntSetting secondsPast;
	private final BooleanSetting debugMode;
	private final BooleanSetting showPrePull;
	private final RefreshLoop<TimelineProcessor> refresher;
	private final LabelResolver resolver;
	private final CurrentTimeSource timeSource;
	private @Nullable TimelineSync lastSync;


	private TimelineProcessor(TimelineManager manager, List<TimelineEntry> entries, Job playerJob) {
		this.manager = manager;
		this.rawEntries = entries;
		this.entries = entries.stream().filter(TimelineEntry::enabled).filter(te -> playerJob == null || te.enabledForJob(playerJob)).collect(Collectors.toList());
		this.timeSource = manager.getTimeSource();
		entries.stream().filter(TimelineEntry::isLabel).forEach(label -> this.labels.put(label.name(), label.time()));
		secondsFuture = manager.getSecondsFuture();
		secondsPast = manager.getSecondsPast();
		debugMode = manager.getDebugMode();
		showPrePull = manager.getPrePullSetting();
		resolver = key -> {
			Double value = labels.get(key);
			if (value == null) {
				log.error("Missing timeline label: '{}'", key);
				return null;
			}
			return value;
		};
		refresher = new RefreshLoop<>("TimelineRefresher", this, TimelineProcessor::handleTriggers, i -> 200L);
		refresher.start();
	}

	public static TimelineProcessor of(TimelineManager manager, InputStream file, List<? extends TimelineEntry> extra, Job playerJob, LanguageReplacements replacements) {
		List<TimelineEntry> timelineEntries;
		try {
			timelineEntries = TimelineParser.parseMultiple(IOUtils.readLines(file, StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<TimelineEntry> all = new ArrayList<>(timelineEntries);
		for (int i = 0; i < all.size(); i++) {
			TimelineEntry currentItem = all.get(i);
			final String originalName = currentItem.name();
			final String originalSync = currentItem.sync() == null ? null : currentItem.sync().pattern();
			String newName = originalName;
			String newSync = originalSync;
			if (originalName != null) {
				for (var textReplacement : replacements.replaceText().entrySet()) {
					newName = textReplacement.getKey().matcher(newName).replaceAll(textReplacement.getValue());
				}
			}
			if (originalSync != null) {
				for (var syncReplacement : replacements.replaceSync().entrySet()) {
					newSync = syncReplacement.getKey().matcher(newSync).replaceAll(syncReplacement.getValue());
				}
			}
			if (!Objects.equals(originalName, newName) || !Objects.equals(originalSync, newSync)) {
				Pattern newSyncFinal = newSync == null ? null : Pattern.compile(newSync);
				all.set(i, new TranslatedTextFileEntry(currentItem, newName, newSyncFinal));
			}
		}

		// Remove things that have been overridden
		for (TimelineEntry customEntry : extra) {
			all.removeIf(customEntry::shouldSupersede);
		}
		all.addAll(extra);
		all.sort(Comparator.naturalOrder());
		return new TimelineProcessor(manager, all, playerJob);
	}

	interface TimelineSync {
		long msSince();

		default double secondSince() {
			return msSince() / 1000.0;
		}

		double syncTo();

		TimelineEntry timelineEntry();
	}

	record LogLineSync(
			ACTLogLineEvent line,
			double syncTo,
			TimelineEntry timelineEntry
	) implements TimelineSync {
		@Override
		public long msSince() {
			return line.getEffectiveTimeSince().toMillis();
		}
	}

	record ForceJumpSync(
			Instant jumpTime,
			double syncTo,
			TimelineEntry timelineEntry,
			CurrentTimeSource timeSource
	) implements TimelineSync {

		@Override
		public long msSince() {
			return Duration.between(jumpTime, timeSource.now()).toMillis();
		}
	}

	public double getEffectiveTime() {
		if (lastSync == null) {
			return 0;
		}
		return lastSync.syncTo() + lastSync.secondSince();
	}

	public @Nullable TimelineSync getLastSync() {
		return lastSync;
	}

	// Exposed so that we can reload the timeline but force the sync to stay
	void setLastSync(@Nullable TimelineSync lastSync) {
		this.lastSync = lastSync;
	}

	public void processActLine(ACTLogLineEvent event) {
		// Skip spammy syncs
		if (lastSync != null && lastSync.msSince() < 10) {
			return;
		}
		// To save on processing time, ignore some events that will never be found in a timeline
		int num = event.getLineNumber();
		// Things that can be ignored:
		/*
			1. Change Zone
			2. Change Primary Player
			11. Party List
			12. Player Stats
			24. DoT tick
			28. Waymarks
			29. Player marker
			31. Gauge
			37. Action resolved (probably can ignore)
			38. Status effect list
			39. HP Update
			200 and up. Only used by the ACT plugin itself for debug messages and such.
		 */
		if (num == 1 || num == 2 || num == 11 || num == 12 || num == 24 || num == 28 || num == 29 || num == 31 || num == 37 || num == 38 || num == 39 || num > 200) {
			return;
		}
		String emulatedActLogLine = event.getEmulatedActLogLine();
		double timeNow = getEffectiveTime();
		Optional<TimelineEntry> newSync = entries.stream().filter(entry -> entry.shouldSync(timeNow, emulatedActLogLine)).findFirst();
		newSync.ifPresent(rawTimelineEntry -> {
			double timeToSyncTo = rawTimelineEntry.getSyncToTime(resolver);
			TimelineSync newTsync = new LogLineSync(event, timeToSyncTo, rawTimelineEntry);
			log.trace("New Sync: {} -> {} ({})", rawTimelineEntry, timeToSyncTo, emulatedActLogLine);
			setNewSync(newTsync);
		});
	}

	private void setNewSync(TimelineSync sync) {
		double effectiveTimeBefore = getEffectiveTime();
		boolean firstSync = lastSync == null;
		lastSync = sync;
		double effectiveTimeAfter = getEffectiveTime();

		double delta = effectiveTimeAfter - effectiveTimeBefore;
		log.info("New Sync: {}", sync);
		if (Math.abs(delta) > 0.6) {
			log.info("Timeline jumped by {} ({} -> {})", delta, effectiveTimeBefore, effectiveTimeAfter);
		}
		// Only reprocess timeline triggers if the sync changed our timing by more than a couple seconds (i.e.
		// we want to know whether the sync was actually a jump/phase change/whatever, not just time skew).
		if (firstSync || Math.abs(delta) > 1.0) {
			reprocessTriggers();
		}
	}

	public List<TimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public List<TimelineEntry> getRawEntries() {
		return Collections.unmodifiableList(rawEntries);
	}

	public List<VisualTimelineEntry> getCurrentTimelineEntries() {
		if (lastSync == null && !showPrePull.get()) {
			return Collections.emptyList();
		}
		double currentTime = getEffectiveTime();
		boolean debug = debugMode.get();
		int barTimeBasis = manager.getBarTimeBasis().get();
		int maxDisplayed = manager.getRowsToDisplay().get();
		List<VisualTimelineEntry> list = new ArrayList<>(maxDisplayed);
		// Our time basis - needed for displaying entries after a force jump
		double nextFjSyncTo = 0;
		double nextFjAt = 0;

		int secondsPast = this.secondsPast.get();
		outer:
		while (list.size() < maxDisplayed) {
			for (TimelineEntry entry : entries) {
				// Hide labels unless debug mode is on
				if (entry.isLabel() && !debug) {
					continue;
				}
				// The "effective time" for this entry is its actual time, minus the next upcoming forceJump, plus the time we arrive at that FJ
				double effectiveEntryTime = entry.time() - nextFjSyncTo + nextFjAt;
				// If debug mode is enabled, always show the last sync
				if (isLastSync(entry) && debug
				    // Determine if an entry should still be shown.
				    // secondsPast is the user-configurable value for how many seconds in the past to let an entry linger.
				    // Account for duration entries as well - if you want to see 5 seconds in the past, and an entry has
				    // an 8 second duration, it should stick around for 13 seconds after its time.
				    || (effectiveEntryTime + (entry.duration() == null ? 0 : entry.duration()) > (currentTime - secondsPast)
				        // Filter out entries too far in the future
				        && effectiveEntryTime < (currentTime + secondsFuture.get())
				        // Filter out anything that is before our next FJ
				        && entry.time() >= nextFjSyncTo
				        // Filter out empty entries, unless debug is enabled
				        && (entry.name() != null || debug))) {
					double timeUntil = effectiveEntryTime - currentTime;
					VisualTimelineEntry visualTimelineEntry = new VisualTimelineEntry(entry, isLastSync(entry), timeUntil, barTimeBasis);
					list.add(visualTimelineEntry);
					if (entry.forceJump()) {
						Double fj = entry.getSyncToTime(resolver);
						if (fj != null) {
							nextFjAt = effectiveEntryTime;
							nextFjSyncTo = fj;
							// Once we're in loop mode, don't allow anything in the past
							secondsPast = 0;
							continue outer;
						}
					}
				}
			}
			break;
		}
		return list;
	}

	private List<UpcomingAction> upcomingTriggers = Collections.emptyList();

	public interface UpcomingAction {
		double timeUntil();

		void fire();
	}

	private class UpcomingForceJump implements UpcomingAction {

		private final TimelineEntry entry;
		private final double jumpTime;

		public UpcomingForceJump(double jumpTime, TimelineEntry entry) {
			this.entry = entry;
			this.jumpTime = jumpTime;
		}

		@Override
		public double timeUntil() {
			return entry.time() - getEffectiveTime();
		}

		@Override
		public void fire() {
			log.info("UpcomingForceJump fired");
			setNewSync(new ForceJumpSync(timeSource.now(), jumpTime, entry, timeSource));
		}

		@Override
		public String toString() {
			return "UpcomingForceJump{" +
			       "entry=" + entry +
			       ", jumpTime=" + jumpTime +
			       '}';
		}
	}

	public class UpcomingTrigger implements UpcomingAction, HasDuration, HasOptionalIconURL {

		private final double timelineTime;
		private final double callTime;
		private final Duration effectiveDuration;
		private final boolean isPreCall;
		private final TimelineEntry entry;

		UpcomingTrigger(TimelineEntry entry) {
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

		@Override
		public double timeUntil() {
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
			return Duration.ofMillis((long) (timeUntil() * 1000)).minus(getEstimatedRemainingDuration());
		}

		public TimelineEntry getEntry() {
			return entry;
		}

		@Override
		public @Nullable HasIconURL getIconUrl() {
			URL rawIcon = entry.icon();
			if (rawIcon == null) {
				return null;
			}
			else {
				return () -> rawIcon;
			}
		}

		@Override
		public void fire() {
			manager.doTriggerCall(this);
		}

		@Override
		public String toString() {
			return "UpcomingTrigger{" +
			       "timelineTime=" + timelineTime +
			       ", callTime=" + callTime +
			       ", effectiveDuration=" + effectiveDuration +
			       ", isPreCall=" + isPreCall +
			       ", entry=" + entry +
			       '}';
		}
	}

	private void reprocessTriggers() {
		if (lastSync == null) {
			upcomingTriggers = Collections.emptyList();
			return;
		}
		List<UpcomingAction> out = new ArrayList<>();

		double effectiveTime = getEffectiveTime();

		for (TimelineEntry entry : entries) {
			if (entry.callout()) {
				double timeUntil = entry.effectiveCalloutTime() - effectiveTime;
				if (timeUntil > -0.1 || entry == lastSync.timelineEntry() || entry.time() > lastSync.syncTo()) {
					out.add(new UpcomingTrigger(entry));
				}
			}
			if (entry.forceJump()) {
				/*
					Logic for force jumps:
					Schedule a jump if and only if:
					1. The time of the entry is in the future (less than 0.1 seconds in the past)
					2. The entry is not our current sync, unless at least a second has elapsed
				 */
				Double jumpTo = entry.getSyncToTime(resolver);
				double timeUntil = entry.time() - effectiveTime;
				if (jumpTo != null
				    && timeUntil > -0.1
//				    && (lastSync == null
//				        || lastSync.timelineEntry() != entry
//				        || lastSync.msSince() > 1_000)
				) {
					out.add(new UpcomingForceJump(jumpTo, entry));
				}
			}
		}
		upcomingTriggers = out;
		log.info("Upcoming: {}", out);
		refresher.refreshNow();
	}

	private void handleTriggers() {
		if (upcomingTriggers.isEmpty()) {
			return;
		}
		Iterator<UpcomingAction> iter = upcomingTriggers.iterator();
		while (iter.hasNext()) {
			UpcomingAction next = iter.next();
			if (next.timeUntil() <= 0) {
				iter.remove();
				next.fire();
			}
		}
	}

	private boolean isLastSync(TimelineEntry entry) {
		TimelineSync sync = lastSync;
		return sync != null && sync.timelineEntry() == entry;
	}

	public void reset() {
		lastSync = null;
	}

}
