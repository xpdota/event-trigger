package gg.xp.events.actlines;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

public class PlayerChangeEvent extends BaseEvent {
	private final XivEntity player;

	public PlayerChangeEvent(XivEntity player) {
		this.player = player;
	}

	public XivEntity getPlayer() {
		return player;
	}
}
