package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;


public class StatusIdFilter implements SimpleCondition<HasStatusEffect> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Status ID")
	@IdType(value = StatusEffectInfo.class, matchRequired = false)
	public long expected;

	@Override
	public boolean test(HasStatusEffect hasStatus) {
		return operator.checkLong(hasStatus.getBuff().getId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Status Effect ID";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Status Effect ID %s 0x%x (%s)", operator.getFriendlyName(), expected, expected);
	}

	@Override
	public Class<HasStatusEffect> getEventType() {
		return HasStatusEffect.class;
	}
}
