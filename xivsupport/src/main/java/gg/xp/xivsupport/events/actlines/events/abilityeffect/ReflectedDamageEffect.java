package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class ReflectedDamageEffect extends AbilityEffect {
	private final BaseDamageEffect reflected;

	public ReflectedDamageEffect(long flags, long value, BaseDamageEffect reflected) {
		super(flags, value, AbilityEffectType.REFLECTED_DAMAGE);
		this.reflected = reflected;
	}

	public BaseDamageEffect getReflectedEffect() {
		return reflected;
	}

	@Override
	protected String getBaseDescription() {
		return "Reflected: " + reflected.getBaseDescription();
	}

	@Override
	public String toString() {
		return "Reflected: " + reflected;
	}
}
