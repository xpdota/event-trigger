package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.time.Duration;

public class AbilityCastStart extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasDuration {
	@Serial
	private static final long serialVersionUID = -8156458501097189982L;
	private final XivAbility ability;
	private final XivCombatant source;
	private final XivCombatant target;
	private final Duration duration;

	public AbilityCastStart(XivAbility ability, XivCombatant source, XivCombatant target, double duration) {
		this.ability = ability;
		this.source = source;
		this.target = target;
		this.duration = Duration.ofMillis((long) (duration * 1000.0));
	}

	@Override
	public XivAbility getAbility() {
		return ability;
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
	public Duration getInitialDuration() {
		return duration;
	}
}
