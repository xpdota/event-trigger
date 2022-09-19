package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class SourceHasStatusFilter implements SimpleCondition<HasSourceEntity> {

	@Description("Invert")
	public boolean invert;
	@EditorIgnore
	private final StatusEffectRepository buffs;
	@Description("Status ID")
	@IdType(StatusEffectInfo.class)
	public long expected;

	public SourceHasStatusFilter(@JacksonInject StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	@Override
	public String fixedLabel() {
		return "Source has Status Effect";
	}

	@Override
	public String dynamicLabel() {
		return "Source %s status effect 0x%X".formatted(invert ? "does not have" : "has",  expected);
	}

	@Override
	public boolean test(HasSourceEntity event) {
		return invert != buffs.statusesOnTarget(event.getSource())
				.stream().anyMatch(ba -> ba.buffIdMatches(expected));
	}
}
