package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class MpLoss extends AbilityEffect {
	private final long amount;

	public MpLoss(long flags, long value, long amount) {
		super(flags, value, AbilityEffectType.HEAL);
		this.amount = amount;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("-M(%s)", amount);
	}

	@Override
	public String getBaseDescription() {
		return String.format("Lost MP: %s", amount);
	}
}
