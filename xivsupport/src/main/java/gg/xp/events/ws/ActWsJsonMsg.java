package gg.xp.events.ws;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.events.BaseEvent;
import gg.xp.events.actlines.events.SystemEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.time.Instant;

@SystemEvent
public class ActWsJsonMsg extends BaseEvent {

	private static final long serialVersionUID = -5830123394422861873L;
	private final @Nullable String type;
	private WeakReference<JsonNode> jsonWeakRef;
	private JsonNode json;

	public ActWsJsonMsg(@Nullable String type, JsonNode json) {
		this.type = type;
		jsonWeakRef = new WeakReference<>(json);
		this.json = json;
	}

	public @Nullable String getType() {
		return type;
	}

	public JsonNode getJson() {
		JsonNode jsonHardRef = this.json;
		return jsonHardRef == null ? jsonWeakRef.get() : jsonHardRef;
	}

	@Override
	public void setPumpedAt(Instant pumpedAt) {
		// TODO: should really be a different method
		json = null;
		super.setPumpedAt(pumpedAt);
	}
}
