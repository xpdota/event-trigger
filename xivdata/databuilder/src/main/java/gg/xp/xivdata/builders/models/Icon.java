package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.clienttypes.XivApiStruct;

public interface Icon extends XivApiStruct {
	@XivApiField("id")
	int getId();
}
