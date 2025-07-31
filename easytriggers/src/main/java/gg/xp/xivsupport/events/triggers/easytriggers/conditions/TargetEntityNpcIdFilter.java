package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TargetEntityNpcIdFilter implements SimpleCondition<HasTargetEntity> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("NPC ID")
	public long expected;

	@Override
	public boolean test(HasTargetEntity event) {
		return operator.checkLong(event.getTarget().getbNpcId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Target NPC ID";
	}

	@Override
	public String dynamicLabel() {
		return "Target NPC ID " + operator.getFriendlyName() + ' ' + expected;
	}

	@Override
	public Class<HasTargetEntity> getEventType() {
		return HasTargetEntity.class;
	}
}
