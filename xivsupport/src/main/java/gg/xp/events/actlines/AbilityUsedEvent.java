package gg.xp.events.actlines;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivEntity;

public class AbilityUsedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {

	private final XivAbility ability;
	private final XivEntity caster;
	private final XivEntity target;

	public AbilityUsedEvent(XivAbility ability, XivEntity caster, XivEntity target) {
		this.ability = ability;
		this.caster = caster;
		this.target = target;
	}

	public XivAbility getAbility() {
		return ability;
	}

	@Override
	public XivEntity getSource() {
		return caster;
	}

	@Override
	public XivEntity getTarget() {
		return target;
	}
}
