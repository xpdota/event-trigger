package gg.xp.xivsupport.gui.imprt;

import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ListEventIterator<X extends Event> implements EventIterator<X> {

	private final List<X> list;
	private volatile int index;

	public ListEventIterator(List<X> events) {
		this.list = new ArrayList<>(events);
	}

	@Override
	public boolean hasMore() {
		return index < list.size();
	}

	@Override
	public @Nullable X getNext() {
		if (!hasMore()) {
			return null;
		}
		X value = list.get(index);
		list.set(index, null);
		index++;
		return value;
	}
}
