package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class BlockedDamageEffect extends AbilityEffect implements DamageEffect {
	private final long amount;

	public BlockedDamageEffect(long amount) {
		super(AbilityEffectType.BLOCKED);
		this.amount = amount;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("Block(%s)", amount);
	}

	@Override
	public String getDescription() {
		return String.format("Blocked Damage: %s", amount);
	}
}
