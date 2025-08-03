package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;


public class StatusRawStacksFilter implements SimpleCondition<HasStatusEffect> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Raw Stacks")
	public long expected;

	@Override
	public boolean test(HasStatusEffect hasStatus) {
		return operator.checkLong(hasStatus.getRawStacks(), expected);
	}


	@Override
	public String fixedLabel() {
		return "# of Stacks (raw)";
	}

	@Override
	public String dynamicLabel() {
		return "# of Raw Stacks " + operator.getFriendlyName() + ' ' + expected;
	}

	@Override
	public Class<HasStatusEffect> getEventType() {
		return HasStatusEffect.class;
	}
}
