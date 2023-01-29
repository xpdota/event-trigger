package gg.xp.telestosupport;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class BaseTelestoResponse extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -5724122000114932007L;
	private transient TelestoOutgoingMessage responseTo;

	public void setResponseTo(TelestoOutgoingMessage responseTo) {
		this.responseTo = responseTo;
	}

	public TelestoOutgoingMessage getResponseTo() {
		return responseTo;
	}

	@Override
	public boolean shouldSave() {
		return true;
	}
}
