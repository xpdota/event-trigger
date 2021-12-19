package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;

import java.time.Duration;
import java.time.Instant;

public class VisualCdInfo implements CurrentMaxPair, LabelOverride {

	private final Cooldown cd;
	private final AbilityUsedEvent abilityEvent;
	private final BuffApplied buffApplied;

	public VisualCdInfo(Cooldown cd, AbilityUsedEvent abilityEvent, BuffApplied buffApplied) {
		this.cd = cd;
		this.abilityEvent = abilityEvent;
		this.buffApplied = buffApplied;
	}

	public AbilityUsedEvent getEvent() {
		return abilityEvent;
	}

	@Override
	public String getLabel() {
		if (buffApplied == null) {
			if (getCurrent() == getMax()) {
				return "Ready!";
			}
			return String.format("%.1f", ((double) (getMax() - getCurrent())) / 1000.0f);
		}
		else {
			if (buffApplied.isPreApp()) {
				return "...";
			}
			return String.format("%.1f", ((double) (buffApplied.getEstimatedRemainingDuration().toMillis())) / 1000.0f);
		}
	}

	@Override
	public long getCurrent() {
		if (buffApplied != null) {
			return buffApplied.getEstimatedRemainingDuration().toMillis();
		}
		Instant start = getEvent().getPumpedAt();
		Instant now = Instant.now();
		long durMillis = Duration.between(start, now).toMillis();
		return Math.min(Math.max(0, durMillis), getMax());
	}

	public BuffApplied getBuffApplied() {
		return buffApplied;
	}

	@Override
	public long getMax() {
		if (buffApplied != null) {
			return buffApplied.getInitialDuration().toMillis();
		}
		//noinspection NumericCastThatLosesPrecision
		return (long) (cd.getCooldown() * 1000L);
	}

	public boolean stillValid() {

		// TODO: make hang time a setting
		return (buffApplied != null
				|| Duration.between(abilityEvent.getEnqueuedAt().plusMillis((long) (cd.getCooldown() * 1000L)), Instant.now()).toMillis() < 10_000);
	}

}
