package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

/**
 * Event for when an entity becomes targetable or untargetable. Note that initial spawns do not seem to emit this
 * event, so it only works well for certain mid-combat things.
 */
public class TargetabilityUpdate extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasPrimaryValue {

	private final XivCombatant source;
	private final XivCombatant target;
	private final boolean newState;

	public TargetabilityUpdate(XivCombatant source, XivCombatant target, boolean newState) {
		this.source = source;
		this.target = target;
		this.newState = newState;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	public boolean isTargetable() {
		return newState;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public String getPrimaryValue() {
		return newState ? "Targetable" : "Untargetable";
	}
}
