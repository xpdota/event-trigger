package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivCombatant;

public class EntityKilledEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {
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
