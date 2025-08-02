package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class SourceEntityNpcIdFilter implements SimpleCondition<HasSourceEntity> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("NPC ID")
	public long expected;

	@Override
	public boolean test(HasSourceEntity event) {
		return operator.checkLong(event.getSource().getbNpcId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Source NPC ID";
	}

	@Override
	public String dynamicLabel() {
		return "Source NPC ID " + operator.getFriendlyName() + ' ' + expected;
	}

	@Override
	public Class<HasSourceEntity> getEventType() {
		return HasSourceEntity.class;
	}
}
