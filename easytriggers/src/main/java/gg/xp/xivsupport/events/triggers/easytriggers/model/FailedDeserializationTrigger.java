package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.jsontype.TypeSerializer;

import java.util.List;

// Jackson has some behavior that we don't want when we use @JsonValue, so we use our own serializer instead.
@JsonSerialize(using = FailedDeserializationTrigger.FailedSerializer.class)
public final class FailedDeserializationTrigger extends BaseTrigger<Object> {

	private final JsonNode originalJson;
	private final @Nullable Throwable originalError;

	public FailedDeserializationTrigger(JsonNode originalJson, @Nullable Throwable originalError) {
		this.originalJson = originalJson;
		this.originalError = originalError;
	}

	@Override
	public String getName() {
		String base = "FAILED";
		if (originalJson.has("name")) {
			JsonNode nameNode = originalJson.get("name");
			if (nameNode != null && nameNode.isString()) {
				return base + ": " + nameNode.asString();
			}
		}
		return base + " (unknown name)";
	}

	@Override
	public void setName(String name) {
		// Do nothing
	}

	public JsonNode getOriginalJson() {
		return originalJson.deepCopy();
	}

	public @Nullable Throwable getOriginalError() {
		return originalError;
	}

	@Override
	public void recalc() {
		// Do nothing
	}

	@Override
	protected void handleEventInternal(EventContext context, BaseEvent event, EasyTriggerContext ectx) {
		// Do nothing
	}

	@Override
	public void setConditions(List<Condition<? super Object>> conditions) {
		// Do nothing
	}

	@Override
	public void addCondition(Condition<? super Object> condition) {
		// Do nothing
	}

	@Override
	public void removeCondition(Condition<? super Object> condition) {
		// Do nothing
	}

	@Override
	public Class<Object> classForConditions() {
		return Object.class;
	}

	@Override
	public Class<?> getEventType() {
		return Object.class;
	}

	@Override
	public List<Condition<? super Object>> getConditions() {
		return List.of();
	}

	static class FailedSerializer extends ValueSerializer<FailedDeserializationTrigger> {

		@Override
		public void serializeWithType(FailedDeserializationTrigger value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) throws JacksonException {
			serialize(value, gen, ctxt);
		}

		@Override
		public void serialize(FailedDeserializationTrigger value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
			gen.writeTree(value.getOriginalJson());
		}
	}
}
