package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.slf4j.LogEvent;

public class SystemLogThreadFilter extends TextBasedFilter<LogEvent> {
	public SystemLogThreadFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Thread", logEvent -> logEvent.getEvent().getThreadName());
	}
}
