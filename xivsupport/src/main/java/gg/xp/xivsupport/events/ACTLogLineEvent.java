package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

@SystemEvent
public class ACTLogLineEvent extends BaseEvent {

	private static final long serialVersionUID = -5255204546093791693L;
	private final String logLine;

	public ACTLogLineEvent(String logLine) {
		this.logLine = logLine;
	}

	public String getLogLine() {
		return logLine;
	}
}
