package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.time.Duration;

@SystemEvent
public class AnimationLockEvent extends BaseEvent implements HasDuration, HasAbility, HasSourceEntity, HasTargetEntity {
	@Serial
	private static final long serialVersionUID = 958709967151453796L;
	private final AbilityUsedEvent original;
	private final Duration duration;

	public AnimationLockEvent(AbilityUsedEvent original, double duration) {
		this.duration = Duration.ofMillis((long) (duration * 1000.0));
		this.original = original;
	}

	@Override
	public XivAbility getAbility() {
		return original.getAbility();
	}

	@Override
	public XivCombatant getSource() {
		return original.getSource();
	}

	@Override
	public XivCombatant getTarget() {
		return original.getTarget();
	}

	@Override
	public Duration getInitialDuration() {
		return duration;
	}
}
