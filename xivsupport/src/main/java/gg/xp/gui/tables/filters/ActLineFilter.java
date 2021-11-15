package gg.xp.gui.tables.filters;

import gg.xp.events.ACTLogLineEvent;

public class ActLineFilter extends TextBasedFilter<ACTLogLineEvent> {

	public ActLineFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Line", ACTLogLineEvent::getLogLine);
		textBox.setColumns(20);
	}

}
