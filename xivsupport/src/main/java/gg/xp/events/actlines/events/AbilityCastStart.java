package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivCombatant;

public class AbilityCastStart extends BaseEvent implements HasSourceEntity, HasTargetEntity {
	private static final long serialVersionUID = -8156458501097189982L;
	private final XivAbility ability;
	private final XivCombatant source;
	private final XivCombatant target;
	private final double duration;

	public AbilityCastStart(XivAbility ability, XivCombatant source, XivCombatant target, double duration) {
		this.ability = ability;
		this.source = source;
		this.target = target;
		this.duration = duration;
	}

	public XivAbility getAbility() {
		return ability;
	}

	public XivCombatant getSource() {
		return source;
	}

	public XivCombatant getTarget() {
		return target;
	}

	public double getDuration() {
		return duration;
	}
}
