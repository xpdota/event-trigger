package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;

public class AbilityIdFilter implements Condition<HasAbility> {

	public NumericOperator operator = NumericOperator.EQ;
	public long expected;

	@Override
	public boolean test(HasAbility hasAbility) {
		return operator.checkLong(hasAbility.getAbility().getId(), expected);
	}


	@Override
	public String label() {
		return "Ability ID";
	}

	@Override
	public String describe() {
		return "Ability ID " + operator.getFriendlyName() + ' ' + expected;
	}
}
