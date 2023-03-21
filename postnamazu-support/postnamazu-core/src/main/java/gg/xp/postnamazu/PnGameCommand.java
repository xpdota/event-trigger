package gg.xp.postnamazu;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

public class PnGameCommand extends BaseEvent implements HasPrimaryValue {

	private final String command;

	public PnGameCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public String getPrimaryValue() {
		return command;
	}
}
