package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.slf4j.LogEvent;

public class SystemLogLoggerNameFilter extends TextBasedFilter<LogEvent> {
	public SystemLogLoggerNameFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Logger/Class", logEvent -> logEvent.getEvent().getLoggerName());
	}
}
