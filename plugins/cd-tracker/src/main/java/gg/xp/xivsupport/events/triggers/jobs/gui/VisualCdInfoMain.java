package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.BasicCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import gg.xp.xivsupport.models.CurrentMaxPair;
import gg.xp.xivsupport.models.CurrentMaxPairImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
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
		CurrentMaxPair buffStatus = getBuffStatus();
		if (buffStatus == null) {
			// Ready
			CurrentMaxPair as = getAbilityStatus();
			// Hasn't been used yet
			if (as == null) {
				return "Ready!";
			}
			if (as.current() == as.max()) {
				return "Ready!";
			}
			// On CD
			return String.format("%.1f", ((double) (as.max() - as.current())) / 1000.0f);
		}
		else {
			// Pre-app
			if (isPreApp()) {
				return "...";
			}
			// Actual buff duration
			return String.format("%.1f", ((double) (buffStatus.current()) / 1000.0f));
		}
	}

	private boolean isPreApp() {
		return buffApplied != null && buffApplied.isPreApp();
	}

	@Override
	public long current() {
		return getBestPair().current();
	}

	private CurrentMaxPair getBestPair() {
		CurrentMaxPair bs = getBuffStatus();
		if (bs != null) {
			return bs;
		}
		CurrentMaxPair as = getAbilityStatus();
		if (as != null) {
			return as;
		}
		return getFallback();
	}

	private @Nullable CurrentMaxPair getAbilityStatus() {
		if (basisEvent == null) {
			return null;
		}
		else {
			long max = (long) (cd.getCooldown() * 1000L);
			long current = Math.min(Math.max(0, basisEvent.getEffectiveTimeSince().toMillis()), max);
			return new CurrentMaxPairImpl(current, max);
		}
	}

	private @Nullable CurrentMaxPair getBuffStatus() {
		// Cases that need to be covered:
		// 1. Normal, buff active - return info based on buff
		// 2. Normal, buff not active - return null
		// 3. Dur override, with buff - return hybrid of buff + fixed info
		// 4. Dur override, no buff - return completely fixed info
		// 5. No buff, with no dur override - useless (TODO warn user if they've set it up like this?)
		Double durOvr = cd.getDurationOverride();
		if (cd.noStatusEffect()) {
			if (durOvr == null) {
				// Case #5
				return null;
			}
			BaseEvent base = basisEvent;
			if (base == null) {
				return null;
			}
			long since = base.getEffectiveTimeSince().toMillis();
			long fakeDur = (long) (durOvr * 1000L);

			// Case #4
			if (since > fakeDur) {
				return null;
			}
			else {
				long remaining = fakeDur - since;
				if (remaining < 0) {
					return null;
				}
				return new CurrentMaxPairImpl(remaining, fakeDur);
			}
		}
		else {
			if (buffApplied == null) {
				// Case #2
				return null;
			}
			if (buffApplied.isPreApp()) {
				return new CurrentMaxPairImpl(100, 100);
			}
			final long max;
			final long current;
			if (durOvr == null) {
				// Case #1
				max = buffApplied.getInitialDuration().toMillis();
				current = buffApplied.getEstimatedRemainingDuration().toMillis();
			}
			else {
				// Case #3
				max = (long) (durOvr * 1000L);
				long since = buffApplied.getEffectiveTimeSince().toMillis();
				if (since > max) {
					return null;
				}
				current = max - since;
			}
			return new CurrentMaxPairImpl(current, max);
		}
	}

	private CurrentMaxPair getFallback() {
		long num = (long) (cd.getCooldown() * 1000L);
		return new CurrentMaxPairImpl(num, num);
	}

	@Override
	public @Nullable BuffApplied getBuffApplied() {
		return buffApplied;
	}

	@Override
	public long max() {
		return getBestPair().max();
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
	public CdStatus getStatus() {
		if (isPreApp()) {
			return CdStatus.BUFF_PREAPP;
		}
		else if (getBuffStatus() != null) {
			return CdStatus.BUFF_ACTIVE;
		}
		else {
			CurrentMaxPair as = getAbilityStatus();
			if (as == null) {
				return CdStatus.NOT_YET_USED;
			}
			else if (as.isFull()) {
				return CdStatus.READY;
			}
			else {
				return CdStatus.ON_COOLDOWN;
			}
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
