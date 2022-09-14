package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;


public class ChatLineTypeFilter implements SimpleCondition<ChatLineEvent> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Chat Line Type Number")
	public int expected;

	@Override
	public boolean test(ChatLineEvent event) {
		return operator.checkLong(event.getCode(), expected);
	}


	@Override
	public String fixedLabel() {
		return "Chat Line Number";
	}

	@Override
	public String dynamicLabel() {
		return "Chat Line Number " + operator.getFriendlyName() + ' ' + expected;
	}
}
