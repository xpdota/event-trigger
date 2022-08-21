package gg.xp.xivsupport.events.actlines.events.vfx;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class StatusLoopVfxRemoved extends BaseEvent implements HasTargetEntity, HasStatusLoopVfx {
	@Serial
	private static final long serialVersionUID = -3643333829181102972L;
	private final XivCombatant target;
	private final StatusLoopVfx statusLoopVfx;

	public StatusLoopVfxRemoved(XivCombatant target, BuffRemoved buff) {
		this.target = target;
		this.statusLoopVfx = StatusLoopVfx.of(buff.getRawStacks());
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public StatusLoopVfx getStatusLoopVfx() {
		return statusLoopVfx;
	}


}
