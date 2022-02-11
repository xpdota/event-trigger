package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.jobs.StatusEffectIcon;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.io.Serial;

public class BuffRemoved extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasStatusEffect {
	@Serial
	private static final long serialVersionUID = -5438212467951183512L;
	private final XivStatusEffect buff;
	private final double duration;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long rawStacks;
	private final long stacks;

	public BuffRemoved(XivStatusEffect buff, double duration, XivCombatant source, XivCombatant target, long rawStacks) {
		this.buff = buff;
		this.duration = duration;
		this.source = source;
		this.target = target;
		this.rawStacks = rawStacks;
		long maxStacks = StatusEffectIcon.getCsvValues().get(buff.getId()).getNumStacks();
		if (rawStacks >= 0 && rawStacks <= maxStacks) {
			stacks = rawStacks;
		}
		else {
			stacks = 0;
		}
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

	@Override
	public long getStacks() {
		return stacks;
	}

	public long getRawStacks() {
		return rawStacks;
	}

	@Override
	public String toString() {
		return "BuffRemoved{" +
				"buff=" + buff +
				", duration=" + duration +
				", source=" + source +
				", target=" + target +
				", stacks=" + rawStacks +
				'}';
	}
}
