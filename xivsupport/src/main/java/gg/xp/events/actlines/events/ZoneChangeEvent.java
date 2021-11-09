package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivZone;

public class ZoneChangeEvent extends BaseEvent implements XivStateChange {
	private static final long serialVersionUID = 3743475710853003703L;
	private final XivZone zone;

	public ZoneChangeEvent(XivZone zone) {
		this.zone = zone;
	}

	public XivZone getZone() {
		return zone;
	}
}
