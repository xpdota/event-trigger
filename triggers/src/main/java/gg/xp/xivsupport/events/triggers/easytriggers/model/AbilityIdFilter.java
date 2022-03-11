package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.xivsupport.events.actlines.events.HasAbility;

public class AbilityIdFilter implements Condition<HasAbility> {

	public NumericOperator operator;
	public long expected;

	@Override
	public boolean test(HasAbility hasAbility) {
		return operator.checkLong(hasAbility.getAbility().getId(), expected);
	}
}
