package gg.xp.events.actlines;

import gg.xp.events.BaseEvent;
import gg.xp.events.Event;

public class ZeroLogLineEvent extends BaseEvent {
	private final long code;
	private final String name;
	private final String line;

	public ZeroLogLineEvent(long code, String name, String line) {
		this.code = code;
		this.name = name;
		this.line = line;
	}

	public long getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getLine() {
		return line;
	}
}
