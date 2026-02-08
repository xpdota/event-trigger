package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.annotations.XivApiRaw;
import gg.xp.xivapi.clienttypes.XivApiObject;

public interface World extends XivApiObject {
	String getName();

	@XivApiRaw
	int getDataCenter();

	@XivApiField("IsPublic")
	boolean isPublic();
}
