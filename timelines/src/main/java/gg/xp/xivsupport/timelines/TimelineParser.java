package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.timelines.cbevents.CbEventFmt;
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

public final class TimelineParser {

	private static final Logger log = LoggerFactory.getLogger(TimelineParser.class);
	// TODO: there are also windows that do not have a start + end, only a single number
	private static final Pattern timelinePatternLong = Pattern.compile("^(?<time>\\d+\\.?\\d*)" +
	                                                                   // These can be in any order
	                                                                   // This non-capturing group matches one element each pass
	                                                                   " (?:\"(?<title>[^\"]*)\"|" +
	                                                                   " sync /(?<sync>.*)/|" +
	                                                                   " window (?:(?<windowStart>\\d*\\.?\\d*),)?(?<windowEnd>\\d*\\.?\\d*)|" +
	                                                                   " (?<forcejump>force)?jump (?:\"(?<jumplabel>[^\"]*)\"|(?<jump>\\d*\\.?\\d*))|" +
	                                                                   " (?<eventType>[A-Z].*) (?<eventCond>\\{[^}]*})|" +
	                                                                   " duration (?<duration>\\d*\\.?\\d*)" +
	                                                                   ")*(?:$|\\s*#.*$)"
	);

	private static final Pattern timelineLabelPattern = Pattern.compile("^(?<time>\\d+\\.?\\d*) label \"(?<label>[^\"]*)\"");

	private TimelineParser() {
	}

	public static List<TimelineEntry> parseMultiple(Collection<String> line) {
		return line.stream().map(TimelineParser::parseRaw).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static @Nullable TimelineEntry parseRaw(String line) {
		try {
			// Skip these for now
			if (line.trim().isEmpty() || line.startsWith("hideall")) {
				return null;
			}

			Matcher labelMatcher = timelineLabelPattern.matcher(line);
			if (labelMatcher.matches()) {
				String timeRaw = labelMatcher.group("time");
				double time;
				try {
					time = Double.parseDouble(timeRaw);
				}
				catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Not a valid time: %s (entire line: %s)".formatted(timeRaw, line));
				}
				return new TextFileLabelEntry(time, labelMatcher.group("label"));
			}
			Matcher matcher = timelinePatternLong.matcher(line);
			if (matcher.matches()) {
				Pattern sync;
				log.trace("Matching line: {}", line);
				String timeRaw = matcher.group("time");
				String title = matcher.group("title");
				String syncRaw = matcher.group("sync");
				String windowStartRaw = matcher.group("windowStart");
				String windowEndRaw = matcher.group("windowEnd");
				String jumpRaw = matcher.group("jump");
				String jumpLabel = matcher.group("jumplabel");
				String forceJumpRaw = matcher.group("forcejump");
				String durationRaw = matcher.group("duration");
				String eventTypeRaw = matcher.group("eventType");
				EventSyncController esc = null;

				if ("--sync--".equals(title)) {
					title = null;
				}
				if (syncRaw == null) {
					sync = null;
					if (eventTypeRaw != null) {
						esc = CbEventFmt.parseRaw(eventTypeRaw, matcher.group("eventCond"));
					}
				}
				else {
					sync = Pattern.compile(syncRaw, Pattern.CASE_INSENSITIVE);
				}
				TimelineWindow window;
				if (windowEndRaw != null) {
					double windowStart;
					if (windowStartRaw == null) {
						windowStart = 0;
					}
					else {
						windowStart = Double.parseDouble(windowStartRaw);
					}
					window = new TimelineWindow(windowStart, Double.parseDouble(windowEndRaw));
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
				boolean forceJump = forceJumpRaw != null;
				return new TextFileTimelineEntry(time, title, sync, doubleOrNull(durationRaw), window, doubleOrNull(jumpRaw), jumpLabel, forceJump, esc);
			}
			log.trace("Line did not match: {}", line);
			return null;
		}
		catch (Throwable t) {
			throw new IllegalArgumentException("Error parsing timeline entry '%s'".formatted(line), t);
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
