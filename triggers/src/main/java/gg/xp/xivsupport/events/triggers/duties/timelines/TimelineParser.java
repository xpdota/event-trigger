package gg.xp.xivsupport.events.triggers.duties.timelines;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class TimelineParser {

	public static @Nullable RawTimelineEntry parseLine(String line) {
		// Skip these for now
		if (line.trim().isEmpty() || line.startsWith("hideall")) {
			return null;
		}

		// TODO
		return null;

	}

}
