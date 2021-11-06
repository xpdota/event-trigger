package gg.xp.events.actlines;

import gg.xp.events.Event;
import gg.xp.events.XivZone;

public class ZoneChangeEvent implements Event {
	private final XivZone zone;

	public ZoneChangeEvent(XivZone zone) {
		this.zone = zone;
	}

	public XivZone getZone() {
		return zone;
	}
}
