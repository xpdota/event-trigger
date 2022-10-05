package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.TickInfo;
import gg.xp.xivsupport.models.CurrentMaxPair;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class VisualDotInfo implements CurrentMaxPair, LabelOverride {

	private final BuffApplied buff;
	private final String labelOverride;
	private final @Nullable TickInfo tick;
	private final @Nullable Duration appDelay;

	public VisualDotInfo(BuffApplied buff, String labelOverride, @Nullable TickInfo tick, @Nullable Duration appDelay) {
		this.buff = buff;
		this.labelOverride = labelOverride;
		this.tick = tick;
		this.appDelay = appDelay;
	}

	public BuffApplied getEvent() {
		return buff;
	}

	@Override
	public String getLabel() {
		if (labelOverride == null) {
			return buff.getTarget().getName();
		}
		else {
			return labelOverride;
		}
	}

	@Override
	public long current() {
		return buff.getEstimatedElapsedDuration().toMillis();
	}

	@Override
	public long max() {
		return buff.getInitialDuration().toMillis();
	}


	public @Nullable TickInfo getTick() {
		return tick;
	}

	public @Nullable Duration getAppDelay() {
		return appDelay;
	}
}
