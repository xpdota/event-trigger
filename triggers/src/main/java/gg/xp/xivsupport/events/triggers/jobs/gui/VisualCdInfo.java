package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;

public interface VisualCdInfo extends CurrentMaxPair, LabelOverride {
	AbilityUsedEvent getEvent();

	BuffApplied getBuffApplied();

	boolean useChargeDisplay();

	Cooldown getCd();

	boolean stillValid();
}
