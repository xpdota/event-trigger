package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.XivMap;

import java.io.Serial;

/**
 * Represents a map change event
 */
public class MapChangeEvent extends BaseEvent implements XivStateChange {
	@Serial
	private static final long serialVersionUID = -5578740136371565264L;
	private final XivMap map;

	public MapChangeEvent(XivMap map) {
		this.map = map;
	}

	public XivMap getMap() {
		return map;
	}
}
