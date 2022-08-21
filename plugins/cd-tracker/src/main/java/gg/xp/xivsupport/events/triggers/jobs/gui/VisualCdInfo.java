package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CurrentMaxPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface VisualCdInfo extends CurrentMaxPair, LabelOverride {
	@Nullable AbilityUsedEvent getEvent();

	@Nullable BuffApplied getBuffApplied();

	boolean useChargeDisplay();

	long getPrimaryAbilityId();

	boolean stillValid();

	List<? extends VisualCdInfo> makeChargeInfo();

	CdStatus getStatus();
}
