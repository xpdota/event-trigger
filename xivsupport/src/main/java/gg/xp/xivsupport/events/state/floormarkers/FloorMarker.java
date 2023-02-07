package gg.xp.xivsupport.events.state.floormarkers;

import gg.xp.xivdata.data.*;

import java.net.URL;

public enum FloorMarker implements HasIconURL {
	A(61241),
	B(61242),
	C(61243),
	D(61247),
	ONE("1", 61244),
	TWO("2", 61245),
	THREE("3", 61246),
	FOUR("4", 61248);

	private final String name;
	private final int iconId;

	FloorMarker(int iconId) {
		name = toString();
		this.iconId = iconId;
	}

	FloorMarker(String nameOverride, int iconId) {
		name = nameOverride;
		this.iconId = iconId;
	}

	public String getName() {
		return name;
	}

	@Override
	public URL getIconUrl() {
		return IconUtils.iconUrl(iconId);
	}
}
