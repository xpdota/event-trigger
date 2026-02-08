package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;

/**
 * Represents an ability cast beginning i.e. castbar has appeared
 */
public class AbilityCastStart extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasDuration {
	@Serial
	private static final long serialVersionUID = -8156458501097189980L;
	private final XivAbility ability;
	private final XivCombatant source;
	private final XivCombatant target;
	private final Duration duration;
	private final Duration unmodifiedCastDuration;
	private final Duration extraCastDuration;
	private @Nullable DescribesCastLocation<AbilityCastStart> locationInfo;

	public AbilityCastStart(XivAbility ability, XivCombatant source, XivCombatant target, double duration) {
		this.ability = ability;
		this.source = source;
		this.target = target;
		this.duration = Duration.ofMillis((long) (duration * 1000.0));
		ActionInfo ai = ActionLibrary.forId(ability.getId());
		if (ai == null) {
			unmodifiedCastDuration = null;
			extraCastDuration = Duration.ZERO;
		}
		else {
			unmodifiedCastDuration = Duration.ofMillis(ai.castTimeRaw() * 100);
			extraCastDuration = Duration.ofMillis((long) (ai.getExtraCastTime() * 1000.0));
		}
	}

	@Override
	public XivAbility getAbility() {
		return ability;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public Duration getInitialDuration() {
		return duration;
	}

	public @Nullable Duration getUnmodifiedCastDuration() {
		return unmodifiedCastDuration;
	}

	public Duration getExtraCastDuration() {
		return extraCastDuration;
	}

	public boolean hasExtraCastDuration() {
		return extraCastDuration.compareTo(Duration.ZERO) > 0;
	}

	public Duration getInitialDurationWithExtra() {
		return duration.plus(extraCastDuration);
	}

	public Duration getRemainingDurationWithExtra() {
		return remainingDurationPlus(extraCastDuration);
	}

	public Duration getEstimatedTimeSinceExpiryWithExtra() {
		Duration elapsed = getEffectiveTimeSince();
		return elapsed.minus(getInitialDurationWithExtra());
	}

	public @Nullable DescribesCastLocation<AbilityCastStart> getLocationInfo() {
		return locationInfo;
	}

	public void setLocationInfo(@NotNull DescribesCastLocation<AbilityCastStart> locationInfo) {
		this.locationInfo = locationInfo;
	}

	@Override
	public String toString() {
		return "AbilityCastStart{" +
		       "ability=" + ability +
		       ", source=" + source +
		       ", target=" + target +
		       ", duration=" + duration +
		       ", locationInfo=" + locationInfo +
		       '}';
	}
}
