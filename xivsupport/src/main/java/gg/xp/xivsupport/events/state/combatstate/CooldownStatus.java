package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.CdTrackingKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record CooldownStatus(
		@NotNull CdTrackingKey cdKey,
		@Nullable AbilityUsedEvent used,
		@Nullable BuffApplied buff,
		@Nullable Instant replenishedAt
) {
}
