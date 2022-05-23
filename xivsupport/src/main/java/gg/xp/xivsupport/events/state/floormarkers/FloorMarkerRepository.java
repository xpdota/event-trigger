package gg.xp.xivsupport.events.state.floormarkers;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.MarkerPlacedEvent;
import gg.xp.xivsupport.events.actlines.events.MarkerRemovedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.Position;

import java.util.EnumMap;

public class FloorMarkerRepository {

	private final Object lock = new Object();
	private final EnumMap<FloorMarker, Position> map = new EnumMap<>(FloorMarker.class);

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent zce) {
		synchronized (lock) {
			map.clear();
		}
	}

	@HandleEvents
	public void markerPlaced(EventContext context, MarkerPlacedEvent placed) {
		synchronized (lock) {
			map.put(placed.getMarker(), placed.getPosition());
		}
	}

	@HandleEvents
	public void markerRemoved(EventContext context, MarkerRemovedEvent removed) {
		synchronized (lock) {
			map.remove(removed.getMarker());
		}
	}

	public EnumMap<FloorMarker, Position> getMarkers() {
		synchronized (lock) {
			return map.clone();
		}
	}
}
