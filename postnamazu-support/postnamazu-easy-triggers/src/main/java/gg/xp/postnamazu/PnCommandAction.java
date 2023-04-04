package gg.xp.postnamazu;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.WideTextField;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptProcessor;

public class PnCommandAction implements Action<Event> {

	@JsonIgnore
	private final GroovyScriptProcessor gsp;
	private final GroovyManager mgr;
	@JsonProperty("command")
	private String command;

	@JsonProperty("textScript")
	@Description("Text Script")
	@WideTextField
	public String textScript = "";

	@Description("Single Replacements")
	public boolean calloutReplacements;

	@Description("Global Replacements")
	public boolean globalReplacements;

	public PnCommandAction(@JacksonInject GroovyScriptProcessor gsp, @JacksonInject GroovyManager mgr) {
		this.gsp = gsp;
		this.mgr = mgr;
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		String command = gsp.replaceInString(textScript, context.makeBinding(mgr.makeBinding()), calloutReplacements, globalReplacements);
		context.accept(new PnGameCommand(command));
	}

	@Override
	public String fixedLabel() {
		return null;
	}

	@Override
	public String dynamicLabel() {
		return "Run Command '%s'".formatted(command);
	}
}
