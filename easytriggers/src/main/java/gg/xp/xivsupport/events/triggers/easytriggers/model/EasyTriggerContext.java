package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EasyTriggerContext {

	private final EventContext context;
	private Map<String, Object> extraVariables;
	private boolean stopProcessing;
	private Consumer<Event> acceptHook;
	private Consumer<Event> enqueueHook;

	public EasyTriggerContext(EventContext context) {
		this.context = context;
		if (context == null) {
			acceptHook = i -> {};
			enqueueHook = i -> {};
		}
		else {
			acceptHook = context::accept;
			enqueueHook = context::accept;
		}
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

//	public EventContext getEventContext() {
//		return context;
//	}
//
	public void accept(Event event) {
		acceptHook.accept(event);
	};

	public void enqueue(Event event) {
		enqueueHook.accept(event);
	}

	public void setStopProcessing(boolean stopProcessing) {
		this.stopProcessing = stopProcessing;
	}

	public boolean shouldStopProcessing() {
		return stopProcessing;
	}

	public void setAcceptHook(Consumer<Event> accept) {
		this.acceptHook = accept;
	}

	public void setEnqueueHook(Consumer<Event> enqueue) {
		this.enqueueHook = enqueue;
	}
}
