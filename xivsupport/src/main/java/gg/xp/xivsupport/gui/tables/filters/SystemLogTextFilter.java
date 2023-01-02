package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.slf4j.LogEvent;

public class SystemLogTextFilter extends TextBasedFilter<LogEvent> {
	public SystemLogTextFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Log Text", LogEvent::getEncoded);
	}
}
