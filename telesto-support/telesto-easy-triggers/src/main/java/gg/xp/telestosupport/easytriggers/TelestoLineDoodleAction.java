package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.CreateDoodleRequest;
import gg.xp.telestosupport.doodle.DoodleLocation;
import gg.xp.telestosupport.doodle.LineDoodleSpec;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
import gg.xp.xivsupport.groovy.GroovyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelestoLineDoodleAction extends BaseTelestoDoodleAction {

	private static final Logger log = LoggerFactory.getLogger(TelestoLineDoodleAction.class);

	@JsonProperty("start")
	public TelestoLocation start;
	@JsonProperty("end")
	public TelestoLocation end;
	@JsonProperty("thickness")
	@Description("Line Width")
	public double thickness = 10.0;

	@JsonIgnore
	private final XivState state;

	public TelestoLineDoodleAction(@JacksonInject XivState state, @JacksonInject GroovyManager groovyManager) {
		this.state = state;
		start = TelestoLocation.create(groovyManager);
		end = TelestoLocation.create(groovyManager);
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		if (start == null || end == null) {
			return;
		}
		DoodleLocation startLocation = start.toDoodleLocation(event, context, state);
		DoodleLocation endLocation = end.toDoodleLocation(event, context, state);
		if (start != null && end != null) {
			LineDoodleSpec spec = new LineDoodleSpec(startLocation, endLocation, thickness);
			finishSpec(spec);
			context.accept(new CreateDoodleRequest(spec));
		}
	}

	@Override
	public String dynamicLabel() {
		return "Telesto Line Doodle";
	}
}
