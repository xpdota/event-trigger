package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class PlayerStatsUpdatedEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -1731295713727741955L;
	private final PlayerStats stats;

	public PlayerStatsUpdatedEvent(PlayerStats stats) {
		this.stats = stats;
	}

	public PlayerStats getStats() {
		return stats;
	}
}
