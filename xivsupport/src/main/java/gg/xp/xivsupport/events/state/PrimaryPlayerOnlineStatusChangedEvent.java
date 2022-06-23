package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;

import java.io.Serial;

public class PrimaryPlayerOnlineStatusChangedEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 2792746140131840006L;
	private final OnlineStatus oldStatus;
	private final OnlineStatus playerOnlineStatus;

	public PrimaryPlayerOnlineStatusChangedEvent(OnlineStatus oldStatus, OnlineStatus playerOnlineStatus) {
		this.oldStatus = oldStatus;
		this.playerOnlineStatus = playerOnlineStatus;
	}

	public OnlineStatus getOldStatus() {
		return oldStatus;
	}

	public OnlineStatus getPlayerOnlineStatus() {
		return playerOnlineStatus;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s -> %s", oldStatus, playerOnlineStatus);
	}
}
