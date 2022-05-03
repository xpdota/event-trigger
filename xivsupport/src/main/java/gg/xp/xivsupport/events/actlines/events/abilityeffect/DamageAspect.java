package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public enum DamageAspect {
	Unknown,
	Fire,
	Ice,
	Wind,
	Earth,
	Lighting,
	Water,
	Unaspected;

	public static DamageAspect forByte(int value) {
		if (value < 0 || value > Unaspected.ordinal()) {
			return Unknown;
		}
		else {
			return values()[value];
		}
	}
}
