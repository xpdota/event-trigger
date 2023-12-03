package gg.xp.xivsupport.timelines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.regex.Pattern;

public record TextFileLabelEntry(
		double time,
		String name
) implements TimelineEntry, Serializable {

	@Override
	public String toString() {
		return "TextFileLabelEntry{" +
		       "time=" + time +
		       ", name='" + name + '\'' +
		       '}';
	}

	@Override
	public @Nullable Pattern sync() {
		return null;
	}

	@Override
	public @Nullable Double duration() {
		return null;
	}

	@Override
	public @NotNull TimelineWindow timelineWindow() {
		return TimelineWindow.NONE;
	}

	@Override
	public @Nullable Double jump() {
		return null;
	}

	@Override
	public @Nullable String jumpLabel() {
		return null;
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

	@Override
	public boolean isLabel() {
		return true;
	}

	@Override
	public @Nullable EventSyncController eventSyncController() {
		return null;
	}
}
