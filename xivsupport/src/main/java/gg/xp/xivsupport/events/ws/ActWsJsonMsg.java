package gg.xp.xivsupport.events.ws;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.lang.ref.WeakReference;
import java.time.Instant;

@SystemEvent
public class ActWsJsonMsg extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -5830123394422861873L;
	private final @Nullable String type;
	private final Object rseq;
	private final WeakReference<JsonNode> jsonWeakRef;
	private JsonNode json;

	public ActWsJsonMsg(@Nullable String type, @Nullable Object rseq, JsonNode json) {
		this.type = type;
		this.rseq = rseq;
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
	public void setPumpFinishedAt(Instant pumpedAt) {
		// TODO: should really be a different method
		json = null;
		super.setPumpFinishedAt(pumpedAt);
	}

	public @Nullable Object getRseq() {
		return rseq;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("type: %s, rseq: %s", type, rseq);
	}
}
