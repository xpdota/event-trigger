package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class AbilityEffect {

	private final AbilityEffectType effectType;

	protected AbilityEffect(AbilityEffectType effectType) {
		this.effectType = effectType;
	}

	public AbilityEffectType getEffectType() {
		return effectType;
	}

	public String getDescription() {
		return toString();
	}
}
