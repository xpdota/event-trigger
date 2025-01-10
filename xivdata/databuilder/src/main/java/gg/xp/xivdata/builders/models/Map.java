package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.clienttypes.XivApiObject;

public interface Map extends XivApiObject {
	int getOffsetX();
	int getOffsetY();
	int getSizeFactor();

	@XivApiField("Id")
	String mapPath();

	PlaceName getPlaceName();
	PlaceNameRegion getPlaceNameRegion();
	PlaceNameSub getPlaceNameSub();
}
