package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;

public class PlayerStatsUpdatedEvent extends BaseEvent {

	private final PlayerStats stats;

	public PlayerStatsUpdatedEvent(PlayerStats stats) {
		this.stats = stats;
	}

	public PlayerStats getStats() {
		return stats;
	}
}
