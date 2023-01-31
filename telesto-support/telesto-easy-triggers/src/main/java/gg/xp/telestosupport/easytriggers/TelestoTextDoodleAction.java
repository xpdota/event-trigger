package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.CircleDoodleSpec;
import gg.xp.telestosupport.doodle.CreateDoodleRequest;
import gg.xp.telestosupport.doodle.DoodleLocation;
import gg.xp.telestosupport.doodle.DynamicText;
import gg.xp.telestosupport.doodle.DynamicTextDoodleSpec;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovySubScriptHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.WideTextField;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;

public class TelestoTextDoodleAction extends BaseTelestoDoodleAction {

	@JsonProperty("location")
	public TelestoLocation location;
	@JsonProperty("size")
	@Description("Font Size")
	public int textSize = 20;
	@JsonProperty("textScript")
	@Description("Text Script")
	@WideTextField
	public String textScript = "";

	@JsonIgnore
	private final XivState state;

	@Description("Single Replacements")
	@JsonProperty("replaceSingle")
	public boolean singleReplacements = true;
	@Description("Global Replacements")
	@JsonProperty("replaceGlobal")
	public boolean globalReplacements;

	public TelestoTextDoodleAction(@JacksonInject XivState state, @JacksonInject GroovyManager groovyManager) {
		this.state = state;
		location = new TelestoLocation();
		location.customExpression = new GroovySubScriptHelper(groovyManager);
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		if (location == null) {
			return;
		}
		DoodleLocation location = this.location.toDoodleLocation(event, context, state);
		if (location != null) {
			DynamicTextDoodleSpec spec = new DynamicTextDoodleSpec(location, textSize, new DynamicText(textScript, singleReplacements, globalReplacements));
			finishSpec(spec, (BaseEvent) event);
			context.accept(new CreateDoodleRequest(spec));
		}
	}

	@Override
	public String dynamicLabel() {
		return "Text Doodle (%s)".formatted(textScript);
	}
}
