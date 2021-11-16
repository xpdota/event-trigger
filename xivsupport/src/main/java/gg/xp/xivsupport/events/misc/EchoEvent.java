package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.BaseEvent;

public class EchoEvent extends BaseEvent {

	private static final long serialVersionUID = -6661787415932481263L;
	private final String line;

	public EchoEvent(String line) {
		this.line = line;
	}

	public String getLine() {
		return line;
	}
}
