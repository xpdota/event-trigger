package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;

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
