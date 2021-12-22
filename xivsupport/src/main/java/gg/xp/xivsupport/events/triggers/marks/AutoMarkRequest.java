package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;

public class AutoMarkRequest extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -915091489094353125L;
	private final XivPlayerCharacter playerToMark;
	@SuppressWarnings({"unused", "FieldCanBeLocal"}) // for debugging
	private int resolvedPartySlot;

	public AutoMarkRequest(XivPlayerCharacter playerToMark) {
		this.playerToMark = playerToMark;
	}

	public XivPlayerCharacter getPlayerToMark() {
		return playerToMark;
	}

	public void setResolvedPartySlot(int resolvedPartySlot) {
		this.resolvedPartySlot = resolvedPartySlot;
	}
}
