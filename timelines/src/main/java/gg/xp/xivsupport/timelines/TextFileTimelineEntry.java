package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record TextFileTimelineEntry(
		double time,
		@Nullable String name,
		@Nullable Pattern sync,
		@Nullable Double duration,
		@NotNull TimelineWindow timelineWindow,
		@Nullable Double jump,
		@Nullable String jumpLabel,
		boolean forceJump,
		EventSyncController eventSyncController) implements TimelineEntry, Serializable {

	@Override
	public String toString() {
		return "TextFileTimelineEntry{" +
		       "time=" + time +
		       ", name='" + name + '\'' +
		       ", sync=" + sync +
		       ", duration=" + duration +
		       ", timelineWindow=" + timelineWindow +
		       ", jump=" + jump +
		       ", jumpLabel='" + jumpLabel + '\'' +
		       ", forceJump=" + forceJump +
		       ", syncCtrl=" + eventSyncController +
		       '}';
	}

	@Override
	public boolean enabled() {
		return true;
	}

	@Override
	public boolean callout() {
		return false;
	}

	@Override
	public double calloutPreTime() {
		return 0;
	}
}
