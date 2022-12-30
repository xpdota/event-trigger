package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TetherIdFilter implements SimpleCondition<TetherEvent> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Tether ID: ")
	@IdType(ActionInfo.class)
	public long expected;

	@Override
	public boolean test(TetherEvent tether) {
		return operator.checkLong(tether.getId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Tether ID";
	}

	@Override
	public String dynamicLabel() {
		return "Tether ID " + operator.getFriendlyName() + ' ' + expected;
	}
}
