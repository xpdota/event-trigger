package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VisualCdInfo extends CurrentMaxPair, LabelOverride {
	@Nullable AbilityUsedEvent getEvent();

	@Nullable BuffApplied getBuffApplied();

	boolean useChargeDisplay();

	@NotNull Cooldown getCd();

	boolean stillValid();
}
