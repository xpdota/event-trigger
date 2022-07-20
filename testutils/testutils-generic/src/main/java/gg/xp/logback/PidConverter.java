package gg.xp.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PidConverter extends ClassicConverter {
	private final long pid = ProcessHandle.current().pid();
	private final String pidAsStr = Long.toString(pid);

	@Override
	public String convert(ILoggingEvent iLoggingEvent) {
		return pidAsStr;
	}
}
