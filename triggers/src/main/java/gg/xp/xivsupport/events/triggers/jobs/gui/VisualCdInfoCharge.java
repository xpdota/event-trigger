package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: value in supporting buffs?
public class VisualCdInfoCharge implements VisualCdInfo {

	private final Cooldown cd;
	private final AbilityUsedEvent abilityEvent;
	private final BuffApplied buffApplied;
	private final Instant replenishedAt;
	private final int chargeNum;
	private final Instant start;
	private final Instant end;

	public VisualCdInfoCharge(Cooldown cd, @NotNull AbilityUsedEvent abilityEvent, @Nullable BuffApplied buffApplied, @NotNull Instant replenishedAt, int chargeNum) {
		this.cd = cd;
		this.abilityEvent = abilityEvent;
		this.buffApplied = buffApplied;
		this.replenishedAt = replenishedAt;
		this.chargeNum = chargeNum;
		// e.g. for a 3 charge ability, this value should be 3 for the first charge, then 2, then 1
		int chargeOffset = cd.getMaxCharges() - chargeNum;
		this.start = replenishedAt.minus(cd.getCooldownAsDuration().multipliedBy(chargeOffset));
		this.end = replenishedAt.minus(cd.getCooldownAsDuration().multipliedBy(chargeOffset - 1));
	}

	private Instant getNow() {
		return abilityEvent.effectiveTimeNow();
	}

	@Override
	public AbilityUsedEvent getEvent() {
		return abilityEvent;
	}

	@Override
	public String getLabel() {
		if (getCurrent() == getMax()) {
			return "Ready!";
		}
		return String.format("%.1f", ((double) (getMax() - getCurrentUnbounded())) / 1000.0f);
	}

	@Override
	public long getCurrent() {
		long durMillis = Duration.between(start, getNow()).toMillis();
		return Math.min(Math.max(0, durMillis), getMax());
	}

	private long getCurrentUnbounded() {
		long durMillis = Duration.between(start, getNow()).toMillis();
		return Math.min((durMillis), getMax());
	}

	@Override
	public BuffApplied getBuffApplied() {
		return buffApplied;
	}

	@Override
	public long getMax() {
		return cd.getCooldownAsDuration().toMillis();
	}

	@Override
	public boolean useChargeDisplay() {
		return false; // This IS the charge display
	}

	@Override
	public Cooldown getCd() {
		return cd;
	}

	@Override
	public boolean stillValid() {
		// TODO: does this need to exist? remove from interface maybe?
		return true;
	}

}
