package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import org.jetbrains.annotations.Nullable;

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
		// TODO
//		else if (end instanceof CastInterrupted) {
//			return CastResult.INTERRUPTED;
//		}
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
}
