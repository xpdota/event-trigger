package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;

public class TargetEntityTypeFilter implements Condition<HasTargetEntity> {

	public EntityType type = EntityType.ANY;

	@Override
	public String label() {
		return "Target Combatant Type";
	}

	@Override
	public String describe() {
		return "Target Combatant is " + type.getFriendlyName();
	}

	@Override
	public boolean test(HasTargetEntity hasTargetEntity) {
		return type.test(hasTargetEntity.getTarget());
	}
}
