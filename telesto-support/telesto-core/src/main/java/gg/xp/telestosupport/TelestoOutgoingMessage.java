package gg.xp.telestosupport;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.io.Serial;

@SystemEvent
public class TelestoOutgoingMessage extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -4682203569109709940L;
	private final JsonNode json;
	private final boolean delay;

	public TelestoOutgoingMessage(JsonNode json, boolean delay) {
		this.json = json;
		this.delay = delay;
	}

	public JsonNode getJson() {
		return json;
	}

	public boolean shouldDelay() {
		return delay;
	}
}
