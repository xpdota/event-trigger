package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiRaw;
import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.clienttypes.XivApiObject;

@XivApiSheet("WorldDCGroupType")
public interface DataCenter extends XivApiObject {
	String getName();
	@XivApiRaw
	int getRegion();
}
