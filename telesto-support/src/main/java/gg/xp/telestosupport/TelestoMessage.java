package gg.xp.telestosupport;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class TelestoMessage extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -4682203569109709940L;
	private final JsonNode json;

	public TelestoMessage(JsonNode json) {
		this.json = json;
	}

	public JsonNode getJson() {
		return json;
	}
}
