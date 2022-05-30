package gg.xp.telestosupport;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class TelestoGameCommand extends BaseEvent {
	@Serial
	private static final long serialVersionUID = 1079632856179031899L;
	private final String command;

	public TelestoGameCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}
}
