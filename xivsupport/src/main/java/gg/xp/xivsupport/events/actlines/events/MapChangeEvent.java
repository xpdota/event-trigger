package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.XivMap;

import java.io.Serial;

/**
 * Represents a map change event
 */
public class MapChangeEvent extends BaseEvent implements XivStateChange, HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -5578740136371565264L;
	private final XivMap map;
	private final boolean isSubChange;

	public MapChangeEvent(XivMap map) {
		this(map, false);
	}

	public MapChangeEvent(XivMap map, boolean isSubChange) {
		this.map = map;
		this.isSubChange = isSubChange;
	}

	public XivMap getMap() {
		return map;
	}

	/**
	 * @return Whether this event represents an 'in-place' minimap change, where we have not actually moved to a new
	 * area, but rather only the minimap has changed, usually due to the shape of the arena changing. e.g. CoD Chaotic.
	 */
	public boolean isSubChange() {
		return isSubChange;
	}

	@Override
	public String getPrimaryValue() {
		return String.valueOf(map);
	}
}
