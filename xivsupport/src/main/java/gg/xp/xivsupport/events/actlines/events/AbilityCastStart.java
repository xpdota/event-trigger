package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class AbilityCastStart extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility {
	@Serial
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

	public double getDuration() {
		return duration;
	}
}
