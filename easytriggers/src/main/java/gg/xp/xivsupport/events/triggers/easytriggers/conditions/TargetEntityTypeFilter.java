package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TargetEntityTypeFilter implements SimpleCondition<HasTargetEntity> {

	public EntityType type = EntityType.ANY;

	@Override
	public String fixedLabel() {
		return "Target Combatant Type";
	}

	@Override
	public String dynamicLabel() {
		return "Target Combatant is " + type.getFriendlyName();
	}

	@Override
	public boolean test(HasTargetEntity hasTargetEntity) {
		return type.test(hasTargetEntity.getTarget());
	}
}
