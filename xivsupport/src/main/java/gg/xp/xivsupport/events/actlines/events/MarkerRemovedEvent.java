package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Event indicating that a floor marker (1234ABCD) has been removed
 */
public class MarkerRemovedEvent extends BaseEvent implements HasSourceEntity, HasPrimaryValue, HasFloorMarker {
	@Serial
	private static final long serialVersionUID = -1493204821969682620L;
	private final FloorMarker marker;
	private final XivCombatant placer;

	public MarkerRemovedEvent(FloorMarker marker, XivCombatant placer) {
		this.marker = marker;
		this.placer = placer;
	}

	@Override
	public FloorMarker getMarker() {
		return marker;
	}

	@Override
	public XivCombatant getSource() {
		return placer;
	}

	@Override
	public @Nullable String extraDescription() {
		return getPrimaryValue();
	}

	@Override
	public String getPrimaryValue() {
		return String.format("Marker %s removed", marker.getName());
	}
}
