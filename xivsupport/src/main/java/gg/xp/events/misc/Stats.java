package gg.xp.events.misc;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.debug.DebugCommand;
import gg.xp.events.state.QueueState;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SuppressWarnings("ALL")
public class Stats { // :eldercat:

	private static final Logger log = LoggerFactory.getLogger(Stats.class);

	private volatile long start = System.currentTimeMillis();
	private volatile long total;
	private volatile long primogenitor;
	private volatile long synthetic;

	@HandleEvents
	public void countEvents(EventContext<Event> context, Event event) {
		total++;
		if (event.getParent() == null) {
			primogenitor++;
		}
		else {
			synthetic++;
		}
	}

	@HandleEvents
	public void statsCommand(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("stats")) {
			if (event.args().size() == 1) {
				long now = System.currentTimeMillis();
				double delta = ((double) (now - start)) / 1000;
				log.info("Processing for {} seconds", delta);
				log.info("Event stats: Total: {}, Primogenitor: {}, Synthetic: {}", total, primogenitor, synthetic);
				log.info("Event stats (per second): Total: {}, Primogenitor: {}, Synthetic: {}", total / delta, primogenitor / delta, synthetic / delta);
				log.info("Queue depth: {}", context.getStateInfo().get(QueueState.class).getQueueDepth());
				MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
				log.info("Heap: {}", memory.getHeapMemoryUsage().toString());
				log.info("Non-heap: {}", memory.getNonHeapMemoryUsage().toString());
			}
			else if ("clear".equals(event.args().get(1))) {
				log.info("Clearing stats");
				start = System.currentTimeMillis();
				total = 0;
				primogenitor = 0;
				synthetic = 0;

			}

		}
	}

	// These aren't thread safe but it probably doesn't matter
	public Duration getDuration() {
		long now = System.currentTimeMillis();
		long delta = now - start;
		return Duration.of(delta, ChronoUnit.MILLIS);
	}

	public long getTotal() {
		return total;
	}

	public long getPrimogenitor() {
		return primogenitor;
	}

	public long getSynthetic() {
		return synthetic;
	}
}
