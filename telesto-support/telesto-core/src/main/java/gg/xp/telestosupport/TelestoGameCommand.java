package gg.xp.telestosupport;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class TelestoGameCommand extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = 1079632856179031899L;
	private final String command;

	public TelestoGameCommand(String command) {
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
