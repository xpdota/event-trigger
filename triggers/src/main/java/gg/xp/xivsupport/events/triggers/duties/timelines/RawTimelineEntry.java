package gg.xp.xivsupport.events.triggers.duties.timelines;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public record RawTimelineEntry(
		double number,
		String name,
		double duration,
		@Nullable Window window,
		@Nullable Double jump
) {
}
