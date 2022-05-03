package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class CastTracker {
	private final AbilityCastStart cast;
	private @Nullable Event end;

	CastTracker(AbilityCastStart cast) {
		this.cast = cast;
	}

	public AbilityCastStart getCast() {
		return cast;
	}

	public @Nullable Event getEnd() {
		return end;
	}

	void setEnd(@Nullable Event end) {
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
			case SUCCESS -> getCastDuration();
			case INTERRUPTED -> Duration.between(cast.getEffectiveHappenedAt(), end.getEffectiveHappenedAt());
			case IN_PROGRESS, UNKNOWN -> cast.getEstimatedElapsedDuration();
		});
	}
}
