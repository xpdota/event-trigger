package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.EventContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EasyTriggerContext {

	private final EventContext context;
	private Map<String, Object> extraVariables;
	private boolean stopProcessing;

	public EasyTriggerContext(EventContext context) {
		this.context = context;
	}

	public void addVariable(String key, Object value) {
		if (extraVariables == null) {
			extraVariables = new HashMap<>();
		}
		extraVariables.put(key, value);
	}

	public Map<String, Object> getExtraVariables() {
		if (extraVariables == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(extraVariables);
	}

	public EventContext getEventContext() {
		return context;
	}

	public void setStopProcessing(boolean stopProcessing) {
		this.stopProcessing = stopProcessing;
	}

	public boolean shouldStopProcessing() {
		return stopProcessing;
	}
}
