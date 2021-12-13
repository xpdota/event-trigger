package gg.xp.xivsupport.slf4j;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import gg.xp.xivsupport.events.misc.ViewForAppendOnlyList;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogCollector extends AppenderBase<ILoggingEvent> {

	private static final Logger log = LoggerFactory.getLogger(LogCollector.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor(
			new BasicThreadFactory.Builder()
					.namingPattern("LogCollectorNotifier-%d")
					.daemon(true)
					.build());

	private static LogCollector instance;

	{
		if (instance != null) {
			// Hmmmm...probably can't log from here?
		}
		//noinspection ThisEscapedInObjectConstruction
		instance = this;
	}

	private List<LogEvent> events = new ArrayList<>();
	private final List<Runnable> callbacks = new ArrayList<>();

	private PatternLayoutEncoder encoder;

	@Override
	protected void append(ILoggingEvent iLoggingEvent) {
		LogEvent logEvent = new LogEvent(new String(encoder.encode(iLoggingEvent)), iLoggingEvent);
		if (events.size() > 50_000) {
			events = new ArrayList<>(events.subList(20_000, events.size()));
		}
		events.add(logEvent);
		exs.submit(() -> {
			try {
				callbacks.forEach(Runnable::run);
			}
			catch (Throwable t) {
				log.error("Error notifying log listener", t);
			}
		});
	}

	public PatternLayoutEncoder getEncoder() {
		return encoder;
	}

	public void setEncoder(PatternLayoutEncoder encoder) {
		this.encoder = encoder;
	}

	public List<LogEvent> getEvents() {
		return new ViewForAppendOnlyList<>(events);
	}

	public static LogCollector getInstance() {
		return instance;
	}

	public void addCallback(Runnable run) {
		callbacks.add(run);
	}

	public void removeCallback(Runnable run) {
		callbacks.remove(run);
	}
}
