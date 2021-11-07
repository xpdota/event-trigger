package gg.xp.events.misc;

import gg.xp.events.BaseEvent;

public class EchoEvent extends BaseEvent {

	private final String line;

	public EchoEvent(String line) {
		this.line = line;
	}

	public String getLine() {
		return line;
	}
}
