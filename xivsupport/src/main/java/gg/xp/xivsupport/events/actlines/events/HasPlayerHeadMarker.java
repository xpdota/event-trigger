package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import org.jetbrains.annotations.Nullable;

public interface HasPlayerHeadMarker {

	MarkerSign getMarker();

	default @Nullable String extraDescription() {
		return null;
	}
}
