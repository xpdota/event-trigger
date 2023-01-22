package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TargetHasStatusFilter implements SimpleCondition<HasTargetEntity> {

	@Description("Invert")
	public boolean invert;
	@EditorIgnore
	private final StatusEffectRepository buffs;
	@Description("Status ID")
	@IdType(value = StatusEffectInfo.class, matchRequired = false)
	public long expected;

	public TargetHasStatusFilter(@JacksonInject(useInput = OptBoolean.FALSE) StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	@Override
	public String fixedLabel() {
		return "Target has Status Effect";
	}

	@Override
	public String dynamicLabel() {
		return "Target %s status effect 0x%X".formatted(invert ? "does not have" : "has", expected);
	}

	@Override
	public boolean test(HasTargetEntity event) {
		return invert != buffs.statusesOnTarget(event.getTarget())
				.stream().anyMatch(ba -> ba.buffIdMatches(expected));
	}
}
