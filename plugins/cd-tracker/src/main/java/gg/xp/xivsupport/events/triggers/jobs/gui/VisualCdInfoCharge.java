package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.BasicCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

// TODO: value in supporting buffs?
public class VisualCdInfoCharge implements VisualCdInfo {

	private final @NotNull BasicCooldownDescriptor cd;
	private final @Nullable BaseEvent basisEvent;
	private final @Nullable BuffApplied buffApplied;
	private final @Nullable Instant replenishedAt;
	private final int chargeNum;
	private final Instant start;
	private final Instant end;

	public VisualCdInfoCharge(BasicCooldownDescriptor cd, @Nullable BaseEvent basisEvent, @Nullable BuffApplied buffApplied, @Nullable Instant replenishedAt, int chargeNum) {
		this.cd = cd;
		this.basisEvent = basisEvent;
		this.buffApplied = buffApplied;
		this.replenishedAt = replenishedAt;
		this.chargeNum = chargeNum;
		// e.g. for a 3 charge ability, this value should be 3 for the first charge, then 2, then 1
		int chargeOffset = cd.getMaxCharges() - chargeNum;
		if (replenishedAt == null) {
			// It works
			start = Instant.EPOCH;
			end = Instant.EPOCH.plusMillis(1000);
		}
		else {
			this.start = replenishedAt.minus(cd.getCooldownAsDuration().multipliedBy(chargeOffset));
			this.end = replenishedAt.minus(cd.getCooldownAsDuration().multipliedBy(chargeOffset - 1));
		}
	}

	private Instant getNow() {
		return basisEvent.effectiveTimeNow();
	}

	@Override
	public @Nullable AbilityUsedEvent getEvent() {
		if (basisEvent instanceof AbilityUsedEvent aue) {
			return aue;
		}
		return null;
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
//		if (abilityEvent == null) {
//			return getMax();
//		}
//		long durMillis = Duration.between(start, abilityEvent.effectiveTimeNow()).toMillis();
//		return Math.min(Math.max(0, durMillis), getMax());
		return Math.max(0, getCurrentUnbounded());
	}

	private long getCurrentUnbounded() {
		if (basisEvent == null) {
			return getMax();
		}
		long durMillis = Duration.between(start, basisEvent.effectiveTimeNow()).toMillis();
		return Math.min((durMillis), getMax());
	}

	@Override
	public @Nullable BuffApplied getBuffApplied() {
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
	public long getPrimaryAbilityId() {
		return cd.getPrimaryAbilityId();
	}

	@Override
	public boolean stillValid() {
		// TODO: does this need to exist? remove from interface maybe?
		return true;
	}

	@Override
	public List<? extends VisualCdInfo> makeChargeInfo() {
		return Collections.emptyList();
	}

}
