package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;

public class AutoMarkRequest extends BaseEvent implements HasTargetEntity {

	@Serial
	private static final long serialVersionUID = -915091489094353125L;
	private final XivPlayerCharacter playerToMark;

	public AutoMarkRequest(XivPlayerCharacter playerToMark) {
		this.playerToMark = playerToMark;
	}

	public XivPlayerCharacter getPlayerToMark() {
		return playerToMark;
	}

	@Override
	public XivCombatant getTarget() {
		return playerToMark;
	}
}
