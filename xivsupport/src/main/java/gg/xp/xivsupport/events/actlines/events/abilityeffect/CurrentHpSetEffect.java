package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class CurrentHpSetEffect extends AbilityEffect {
	private final long value;

	public CurrentHpSetEffect(long value) {
		super(AbilityEffectType.HP_SET_TO);
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	public String toString() {
		return String.format("HP=%s", value);
	}
}
