package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfx;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxRemoved;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatusLoopVfxTracker {

	private final Map<XivCombatant, StatusLoopVfxApplied> cache = new ConcurrentHashMap<>();

	private final StatusEffectRepository buffs;

	public StatusLoopVfxTracker(StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	@HandleEvents(order = -400)
	public void buffApplied(EventContext context, BuffApplied event) {
		if (event.getBuff().getId() == 0x808) {
			XivCombatant target = event.getTarget();
			StatusLoopVfxApplied out = new StatusLoopVfxApplied(target, event);
			context.accept(out);
			cache.put(target, out);
		}
	}

	@HandleEvents(order = -400)
	public void buffRemoved(EventContext context, BuffRemoved event) {
		if (event.getBuff().getId() == 0x808) {
			XivCombatant target = event.getTarget();
			context.accept(new StatusLoopVfxRemoved(target, event));
			cache.remove(target);
		}
	}

	public @Nullable StatusLoopVfx getCurrent(XivCombatant cbt) {
		StatusLoopVfxApplied computed = cache.compute(cbt, (c, cached) -> {
			if (cached == null) {
				return null;
			}
			BuffApplied original = cached.getOriginalEvent();
			BuffApplied latest = buffs.getLatest(original);
			if (latest == null) {
				return null;
			}
			if (original == latest) {
				return cached;
			}
			else {
				return new StatusLoopVfxApplied(c, latest);
			}
		});
		if (computed == null) {
			return null;
		}
		else {
			return computed.getStatusLoopVfx();
		}


	}

}
