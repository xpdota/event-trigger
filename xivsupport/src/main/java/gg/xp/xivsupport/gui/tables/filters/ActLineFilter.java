package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.events.ACTLogLineEvent;

public class ActLineFilter extends TextBasedFilter<ACTLogLineEvent> {

	public ActLineFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Line", ACTLogLineEvent::getLogLine);
		textBox.setColumns(20);
	}

}
