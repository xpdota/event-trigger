package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivZone;

import java.io.Serial;

/**
 * Represents a zone change
 */
public class ZoneChangeEvent extends BaseEvent implements XivStateChange, HasPrimaryValue, HasZone {
	@Serial
	private static final long serialVersionUID = 3743475710853003703L;
	private final XivZone zone;

	public ZoneChangeEvent(XivZone zone) {
		this.zone = zone;
	}

	@Override
	public XivZone getZone() {
		return zone;
	}

	@Override
	public String getPrimaryValue() {
		return String.valueOf(zone);
	}
}
