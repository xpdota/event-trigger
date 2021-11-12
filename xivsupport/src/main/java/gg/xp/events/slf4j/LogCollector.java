package gg.xp.events.slf4j;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class LogCollector extends AppenderBase<ILoggingEvent> {

	public static LogCollector instance;

	{
		if (instance != null) {
			// Hmmmm...probably can't log from here?
		}
		//noinspection ThisEscapedInObjectConstruction
		instance = this;
	}

	// TODO: pruning
	private final List<LogEvent> events = new ArrayList<>();

	private PatternLayoutEncoder encoder;

	@Override
	protected void append(ILoggingEvent iLoggingEvent) {
		LogEvent logEvent = new LogEvent(new String(encoder.encode(iLoggingEvent)), iLoggingEvent);
		events.add(logEvent);
	}

	public PatternLayoutEncoder getEncoder() {
		return encoder;
	}

	public void setEncoder(PatternLayoutEncoder encoder) {
		this.encoder = encoder;
	}

	public List<LogEvent> getEvents() {
		return new ArrayList<>(events);
	}

	public static LogCollector getInstance() {
		return instance;
	}
}
