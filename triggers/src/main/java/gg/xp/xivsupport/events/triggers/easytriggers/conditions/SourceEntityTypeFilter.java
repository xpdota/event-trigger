package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;

public class SourceEntityTypeFilter implements Condition<HasSourceEntity> {

	public EntityType type = EntityType.ANY;

	@Override
	public String label() {
		return "Source Combatant Type";
	}

	@Override
	public String describe() {
		return "Source Combatant is " + type.getFriendlyName();
	}

	@Override
	public boolean test(HasSourceEntity hasSourceEntity) {
		return type.test(hasSourceEntity.getSource());
	}
}
