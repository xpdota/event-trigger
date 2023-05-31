package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface TimelineEntry extends Comparable<TimelineEntry> {

	@JsonIgnore
	default double getMinTime() {
		return time() - timelineWindow().start();
	}

	@JsonIgnore
	default double getMaxTime() {
		return time() + timelineWindow().end();
	}


	default boolean shouldSync(double currentTime, String line) {
		Pattern sync = sync();
		if (sync == null) {
			return false;
		}
		return currentTime >= getMinTime() && currentTime <= getMaxTime() && sync.matcher(line).find();
	}

	@JsonIgnore
	default double getSyncToTime() {
		Double jump = jump();
		if (jump == null) {
			return time();
		}
		else {
			return jump;
		}

	}

	@Override
	@Nullable String toString();

	double time();

	@Nullable String name();

	@Nullable Pattern sync();

	@Nullable Double duration();

	@NotNull TimelineWindow timelineWindow();

	@Nullable Double jump();

	boolean enabled();

	@Override
	default int compareTo(@NotNull TimelineEntry o) {
		return Double.compare(this.time(), o.time());
	}

	default @Nullable URL icon() {
		return null;
	}

	default @Nullable TimelineReference replaces() {
		return null;
	}

	default boolean shouldSupersede(TimelineEntry that) {
		TimelineReference overrides = this.replaces();
		if (overrides == null) {
			return false;
		}
		// To allow users to switch languages freely without losing their overrides,
		// compare to the untranslated versions.
		that = that.untranslated();
		// Rules:
		// 1. Time must match
		// 2. Name must match
		// 3. Sync must match
		// 4. For backwards compatibility, treat replacing an empty/null pattern as always matching for step 2
		String desiredName = overrides.name();
		//noinspection FloatingPointEquality
		if (overrides.time() == that.time()) {
			String thatPattern = that.sync() == null ? null : that.sync().pattern();
			if (!Objects.equals(desiredName, that.name())) {
				return false;
			}
			if (overrides.pattern() == null || overrides.pattern().isBlank()) {
				return true;
			}
			return (Objects.equals(overrides.pattern(), thatPattern));
		}
		return false;
	}

	boolean callout();

	double calloutPreTime();

	default double effectiveCalloutTime() {
		return time() - calloutPreTime();
	}

	default boolean enabledForJob(Job job) {
		return true;
	}

	default TimelineEntry untranslated() {
		return this;
	}

	@JsonIgnore
	default String toTextFormat() {
		StringBuilder sb = new StringBuilder();
		if (!enabled()) {
			// If disabled, just comment out the line
			sb.append("# ");
		}
		sb.append(fmtDouble(time())).append(' ');
		String name = name();
		if (name == null || name.isEmpty()) {
			sb.append("\"--sync--\"");
		}
		else {
			sb.append('"').append(name).append('"');
		}
		sb.append(' ');
		if (sync() != null) {
			sb.append("sync /").append(sync().pattern()).append('/').append(' ');
		}
		TimelineWindow window = timelineWindow();
		if (!TimelineWindow.DEFAULT.equals(window)) {
			sb.append("window ").append(fmtDouble(window.start())).append(',').append(fmtDouble(window.end())).append(' ');
		}
		Double duration = duration();
		if (duration != null) {
			sb.append("duration ").append(fmtDouble(duration)).append(' ');
		}
		Double jump = jump();
		if (jump != null) {
			sb.append("jump ").append(fmtDouble(jump));
		}
		return sb.toString();
	}

	@JsonIgnore
	default Stream<String> makeTriggerTimelineEntries() {
		if (!enabled() || !callout()) {
			return Stream.empty();
		}
		String uniqueName = makeUniqueName();
		String hideAllLine = "hideall \"%s\"".formatted(uniqueName);
		String actualTimelineLine = new TextFileTimelineEntry(time(), uniqueName, null, null, TimelineWindow.DEFAULT, null).toTextFormat();
		return Stream.of(hideAllLine, actualTimelineLine);
	}

	@JsonIgnore
	default Stream<String> getAllTextEntries() {
		return Stream.concat(Stream.of(toTextFormat()), makeTriggerTimelineEntries());
	}

	@JsonIgnore
	private static String fmtDouble(double dbl) {
		// Use US locale to force period as the decimal separator
		return String.format(Locale.US, "%.01f", dbl);
	}

	@JsonIgnore
	private String makeUniqueName() {
		// And then use DE here to use a comma instead since . is special in regex
		return String.format(Locale.GERMANY, "timeline-trig@%.02f", time());
	}

	@JsonIgnore
	private String callTextForExport() {
		String callText = name();
		if (callText == null) {
			callText = "";
		}
		return callText.replaceAll("\"", "'");
	}

	@JsonIgnore
	default @Nullable String makeTriggerJs() {
		if (!callout()) {
			return null;
		}
		String uniqueName = makeUniqueName();
		return String.format("""
						{
							id: "%s",
							suppressSeconds: 1,
							regex: /%s/,
							beforeSeconds: %s,
							alertText: "%s",
						},
				""", uniqueName, uniqueName, calloutPreTime(), callTextForExport());
	}
}
