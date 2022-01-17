package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisualCdInfoMain implements VisualCdInfo {

	private final Cooldown cd;
	private final AbilityUsedEvent abilityEvent;
	private final BuffApplied buffApplied;
	private final Instant replenishedAt;

	public VisualCdInfoMain(Cooldown cd, @NotNull AbilityUsedEvent abilityEvent, @Nullable BuffApplied buffApplied, @Nullable Instant replenishedAt) {
		this.cd = cd;
		this.abilityEvent = abilityEvent;
		this.buffApplied = buffApplied;
		this.replenishedAt = replenishedAt;
	}

	@Override
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
		long durMillis = getEvent().getEffectiveTimeSince().toMillis();
		return Math.min(Math.max(0, durMillis), getMax());
	}

	@Override
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

	@Override
	public boolean useChargeDisplay() {
		return replenishedAt != null && cd.getMaxCharges() > 1;
	}

	public List<VisualCdInfo> makeChargeInfo() {
		if (useChargeDisplay()) {
			int numCharges = cd.getMaxCharges();
			List<VisualCdInfo> out = new ArrayList<>(numCharges);
			for (int i = 0; i < numCharges; i++) {
				out.add(new VisualCdInfoCharge(cd, abilityEvent, null, replenishedAt, i));
			}
			return out;
		}
		else {
			return Collections.singletonList(this);
		}
	}

	@Override
	public boolean stillValid() {

		// TODO: make hang time a setting
		// TODO: charge based abilities instantly disappear
		return (buffApplied != null
				|| (replenishedAt != null && getEvent().effectiveTimeNow().isBefore(replenishedAt.plus(10, ChronoUnit.SECONDS)))
				|| getEvent().getEffectiveTimeSince().toMillis() < (cd.getCooldown() * 1000L) + 10_000L);
//				|| Duration.between(abilityEvent.getEnqueuedAt().plusMillis((long) (cd.getCooldown() * 1000L)), Instant.now()).toMillis() < 10_000);
	}

}
