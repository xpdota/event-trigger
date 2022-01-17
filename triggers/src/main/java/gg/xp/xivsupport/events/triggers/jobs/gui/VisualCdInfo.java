package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;

public interface VisualCdInfo extends CurrentMaxPair, LabelOverride {
	AbilityUsedEvent getEvent();

	@Override
	String getLabel();

	@Override
	long getCurrent();

	BuffApplied getBuffApplied();

	@Override
	long getMax();

	boolean useChargeDisplay();

	boolean stillValid();
}
