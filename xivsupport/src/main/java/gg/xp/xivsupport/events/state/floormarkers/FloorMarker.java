package gg.xp.xivsupport.events.state.floormarkers;

public enum FloorMarker {
	A,
	B,
	C,
	D,
	ONE("1"),
	TWO("2"),
	THREE("3"),
	FOUR("4");

	private final String name;

	FloorMarker() {
		name = toString();
	}

	FloorMarker(String nameOverride) {
		name = nameOverride;
	}

	public String getName() {
		return name;
	}

}
