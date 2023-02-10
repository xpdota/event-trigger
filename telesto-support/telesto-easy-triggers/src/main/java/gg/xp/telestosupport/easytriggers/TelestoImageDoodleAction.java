package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.CreateDoodleRequest;
import gg.xp.telestosupport.doodle.DoodleLocation;
import gg.xp.telestosupport.doodle.ImageIconDoodleSpec;
import gg.xp.telestosupport.doodle.img.HAlign;
import gg.xp.telestosupport.doodle.img.VAlign;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovySubScriptHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;

import java.awt.*;

public class TelestoImageDoodleAction extends BaseTelestoDoodleAction {

	@JsonProperty("location")
	public TelestoLocation location;

	@JsonProperty("iconSpec")
	public IconSpec iconSpec;

	@JsonProperty
	@Description("H Alignment")
	public HAlign hAlign = HAlign.middle;
	@JsonProperty
	@Description("V Alignment")
	public VAlign vAlign = VAlign.middle;

	@JsonIgnore
	private final XivState state;

	@SuppressWarnings("AssignmentToSuperclassField")
	public TelestoImageDoodleAction(@JacksonInject XivState state, @JacksonInject GroovyManager groovyManager) {
		this.state = state;
		location = new TelestoLocation();
		location.customExpression = new GroovySubScriptHelper(groovyManager);
		iconSpec = new IconSpec();
		color = Color.WHITE;
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		if (location == null) {
			return;
		}
		DoodleLocation doodleLocation = location.toDoodleLocation(event, context, state);
		Long iconId = iconSpec.toIconId(event);
		if (doodleLocation != null && iconId != null) {
			ImageIconDoodleSpec spec = new ImageIconDoodleSpec(doodleLocation, iconId, hAlign, vAlign);
			finishSpec(spec, (BaseEvent) event);
			context.accept(new CreateDoodleRequest(spec));
		}

	}

	@Override
	public String dynamicLabel() {
		return "Telesto Image Icon ID";
	}
}
