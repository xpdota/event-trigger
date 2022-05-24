package gg.xp.xivsupport.events;

import gg.xp.reevent.context.NoOpStateStore;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestEventContext implements EventContext {

	private final List<Event> accepted = new ArrayList<>();
	private final List<Event> enqueued = new ArrayList<>();

	@Override
	public void accept(Event event) {
		accepted.add(event);
	}

	@Override
	public void enqueue(Event event) {
		enqueued.add(event);
	}

	@Override
	public StateStore getStateInfo() {
		return NoOpStateStore.INSTANCE;
	}

	public List<Event> getAccepted() {
		return Collections.unmodifiableList(accepted);
	}

	public List<Event> getEnqueued() {
		return Collections.unmodifiableList(enqueued);
	}
}
