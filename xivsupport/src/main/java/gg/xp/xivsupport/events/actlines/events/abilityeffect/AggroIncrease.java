package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class AggroIncrease extends AbilityEffect {
	private final long amount;

	public AggroIncrease(long flags, long value, long amount) {
		super(flags, value, AbilityEffectType.AGGRO_INCREASE);
		this.amount = amount;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("+Aggro(%s)", amount);
	}

	@Override
	public String getBaseDescription() {
		return String.format("Increased Aggro: %s", amount);
	}
}
