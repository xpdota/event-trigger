package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class InvulnBlockedDamageEffect extends AbilityEffect implements DamageEffect {
	private final long amount;

	public InvulnBlockedDamageEffect(long amount) {
		super(AbilityEffectType.INVULN);
		this.amount = amount;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("Invuln(%s)", amount);
	}

	@Override
	public String getDescription() {
		return String.format("Invulnerable: %s", amount);
	}
}
