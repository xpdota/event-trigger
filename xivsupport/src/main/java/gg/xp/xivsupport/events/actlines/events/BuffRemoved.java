package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.models.XivCombatant;
import gg.xp.xivsupport.events.models.XivStatusEffect;

public class BuffRemoved extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasStatusEffect {
	private static final long serialVersionUID = -5438212467951183512L;
	private final XivStatusEffect buff;
	private final double duration;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long stacks;

	public BuffRemoved(XivStatusEffect buff, double duration, XivCombatant source, XivCombatant target, long stacks) {
		this.buff = buff;
		this.duration = duration;
		this.source = source;
		this.target = target;
		this.stacks = stacks;
	}

	@Override
	public XivStatusEffect getBuff() {
		return buff;
	}

	public double getDuration() {
		return duration;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public long getStacks() {
		return stacks;
	}

	@Override
	public String toString() {
		return "BuffRemoved{" +
				"buff=" + buff +
				", duration=" + duration +
				", source=" + source +
				", target=" + target +
				", stacks=" + stacks +
				'}';
	}
}
