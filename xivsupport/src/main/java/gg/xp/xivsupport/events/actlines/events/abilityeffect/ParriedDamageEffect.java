package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class ParriedDamageEffect extends AbilityEffect implements DamageEffect {
	private final long amount;

	public ParriedDamageEffect(long amount) {
		super(AbilityEffectType.PARRIED);
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
	public String getDescription() {
		return String.format("Parried: %s", amount);
	}
}
