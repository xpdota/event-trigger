package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class MpGain extends AbilityEffect {
	private final long amount;

	public MpGain(long amount) {
		super(AbilityEffectType.HEAL);
		this.amount = amount;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("+M(%s)", amount);
	}
}
