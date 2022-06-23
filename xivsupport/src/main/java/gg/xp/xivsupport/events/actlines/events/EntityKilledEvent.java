package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

/**
 * Represents a combatant being killed
 */
public class EntityKilledEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {
	@Serial
	private static final long serialVersionUID = 2379855663603121468L;
	private final XivCombatant source;
	private final XivCombatant target;

	public EntityKilledEvent(XivCombatant source, XivCombatant target) {
		this.source = source;
		this.target = target;
	}

	/**
	 * @return The killer
	 */
	@Override
	public XivCombatant getSource() {
		return source;
	}

	/**
	 * @return The killed entity
	 */
	@Override
	public XivCombatant getTarget() {
		return target;
	}
}
