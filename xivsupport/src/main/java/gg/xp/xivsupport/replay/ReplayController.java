package gg.xp.xivsupport.replay;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

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
	private int currentIndex;

	public ReplayController(EventMaster master, List<? extends Event> events) {
		this.master = master;
		this.events = events;
	}

	public int getCount() {
		return events.size();
	}

	public int getCurrentPosition() {
		return currentIndex;
	}

	public void advanceBy(int count) {
		for (; count-- > 0 && currentIndex < events.size(); currentIndex ++) {
			master.pushEvent(events.get(currentIndex));
		}
	}

	public void advanceByAsync(int count, Runnable callback) {
		exs.submit(() -> {
			advanceBy(count);
			callback.run();
		});
	}

}
