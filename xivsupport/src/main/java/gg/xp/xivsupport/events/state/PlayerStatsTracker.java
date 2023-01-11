package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.PlayerStats;
import gg.xp.xivsupport.events.actlines.events.PlayerStatsUpdatedEvent;
import org.jetbrains.annotations.Nullable;

public class PlayerStatsTracker {
	private volatile PlayerStats stats;

	@HandleEvents
	public void playerStats(EventContext context, PlayerStatsUpdatedEvent psue) {
		this.stats = psue.getStats();
	}

	public @Nullable PlayerStats getStats() {
		return stats;
	}
}
