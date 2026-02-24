package gg.xp.xivsupport.events.actlines.events.abilityeffect;

// TODO: should move this to xivdata, but that would be a breaking change.
public enum DamageType {
	Unknown,
	Slashing,
	Piercing,
	Blunt,
	Shot,
	Magic,
	Breath,
	Sound,
	LimitBreak,
	/**
	 * WeaponOverride is a special damage type that uses the damage type of the equipped weapon.
	 * This isn't an actual damage type that you would receive from the server, as that would have the real damage
	 * type. This is purely found in game files, to indicate that the action uses the weapon's damage type.
	 */
	WeaponOverride
	;

	public static DamageType forByte(int value) {
		if (value == -1) {
			return WeaponOverride;
		}
		else if (value < 0 || value > LimitBreak.ordinal()) {
			return Unknown;
		}
		else {
			return values()[value];
		}
	}
}
