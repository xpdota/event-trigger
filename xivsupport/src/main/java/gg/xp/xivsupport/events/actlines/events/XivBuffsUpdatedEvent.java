package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.io.Serial;

@SystemEvent
public class XivBuffsUpdatedEvent extends BaseEvent implements XivStateChange {
	@Serial
	private static final long serialVersionUID = -4921893730737138894L;

	@Override
	public boolean shouldSave() {
		return false;
	}
}
