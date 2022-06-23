package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class CurrentHpSetEffect extends AbilityEffect {
	private final long hpAmount;

	public CurrentHpSetEffect(long flags, long value, long hpAmount) {
		super(flags, value, AbilityEffectType.HP_SET_TO);
		this.hpAmount = value;
	}

	public long getHpAmount() {
		return hpAmount;
	}

	public String toString() {
		return String.format("HP=%s", hpAmount);
	}
}
