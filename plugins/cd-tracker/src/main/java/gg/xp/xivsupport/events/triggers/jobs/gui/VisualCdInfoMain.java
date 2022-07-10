package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.BasicCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisualCdInfoMain implements VisualCdInfo {

	private final @NotNull BasicCooldownDescriptor cd;
	private final @Nullable BaseEvent basisEvent;
	private final @Nullable BuffApplied buffApplied;
	private final @Nullable Instant replenishedAt;

	public VisualCdInfoMain(@NotNull BasicCooldownDescriptor cd, @Nullable BaseEvent basisEvent, @Nullable BuffApplied buffApplied, @Nullable Instant replenishedAt) {
		this.cd = cd;
		this.basisEvent = basisEvent;
		this.buffApplied = buffApplied;
		this.replenishedAt = replenishedAt;
	}

	public VisualCdInfoMain(CooldownStatus status) {
		this(status.cdKey().getCooldown(), status.used(), status.buff(), status.replenishedAt());
	}

	public VisualCdInfoMain(BasicCooldownDescriptor cd) {
		this(cd, null, null, null);
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
		if (buffApplied == null) {
			// Ready
			if (getCurrent() == getMax()) {
				return "Ready!";
			}
			// On CD
			return String.format("%.1f", ((double) (getMax() - getCurrent())) / 1000.0f);
		}
		else {
			Double durOvr = cd.getDurationOverride();
			// Duration override (e.g. BRD songs)
			if (durOvr != null) {
				// TODO: move this into buff tracking, so we can have correct buff info everywhere
				long durOvrMs = (long) (durOvr * 1000);
				if (basisEvent == null) {
					return "?";
				}
				long effectiveDur = Math.max(0, durOvrMs - basisEvent.getEffectiveTimeSince().toMillis());
				return String.format("%.1f", effectiveDur / 1000.0);
			}
			// Pre-app
			if (buffApplied.isPreApp()) {
				return "...";
			}
			// Actual buff duration
			return String.format("%.1f", ((double) (buffApplied.getEstimatedRemainingDuration().toMillis())) / 1000.0f);
		}
	}

	@Override
	public long getCurrent() {
		if (basisEvent == null) {
			return getMax();
		}
		if (buffApplied != null) {
			Double durOvr = cd.getDurationOverride();
			if (durOvr != null) {
				// TODO: move this into buff tracking, so we can have correct buff info everywhere
				long durOvrMs = (long) (durOvr * 1000);
				return Math.max(0, durOvrMs - basisEvent.getEffectiveTimeSince().toMillis());
			}
			return buffApplied.getEstimatedRemainingDuration().toMillis();
		}
		else {
			long durMillis = basisEvent.getEffectiveTimeSince().toMillis();
			return Math.min(Math.max(0, durMillis), getMax());
		}
	}

	@Override
	public @Nullable BuffApplied getBuffApplied() {
		return buffApplied;
	}

	@Override
	public long getMax() {
		if (buffApplied != null) {
			Double durOvr = cd.getDurationOverride();
			if (durOvr != null) {
				return (long) (durOvr * 1000.0);
			}
			return buffApplied.getInitialDuration().toMillis();
		}
		//noinspection NumericCastThatLosesPrecision
		return (long) (cd.getCooldown() * 1000L);
	}

	@Override
	public boolean useChargeDisplay() {
		return cd.getMaxCharges() > 1;
	}

	@Override
	public long getPrimaryAbilityId() {
		return cd.getPrimaryAbilityId();
	}

	public List<VisualCdInfo> makeChargeInfo() {
		if (useChargeDisplay()) {
			int numCharges = cd.getMaxCharges();
			List<VisualCdInfo> out = new ArrayList<>(numCharges);
			for (int i = 0; i < numCharges; i++) {
				out.add(new VisualCdInfoCharge(cd, basisEvent, null, replenishedAt, i));
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
		return basisEvent == null || (buffApplied != null
				|| (replenishedAt != null && basisEvent.effectiveTimeNow().isBefore(replenishedAt.plus(10, ChronoUnit.SECONDS)))
				|| basisEvent.getEffectiveTimeSince().toMillis() < (cd.getCooldown() * 1000L) + 10_000L);
//				|| Duration.between(abilityEvent.getEnqueuedAt().plusMillis((long) (cd.getCooldown() * 1000L)), Instant.now()).toMillis() < 10_000);
	}

}
