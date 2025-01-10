package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.annotations.XivApiRaw;
import gg.xp.xivapi.annotations.XivApiTransientField;
import gg.xp.xivapi.clienttypes.XivApiObject;

public interface Action extends XivApiObject {
	String getName();
	Icon getIcon();
	@XivApiField("Cast100ms")
	int getCastRaw();
	@XivApiField("Recast100ms")
	int getRecastRaw();
	int getMaxCharges();

	@XivApiTransientField
	String getDescription();

	@XivApiField("IsPlayerAction")
	boolean isPlayerAbility();

	@XivApiRaw
	@XivApiField("ClassJobCategory")
	int getCategoryRaw();

	int getCastType();
	int getEffectRange();
	int getXAxisModifier();
	Omen getOmen();
}
