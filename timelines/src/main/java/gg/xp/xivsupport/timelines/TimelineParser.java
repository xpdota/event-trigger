package gg.xp.xivsupport.timelines;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TimelineParser {

	private static final Logger log = LoggerFactory.getLogger(TimelineParser.class);
	// TODO: there are also windows that do not have a start + end, only a single number
	private static final Pattern timelinePatternLong = Pattern.compile("^(?<time>\\d+\\.?\\d*) (?:\"(?<title>.*)\"| sync /(?<sync>.*)/| window (?<windowStart>\\d*\\.?\\d*),(?<windowEnd>\\d*\\.?\\d*)| jump (?<jump>\\d*\\.?\\d*)| duration (?<duration>\\d*\\.?\\d*))*(?:$|\\s*#.*$)");

	public static List<TextFileTimelineEntry> parseMultiple(Collection<String> line) {
		return line.stream().map(TimelineParser::parseRaw).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static @Nullable TextFileTimelineEntry parseRaw(String line) {
		// Skip these for now
		if (line.trim().isEmpty() || line.startsWith("hideall")) {
			return null;
		}

		Matcher matcher = timelinePatternLong.matcher(line);
		Pattern sync;
		if (matcher.matches()) {
			log.trace("Matching line: {}", line);
			String timeRaw = matcher.group("time");
			String title = matcher.group("title");
			String syncRaw = matcher.group("sync");
			String windowStartRaw = matcher.group("windowStart");
			String windowEndRaw = matcher.group("windowEnd");
			String jumpRaw = matcher.group("jump");
			String durationRaw = matcher.group("duration");

			if ("--sync--".equals(title)) {
				title = null;
			}
			if (syncRaw == null) {
				sync = null;
			}
			else {
				sync = Pattern.compile(syncRaw, Pattern.CASE_INSENSITIVE);
			}
			TimelineWindow window;
			if (windowStartRaw != null && windowEndRaw != null) {
				window = new TimelineWindow(Double.parseDouble(windowStartRaw), Double.parseDouble(windowEndRaw));
			}
			else {
				window = TimelineWindow.DEFAULT;
			}
			double time;
			try {
				time = Double.parseDouble(timeRaw);
			}
			catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Not a valid time: %s (entire line: %s)".formatted(timeRaw, line));
			}
			return new TextFileTimelineEntry(time, title, sync, doubleOrNull(durationRaw), window, doubleOrNull(jumpRaw));
		}
		else {
			log.trace("Line did not match: {}", line);
			return null;
		}
	}

	@Contract("null -> null")
	private static @Nullable Double doubleOrNull(@Nullable String input) {
		if (input == null) {
			return null;
		}
		else {
			return Double.parseDouble(input);
		}
	}

}
