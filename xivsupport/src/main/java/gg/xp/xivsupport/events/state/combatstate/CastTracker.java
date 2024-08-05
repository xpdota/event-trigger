package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public class CastTracker implements HasDuration {
	private final AbilityCastStart cast;
	private @Nullable Instant currentTimeOverride;
	private @Nullable BaseEvent end;

	public CastTracker(AbilityCastStart cast) {
		this.cast = cast;
	}

	public AbilityCastStart getCast() {
		return cast;
	}

	public @Nullable BaseEvent getEnd() {
		return end;
	}

	void setEnd(@Nullable BaseEvent end) {
		this.end = end;
	}

	public CastResult getResult() {
		if (end == null) {
			return CastResult.IN_PROGRESS;
		}
		else if (end instanceof AbilityCastCancel) {
			return CastResult.INTERRUPTED;
		}
		else if (end instanceof AbilityUsedEvent) {
			return CastResult.SUCCESS;
		}
		else if (cast.wouldBeExpired()) {
			return CastResult.SUCCESS;
		}
		else {
			return CastResult.UNKNOWN;
		}
	}

	public Duration getCastDuration() {
		return cast.getInitialDuration();
	}

	public Duration getElapsedDuration() {
		return (switch (getResult()) {
//			case SUCCESS -> getCastDuration();
			case INTERRUPTED -> Duration.between(cast.getEffectiveHappenedAt(), end.getEffectiveHappenedAt());
			case SUCCESS, IN_PROGRESS, UNKNOWN -> getEstimatedElapsedDuration();
		});
	}

	public CastTracker withNewCurrentTime(Instant timeBasis) {
		CastTracker out = new CastTracker(cast);
		// Only include the ending event IF it would have occurred at the given time
		if (end != null && end.getEffectiveHappenedAt().compareTo(timeBasis) < 0) {
			out.end = end;
		}
		out.currentTimeOverride = timeBasis;
		return out;
	}

	@Override
	public Duration getInitialDuration() {
		return getCastDuration();
	}

	@Override
	public Duration getEffectiveTimeSince() {
		if (currentTimeOverride == null) {
			return cast.getEffectiveTimeSince();
		}
		else {
			return Duration.between(cast.getEffectiveHappenedAt(), currentTimeOverride);
		}
	}
}
