package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.misc.NpcYellEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;


public class NpcYellIdFilter implements SimpleCondition<NpcYellEvent> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Npc Yell ID")
	@IdType(value = NpcYellInfo.class, matchRequired = false)
	public long expected;

	@Override
	public boolean test(NpcYellEvent event) {
		return operator.checkLong(event.getYell().id(), expected);
	}


	@Override
	public String fixedLabel() {
		return "NPC Yell ID";
	}

	@Override
	public String dynamicLabel() {
		return String.format("NPC Yell ID %s 0x%x (%s)", operator.getFriendlyName(), expected, expected);
	}

	@Override
	public Class<NpcYellEvent> getEventType() {
		return NpcYellEvent.class;
	}
}
