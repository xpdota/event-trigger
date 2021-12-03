package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

@SystemEvent
public class XivStateRecalculatedEvent extends BaseEvent implements XivStateChange {
	private static final long serialVersionUID = -6310170328601920843L;

	@Override
	public boolean shouldSave() {
		return false;
	}
}

