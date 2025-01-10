package gg.xp.xivdata.builders.models;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.clienttypes.XivApiObject;

@XivApiSheet("Status")
public interface StatusEffect extends XivApiObject {
	String getName();

	String getDescription();

	Icon getIcon();

	int getMaxStacks();

	@XivApiField("CanDispel")
	boolean canDispel();

	@XivApiField("IsPermanent")
	boolean isPermanent();

	int getPartyListPriority();

	@XivApiField("IsFcBuff")
	boolean isFcBuff();
}
