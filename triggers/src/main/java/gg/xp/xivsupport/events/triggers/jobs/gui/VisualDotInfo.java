package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;

public class VisualDotInfo implements CurrentMaxPair, LabelOverride {

	private final BuffApplied buff;
	private final String labelOverride;

	public VisualDotInfo(BuffApplied buff, String labelOverride) {
		this.buff = buff;
		this.labelOverride = labelOverride;
	}

	public VisualDotInfo(BuffApplied buff) {
		this(buff, null);
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
	public long getCurrent() {
		return buff.getEstimatedElapsedDuration().toMillis();
	}

	@Override
	public long getMax() {
		return buff.getInitialDuration().toMillis();
	}
}
