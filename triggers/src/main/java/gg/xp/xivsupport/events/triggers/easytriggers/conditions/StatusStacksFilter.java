package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;


public class StatusStacksFilter implements Condition<HasStatusEffect> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Stacks")
	public long expected;

	@Override
	public boolean test(HasStatusEffect hasStatus) {
		return operator.checkLong(hasStatus.getStacks(), expected);
	}


	@Override
	public String fixedLabel() {
		return "# of Stacks";
	}

	@Override
	public String dynamicLabel() {
		return "# of Stacks " + operator.getFriendlyName() + ' ' + expected;
	}
}
