package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class ParriedDamageEffect extends AbilityEffect implements DamageEffect {
	private final long amount;

	public ParriedDamageEffect(long flags, long value, long amount) {
		super(flags, value, AbilityEffectType.PARRIED);
		this.amount = amount;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("Parry(%s)", amount);
	}

	@Override
	public String getBaseDescription() {
		return String.format("Parried: %s", amount);
	}
}
