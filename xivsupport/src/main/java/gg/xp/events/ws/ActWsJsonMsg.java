package gg.xp.events.ws;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

public class ActWsJsonMsg extends BaseEvent {

	private final @Nullable String type;
	private final JsonNode json;

	public ActWsJsonMsg(@Nullable String type, JsonNode json) {
		this.type = type;
		this.json = json;
	}

	public @Nullable String getType() {
		return type;
	}

	public JsonNode getJson() {
		return json;
	}
}
