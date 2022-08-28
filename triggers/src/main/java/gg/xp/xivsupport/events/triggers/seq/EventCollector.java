package gg.xp.xivsupport.events.triggers.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class EventCollector<X> implements Predicate<X> {

	private final Predicate<X> filter;

	public EventCollector(Predicate<X> filter) {
		this.filter = filter;
	}

	private final List<X> events = new ArrayList<>();

	void provideEvent(X event) {
		events.add(event);
	}

	void provideEvents(Collection<? extends X> events) {
		this.events.addAll(events);
	}

	public List<X> getEvents() {
		return Collections.unmodifiableList(events);
	}

	public void clear() {
		events.clear();
	}

	@Override
	public boolean test(X event) {
		return filter.test(event);
	}

	public Optional<X> findAny(Predicate<X> subFilter) {
		return events.stream().filter(subFilter).findAny();
	}
}
