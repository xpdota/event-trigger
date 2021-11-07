package gg.xp.events;

public class AbilityUsedEvent extends BaseEvent {

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

	public XivEntity getCaster() {
		return caster;
	}

	public XivEntity getTarget() {
		return target;
	}
}
