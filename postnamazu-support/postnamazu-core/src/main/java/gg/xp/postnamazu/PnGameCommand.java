package gg.xp.postnamazu;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class PnGameCommand extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5493940106527804609L;
	private final String command;
	private final boolean usesAmQueue;

	/**
	 * @param command The command to run.
	 */
	public PnGameCommand(String command) {
		this(command, false);
	}

	/**
	 * @param command     The command to run
	 * @param isAmCommand true to use the AM queue instead of the command queue;
	 *                    should be true for AM-related commands such as clears.
	 */
	public PnGameCommand(String command, boolean isAmCommand) {
		this.command = command;
		this.usesAmQueue = isAmCommand;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public String getPrimaryValue() {
		return command;
	}

	public boolean getUsesAmQueue() {
		return usesAmQueue;
	}
}
