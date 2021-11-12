package gg.xp.events.slf4j;

import ch.qos.logback.classic.spi.ILoggingEvent;

public final class LogEvent {
	private final String encoded;
	private final ILoggingEvent event;

	LogEvent(String encoded, ILoggingEvent event) {
		this.encoded = encoded;
		this.event = event;
	}

	public String getEncoded() {
		return encoded;
	}

	public ILoggingEvent getEvent() {
		return event;
	}
}
