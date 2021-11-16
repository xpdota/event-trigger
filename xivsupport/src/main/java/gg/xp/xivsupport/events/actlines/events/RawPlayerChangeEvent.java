package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.models.XivEntity;

// Full player info comes from both player + combatant info, so this is NOT the event you want to listen to
public class RawPlayerChangeEvent extends BaseEvent implements XivStateChange {
	private static final long serialVersionUID = -7335295270596538232L;
	private final XivEntity player;

	public RawPlayerChangeEvent(XivEntity player) {
		this.player = player;
	}

	public XivEntity getPlayer() {
		return player;
	}
}
