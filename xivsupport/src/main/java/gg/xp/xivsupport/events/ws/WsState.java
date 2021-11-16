package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.context.SubState;

public class WsState implements SubState {

	private volatile boolean isConnected;

	void setConnected(boolean connected) {
		isConnected = connected;
	}

	public boolean isConnected() {
		return isConnected;
	}
}
