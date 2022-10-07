package gg.xp.xivsupport.events.actlines.events;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public interface HasOptionalDelay {

	Instant getEffectiveHappenedAt();

	@Nullable Instant getPrecursorHappenedAt();

	default @Nullable Duration getDelay() {
		Instant pre = getPrecursorHappenedAt();
		if (pre == null) {
			return null;
		}
		return Duration.between(pre, getEffectiveHappenedAt());
	}

}
