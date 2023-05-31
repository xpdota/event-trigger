package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.DoodleLocation;
import gg.xp.telestosupport.doodle.EntityDoodleLocation;
import gg.xp.telestosupport.doodle.WorldPositionDoodleLocation;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovySubScriptHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

public class TelestoLocation {

	@Description("Location")
	public TelestoLocationType type = TelestoLocationType.PLAYER;
	@Description("Snapshot Position")
	public boolean usePosition;
	@Description("Custom Expression")
	public GroovySubScriptHelper customExpression;
	@Description("Offsets")
	public TelestoOffsets offsets = new TelestoOffsets();

	@JsonIgnore
	public @Nullable DoodleLocation toDoodleLocation(Event event, EasyTriggerContext context, XivState state) {
		DoodleLocation dl = makeDoodleLocation(event, context, state);
		if (dl == null) {
			return null;
		}
		dl.xOffset = offsets.xOffset;
		dl.yOffset = offsets.yOffset;
		dl.zOffset = offsets.zOffset;
		return dl;
	}

	@Nullable
	private DoodleLocation makeDoodleLocation(Event event, EasyTriggerContext context, XivState state) {
		switch (type) {
			case SOURCE -> {
				if (event instanceof HasSourceEntity hse) {
					return usePosition ? new WorldPositionDoodleLocation(hse.getSource().getPos()) : new EntityDoodleLocation(hse.getSource());
				}
			}
			case TARGET -> {
				if (event instanceof HasTargetEntity hse) {
					return usePosition ? new WorldPositionDoodleLocation(hse.getTarget().getPos()) : new EntityDoodleLocation(hse.getTarget());
				}
			}
			case PLAYER -> {
				return usePosition ? new WorldPositionDoodleLocation(state.getPlayer().getPos()) : new EntityDoodleLocation(state.getPlayer());
			}
			case CUSTOM -> {
				Object result = customExpression.run(context, event);
				if (result instanceof XivCombatant cbt) {
					return usePosition ? new WorldPositionDoodleLocation(cbt.getPos()) : new EntityDoodleLocation(cbt);
				}
				else if (result instanceof Position pos) {
					return new WorldPositionDoodleLocation(pos);
				}

			}
		}
		return null;
	}

	@JsonIgnore
	public static TelestoLocation create(GroovyManager mgr) {
		TelestoLocation loc = new TelestoLocation();
		loc.customExpression = new GroovySubScriptHelper(mgr);
		return loc;

	}

}
