package gg.xp.events.actlines;

import gg.xp.events.Event;
import gg.xp.events.XivEntity;

public class PlayerChangeEvent implements Event {
	private final XivEntity player;

	public PlayerChangeEvent(XivEntity player) {
		this.player = player;
	}

	public XivEntity getPlayer() {
		return player;
	}
}
