package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;

import java.io.Serial;

public class PrimaryPlayerOnlineStatusChangedEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 2792746140131840006L;
	private final OnlineStatus playerOnlineStatus;

	public PrimaryPlayerOnlineStatusChangedEvent(OnlineStatus oldStatus, OnlineStatus playerOnlineStatus) {
		this.playerOnlineStatus = playerOnlineStatus;
	}

	public OnlineStatus getPlayerOnlineStatus() {
		return playerOnlineStatus;
	}
}
