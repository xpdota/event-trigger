package gg.xp.xivsupport.events.actlines.events.vfx;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class StatusLoopVfxApplied extends BaseEvent implements HasTargetEntity, HasStatusLoopVfx {
	@Serial
	private static final long serialVersionUID = 3105290285114091029L;
	private final XivCombatant target;
	private final StatusLoopVfx statusLoopVfx;
	private final BuffApplied originalEvent;

	public StatusLoopVfxApplied(XivCombatant target, BuffApplied buff) {
		this.target = target;
		originalEvent = buff;
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

	public BuffApplied getOriginalEvent() {
		return originalEvent;
	}
}
