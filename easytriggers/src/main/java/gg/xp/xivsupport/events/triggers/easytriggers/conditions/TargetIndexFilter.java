package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasTargetIndex;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;

public class TargetIndexFilter implements Condition<HasTargetIndex> {

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
