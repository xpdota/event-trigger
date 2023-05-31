package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import org.jetbrains.annotations.Nullable;

public interface HasFloorMarker {
	FloorMarker getMarker();

	default @Nullable String extraDescription() {
		return null;
	}
}
