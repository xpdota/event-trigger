package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Event indicating that a floor marker (1234ABCD) has been placed
 */
public class MarkerPlacedEvent extends BaseEvent implements HasSourceEntity, HasPrimaryValue, HasFloorMarker {
	@Serial
	private static final long serialVersionUID = -769814754258708514L;
	private final FloorMarker marker;
	private final Position position;
	private final XivCombatant placer;

	public MarkerPlacedEvent(FloorMarker marker, Position position, XivCombatant placer) {
		this.marker = marker;
		this.position = position;
		this.placer = placer;
	}

	@Override
	public FloorMarker getMarker() {
		return marker;
	}

	@Override
	public @Nullable String extraDescription() {
		return getPrimaryValue();
	}

	public Position getPosition() {
		return position;
	}

	@Override
	public XivCombatant getSource() {
		return placer;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("Marker %s placed at %s, %s, %s", marker.getName(), position.x(), position.y(), position.z());
	}
}
