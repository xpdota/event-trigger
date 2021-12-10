package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class MpLoss extends AbilityEffect {
	private final long amount;

	public MpLoss(long amount) {
		super(AbilityEffectType.HEAL);
		this.amount = amount;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("-M(%s)", amount);
	}
}
