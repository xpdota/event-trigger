package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivEntity;

import java.io.Serial;

// Full player info comes from both player + combatant info, so this is NOT the event you want to listen to

/**
 * A very raw event that represents the primary player changing. Note that you should really just query
 * {@link XivState} for this information.
 */
public class RawPlayerChangeEvent extends BaseEvent implements XivStateChange {
	@Serial
	private static final long serialVersionUID = -7335295270596538232L;
	private final XivEntity player;

	public RawPlayerChangeEvent(XivEntity player) {
		this.player = player;
	}

	public XivEntity getPlayer() {
		return player;
	}
}
