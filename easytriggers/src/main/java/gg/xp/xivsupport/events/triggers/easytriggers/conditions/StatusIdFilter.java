package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.models.XivStatusEffect;


public class StatusIdFilter implements Condition<HasStatusEffect> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Status ID")
	@IdType(StatusEffectInfo.class)
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
		return "Status Effect ID " + operator.getFriendlyName() + ' ' + expected;
	}
}
