package gg.xp.postnamazu;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class PnOutgoingMessage extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5779519876994337089L;
	private final String command;
	private final Object payload;

	public PnOutgoingMessage(String command, Object payload) {
		this.command = command;
		this.payload = payload;
	}

	public String getCommand() {
		return command;
	}

	public Object getPayload() {
		return payload;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s: %s", command, payload);
	}

}
