package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.debug.DebugEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;

public class DebugAction implements Action<Event> {

	public GroovySubScriptHelper script;

	public DebugAction(@JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr) {
		script = new GroovySubScriptHelper("\"Text ${event.class}\"", BaseEvent.class, mgr);
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		Object result = script.run(context, event);
		context.accept(new DebugEvent(result));
	}

	@Override
	public String fixedLabel() {
		return "Debug Action";
	}

	@Override
	public String dynamicLabel() {
		return "Debug Action: {}";
	}
}
