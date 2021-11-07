package gg.xp.events;

public class ACTLogLineEvent extends BaseEvent {

	private final String logLine;

	public ACTLogLineEvent(String logLine) {
		this.logLine = logLine;
	}

	public String getLogLine() {
		return logLine;
	}
}
