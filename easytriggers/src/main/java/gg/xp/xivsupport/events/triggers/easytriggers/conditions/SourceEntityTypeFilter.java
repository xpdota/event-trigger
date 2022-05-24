package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;

// TODO: source/target could be combined by simply setting the field in the newInst
// TODO: support 'in party' though it would need injection and wouldn't work with this enum style
public class SourceEntityTypeFilter implements Condition<HasSourceEntity> {

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
}
