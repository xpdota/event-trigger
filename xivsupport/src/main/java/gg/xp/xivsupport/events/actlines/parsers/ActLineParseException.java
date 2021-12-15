package gg.xp.xivsupport.events.actlines.parsers;

public class ActLineParseException extends RuntimeException {
	private static final long serialVersionUID = 4846226454086193811L;

	public ActLineParseException(String logLine, Throwable cause) {
		super(String.format("Error parsing line: %s", logLine), cause);
	}
}
