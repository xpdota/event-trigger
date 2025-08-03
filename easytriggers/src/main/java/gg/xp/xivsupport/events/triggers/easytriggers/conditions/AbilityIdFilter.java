package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class AbilityIdFilter implements SimpleCondition<HasAbility> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Ability ID")
	@IdType(value = ActionInfo.class, matchRequired = false)
	public long expected;

	@Override
	public boolean test(HasAbility hasAbility) {
		return operator.checkLong(hasAbility.getAbility().getId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Ability ID";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Ability ID %s 0x%x (%s)", operator.getFriendlyName(), expected, expected);
	}

	@Override
	public Class<HasAbility> getEventType() {
		return HasAbility.class;
	}
}
