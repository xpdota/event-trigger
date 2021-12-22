package gg.xp.xivsupport.replay;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.time.CurrentTimeSource;
import gg.xp.xivsupport.events.actlines.parsers.ActTimeKeeper;
import gg.xp.xivsupport.persistence.Compressible;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.picocontainer.PicoContainer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReplayController {

	private final ExecutorService exs = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder()
			.daemon(true)
			.namingPattern("ReplayThread-%d")
			.priority(Thread.MIN_PRIORITY)
			.build()
	);

	private final EventMaster master;
	private final List<? extends Event> events;
	private final boolean decompress;
	private final CurrentTimeSource timeSource;
	private int currentIndex;

	public ReplayController(PicoContainer container, List<? extends Event> events, boolean decompress) {
		this.master = container.getComponent(EventMaster.class);
		this.timeSource = container.getComponent(ActTimeKeeper.class);
		this.events = events;
		this.decompress = decompress;
	}

	public int getCount() {
		return events.size();
	}

	public int getCurrentPosition() {
		return currentIndex;
	}

	public void advanceBy(int count) {
		for (; count-- > 0 && currentIndex < events.size(); currentIndex++) {
			Event event = events.get(currentIndex);
			if (decompress && event instanceof Compressible compressedEvent) {
				compressedEvent.decompress();
			}
			master.pushEvent(event);
		}
	}

	public void advanceByAsync(int count, Runnable callback) {
		exs.submit(() -> {
			advanceBy(count);
			callback.run();
		});
	}

}
