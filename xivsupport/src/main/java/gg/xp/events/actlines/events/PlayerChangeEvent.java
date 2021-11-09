package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

public class PlayerChangeEvent extends BaseEvent implements XivStateChange {
	private static final long serialVersionUID = -7335295270596538232L;
	private final XivEntity player;

	public PlayerChangeEvent(XivEntity player) {
		this.player = player;
	}

	public XivEntity getPlayer() {
		return player;
	}
}
