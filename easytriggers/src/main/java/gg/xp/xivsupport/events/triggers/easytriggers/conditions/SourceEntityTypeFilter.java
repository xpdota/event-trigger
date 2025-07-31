package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

// TODO: source/target could be combined by simply setting the field in the newInst
public class SourceEntityTypeFilter implements SimpleCondition<HasSourceEntity> {

	public EntityType type = EntityType.ANY;

	@Override
	public String fixedLabel() {
		return "Source Combatant Type";
	}

	@Override
	public String dynamicLabel() {
		return "Source Combatant is " + type.getFriendlyName();
	}

	@Override
	public boolean test(HasSourceEntity hasSourceEntity) {
		return type.test(hasSourceEntity.getSource());
	}

	@Override
	public Class<HasSourceEntity> getEventType() {
		return HasSourceEntity.class;
	}
}
