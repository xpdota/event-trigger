package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;

public class SourceEntityNpcIdFilter implements Condition<HasSourceEntity> {

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
}
