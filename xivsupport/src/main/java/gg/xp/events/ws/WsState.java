package gg.xp.events.ws;

import gg.xp.context.SubState;

public class WsState implements SubState {

	private volatile boolean isConnected;

	void setConnected(boolean connected) {
		isConnected = connected;
	}

	public boolean isConnected() {
		return isConnected;
	}
}
