package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import groovy.lang.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EasyTriggerContext {

	private static final Logger log = LoggerFactory.getLogger(EasyTriggerContext.class);
	public static final String SOURCE_EASY_TRIGGER_KEY = "sourceEasyTrigger";

	private final EventContext context;
	// for logging
	private String triggerName = "";
	private Map<String, Object> extraVariables;
	private boolean stopProcessing;
	private Consumer<Event> acceptHook;
	private Consumer<Event> enqueueHook;

	private EasyTriggerContext(EventContext context, String name) {
		this.context = context;
		this.triggerName = name;
		if (context == null) {
			acceptHook = i -> {
			};
			enqueueHook = i -> {
			};
		}
		else {
			acceptHook = context::accept;
			enqueueHook = context::accept;
		}
	}

	public EasyTriggerContext(EventContext context) {
		this(context, (String) null);
	}

	public EasyTriggerContext(EventContext context, BaseTrigger<?> trigger) {
		this(context, trigger.getName());
		addVariable(SOURCE_EASY_TRIGGER_KEY, trigger);
	}

	public <X extends BaseEvent> void runActions(List<Action<? super X>> actions, SequentialTriggerController<X> s, X e1) {
		for (Action<? super X> action : actions) {

			log.info("Action: {}", action);
			setAcceptHook(s::accept);
			setEnqueueHook(s::enqueue);
			addVariable("event", e1);
			addVariable("s", s);
			if (context != null) {
				addVariable("context", context);
			}

			try {
				if (action instanceof SqAction sqa) {
					sqa.accept(s, this, e1);
				}
				else {
					action.accept(this, e1);
				}
			}
			catch (Throwable t) {
				log.error("Error in trigger '{}' action '{}'", triggerName, action.dynamicLabel(), t);
				if (s.isDone()) {
					return;
				}
			}

			if (shouldStopProcessing()) {
				log.info("Easy Trigger wants to stop processing (trigger {}, action {})", triggerName, action.dynamicLabel());
				return;
			}
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
	}

	;

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

	public Binding makeBinding(Binding originalBinding) {
		return new Binding(originalBinding.getVariables()) {
			@Override
			public Object getVariable(String name) {
				Object extra = getExtraVariables().get(name);
				if (extra != null) {
					return extra;
				}
				return super.getVariable(name);
			}

			@Override
			public void setVariable(String name, Object value) {
				addVariable(name, value);
			}

			@Override
			public boolean hasVariable(String name) {
				return getExtraVariables().containsKey(name) || super.hasVariable(name);
			}
		};
	}
}
