package gg.xp.events;

public class ACTLogLineEvent implements Event {

	private final String logLine;

	public ACTLogLineEvent(String logLine) {
		this.logLine = logLine;
	}

	public String getLogLine() {
		return logLine;
	}
}
