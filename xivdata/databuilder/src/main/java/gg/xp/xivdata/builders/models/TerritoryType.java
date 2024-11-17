package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.clienttypes.XivApiObject;

public interface TerritoryType extends XivApiObject {
	ContentFinderCondition getContentFinderCondition();

	PlaceName getPlaceName();

}
