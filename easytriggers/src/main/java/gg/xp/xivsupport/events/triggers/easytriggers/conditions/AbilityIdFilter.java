package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.models.XivAbility;

public class AbilityIdFilter implements SimpleCondition<HasAbility> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Ability ID")
	@IdType(ActionInfo.class)
	public long expected;

	@Override
	public boolean test(HasAbility hasAbility) {
		return operator.checkLong(hasAbility.getAbility().getId(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Ability ID";
	}

	@Override
	public String dynamicLabel() {
		return "Ability ID " + operator.getFriendlyName() + ' ' + expected;
	}
}
