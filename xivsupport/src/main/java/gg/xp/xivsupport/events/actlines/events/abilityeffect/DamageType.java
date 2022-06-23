package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public enum DamageType {
	Unknown,
	Slashing,
	Piercing,
	Blunt,
	Shot,
	Magic,
	Breath,
	Sound,
	LimitBreak;

	public static DamageType forByte(int value) {
		if (value < 0 || value > LimitBreak.ordinal()) {
			return Unknown;
		}
		else {
			return values()[value];
		}
	}
}
