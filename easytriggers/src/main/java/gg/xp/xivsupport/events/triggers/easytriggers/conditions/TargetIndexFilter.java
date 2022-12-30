package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasTargetIndex;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TargetIndexFilter implements SimpleCondition<HasTargetIndex> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Target Index (from 0)")
	public long expected;

	@Override
	public boolean test(HasTargetIndex event) {
		return operator.checkLong(event.getTargetIndex(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Target Index";
	}

	@Override
	public String dynamicLabel() {
		return "Target Index " + operator.getFriendlyName() + ' ' + expected;
	}
}
