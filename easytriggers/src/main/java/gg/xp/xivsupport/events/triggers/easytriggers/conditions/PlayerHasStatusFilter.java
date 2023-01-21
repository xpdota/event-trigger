package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class PlayerHasStatusFilter implements SimpleCondition<Event> {

	@Description("Invert")
	public boolean invert;
	@EditorIgnore
	private final XivState state;
	@EditorIgnore
	private final StatusEffectRepository buffs;
	@Description("Status ID")
	@IdType(value = StatusEffectInfo.class, matchRequired = false)
	public long expected;

	public PlayerHasStatusFilter(@JacksonInject(useInput = OptBoolean.FALSE) XivState state, @JacksonInject(useInput = OptBoolean.FALSE) StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public String fixedLabel() {
		return "Player has Status Effect";
	}

	@Override
	public String dynamicLabel() {
		return "Player %s status effect 0x%X".formatted(invert ? "does not have" : "has", expected);
	}

	@Override
	public boolean test(Event event) {
		return invert != buffs.statusesOnTarget(state.getPlayer())
				.stream().anyMatch(ba -> ba.buffIdMatches(expected));
	}
}
