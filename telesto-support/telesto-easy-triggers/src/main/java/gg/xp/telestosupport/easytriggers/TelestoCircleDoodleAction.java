package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.CircleDoodleSpec;
import gg.xp.telestosupport.doodle.CoordSystem;
import gg.xp.telestosupport.doodle.CreateDoodleRequest;
import gg.xp.telestosupport.doodle.DoodleLocation;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovySubScriptHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelestoCircleDoodleAction extends BaseTelestoDoodleAction {

	private static final Logger log = LoggerFactory.getLogger(TelestoCircleDoodleAction.class);

	@JsonProperty("location")
	public TelestoLocation location;
	@JsonProperty("radius")
	@Description("Radius")
	public double radius = 10.0;
	@JsonProperty("filled")
	@Description("Filled")
	public boolean filled;
	@JsonProperty("system")
	@Description("System")
	public CoordSystem system = CoordSystem.Screen;

	@JsonIgnore
	private final XivState state;

	public TelestoCircleDoodleAction(@JacksonInject XivState state, @JacksonInject GroovyManager groovyManager) {
		this.state = state;
		location = new TelestoLocation();
		location.customExpression = new GroovySubScriptHelper(groovyManager);
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		if (location == null) {
			return;
		}
		DoodleLocation circleLocation = location.toDoodleLocation(event, context, state);
		if (circleLocation != null) {
			CircleDoodleSpec spec = new CircleDoodleSpec(circleLocation, radius, filled, system);
			finishSpec(spec, (BaseEvent) event);
			context.accept(new CreateDoodleRequest(spec));
		}
	}

	@Override
	public String dynamicLabel() {
		return "Telesto Circle Doodle";
	}
}
