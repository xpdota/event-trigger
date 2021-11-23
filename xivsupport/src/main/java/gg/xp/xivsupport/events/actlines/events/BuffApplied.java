package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.time.Duration;
import java.time.Instant;

// TODO: track new application vs refresh
// Note that stacks decreasing (e.g. Embolden) still counts as "Application".
public class BuffApplied extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasStatusEffect {
	private static final long serialVersionUID = -3698392943125561045L;
	private final XivStatusEffect buff;
	private final Duration duration;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long stacks;
	private boolean isRefresh;

	public BuffApplied(XivStatusEffect buff, double durationRaw, XivCombatant source, XivCombatant target, long stacks) {
		this.buff = buff;
		this.duration = Duration.ofMillis((long) (durationRaw * 1000.0));
		this.source = source;
		this.target = target;
		this.stacks = stacks;
	}

	@Override
	public XivStatusEffect getBuff() {
		return buff;
	}

	public Duration getInitialDuration() {
		return duration;
	}

	public Duration getEstimatedElapsedDuration() {
		Duration delta = Duration.between(getHappenedAt(), Instant.now());
		// If negative, return zero. If longer than expected duration, return duration.
		if (delta.isNegative()) {
			return Duration.ZERO;
		}
		else if (delta.compareTo(duration) > 0) {
			return duration;
		}
		else {
			return delta;
		}
	}

	public Duration getEstimatedRemainingDuration() {
		return getInitialDuration().minus(getEstimatedElapsedDuration());
	}

	public Duration getEstimatedTimeSinceExpiry() {
		Duration elapsed = Duration.between(getHappenedAt(), Instant.now());
		return elapsed.minus(duration);
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public long getStacks() {
		return stacks;
	}

	public boolean isRefresh() {
		return isRefresh;
	}

	public void setIsRefresh(boolean refresh) {
		isRefresh = refresh;
	}

	@Override
	public String toString() {
		return "BuffApplied{" +
				"buff=" + buff +
				", duration=" + duration +
				", source=" + source +
				", target=" + target +
				", stacks=" + stacks +
				", isRefresh=" + isRefresh +
				'}';
	}
}
