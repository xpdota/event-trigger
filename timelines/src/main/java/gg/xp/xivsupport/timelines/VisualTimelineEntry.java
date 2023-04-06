package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.events.triggers.jobs.gui.LabelOverride;
import gg.xp.xivsupport.models.CurrentMaxPair;

import java.util.Objects;

@SuppressWarnings("NumericCastThatLosesPrecision")
public record VisualTimelineEntry(
		TimelineEntry originalTimelineEntry,
		boolean isCurrentSync,
		double timeUntil,
		int barTimeBasis

) implements LabelOverride, CurrentMaxPair {

	@Override
	public String getLabel() {
		return String.format("%s%s", originalTimelineEntry.name(), isCurrentSync ? "*" : "");
	}

	public boolean shouldDisplay() {
		return originalTimelineEntry.name() != null;
	}

	@Override
	public long current() {
		if (remainingActiveTime() > 0) {
			return (long) (remainingActiveTime() * 1000.0);
		}
		float timeBasis = barTimeBasis * 1000.0f;
		return (long) Math.min(timeBasis - (1000.0 * timeUntil), timeBasis);
	}

	@Override
	public long max() {
		if (remainingActiveTime() > 0) {
			//noinspection ConstantConditions - known to be non-null if remaining active time is > 0
			return (long) (originalTimelineEntry.duration() * 1000);
		}
		return barTimeBasis * 1000L;
	}

	public double remainingActiveTime() {
		Double timelineDuration = originalTimelineEntry.duration();
		if (timelineDuration == null) {
			return 0;
		}
		if (timeUntil > 0) {
			// Not active yet
			return 0;
		}
		// timeUntil will be negative, so we want to add.
		return Math.min(timelineDuration, Math.max(0, timelineDuration + timeUntil));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VisualTimelineEntry that = (VisualTimelineEntry) o;
		return isCurrentSync() == that.isCurrentSync() && originalTimelineEntry.equals(that.originalTimelineEntry);
	}

	@Override
	public int hashCode() {
		return Objects.hash(originalTimelineEntry, isCurrentSync());
	}
}
