package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;


public class DurationFilter implements SimpleCondition<HasDuration> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Milliseconds")
	public long expectedMs;

	@Override
	public boolean test(HasDuration hasStatus) {
		return operator.checkLong(hasStatus.getInitialDuration().toMillis(), expectedMs);
	}


	@Override
	public String fixedLabel() {
		return "Duration";
	}

	@Override
	public String dynamicLabel() {
		return "Duration " + operator.getFriendlyName() + ' ' + expectedMs;
	}


	@Override
	public Class<HasDuration> getEventType() {
		return HasDuration.class;
	}
}
