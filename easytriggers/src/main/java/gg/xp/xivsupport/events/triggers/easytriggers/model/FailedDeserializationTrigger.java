package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;

import java.util.List;

public final class FailedDeserializationTrigger extends BaseTrigger<Object> {

	private final JsonNode originalJson;
	private final Throwable originalError;

	public FailedDeserializationTrigger(JsonNode originalJson, Throwable originalError) {
		this.originalJson = originalJson;
		this.originalError = originalError;
	}


	@JsonValue
	@JsonRawValue
	public JsonNode getOriginalJson() {
		return originalJson;
	}

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
