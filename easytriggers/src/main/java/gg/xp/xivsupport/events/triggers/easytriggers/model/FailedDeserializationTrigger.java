package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import tools.jackson.databind.JsonNode;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FailedDeserializationTrigger extends BaseTrigger<Object> {

	private final JsonNode originalJson;
	private final Throwable originalError;

	public FailedDeserializationTrigger(JsonNode originalJson, Throwable originalError) {
		this.originalJson = originalJson;
		this.originalError = originalError;
	}

	@Override
	public String getName() {
		String base = "FAILED";
		if (originalJson.has("name")) {
			JsonNode nameNode = originalJson.get("name");
			if (nameNode != null && nameNode.isString()) {
				return base += ": " + nameNode.asString();
			}
		}
		return base + " (unknown name)";
	}

	@Override
	public void setName(String name) {
		// Do nothing
	}

	@JsonValue
	public JsonNode getOriginalJson() {
		return originalJson;
	}

	@JsonIgnore
	public Throwable getOriginalError() {
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
}
