package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.services.Handleable;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;

public class AutoMarkRequest extends BaseEvent implements HasTargetEntity, Handleable {

	@Serial
	private static final long serialVersionUID = -915091489094353125L;
	private final XivPlayerCharacter playerToMark;
	private transient boolean handled;

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

	@Override
	public String toString() {
		return "AutoMarkRequest{" +
		       "playerToMark=" + playerToMark +
		       '}';
	}

	@Override
	public boolean isHandled() {
		return handled;
	}

	@Override
	public void setHandled() {
		handled = true;
	}
}
