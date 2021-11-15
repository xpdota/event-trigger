package gg.xp.gui.tables.filters;

public class EventClassFilterFilter extends TextBasedFilter<Object> {

	public EventClassFilterFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Event Class", item -> item.getClass().getSimpleName());
	}

}
