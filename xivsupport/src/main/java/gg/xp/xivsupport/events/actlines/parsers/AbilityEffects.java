package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.BlockedDamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.FullyResistedEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.InvulnBlockedDamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.MissEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.MpGain;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.MpLoss;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.NoEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.OtherEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.ParriedDamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusNoEffect;
import org.jetbrains.annotations.Nullable;

public final class AbilityEffects {
	private AbilityEffects() {
	}

	// TODO: remove all null returns
	public static @Nullable AbilityEffect of(long flags, long value) {
		byte effectTypeByte;
		byte severityByte;
		byte healSeverityByte;
		byte unknownByte;
		long flagsTmp = flags;
		effectTypeByte = (byte) flagsTmp;
		severityByte = (byte) (flagsTmp >>= 8);
		healSeverityByte = (byte) (flagsTmp >>= 8);
		unknownByte = (byte) (flagsTmp >> 8);

		// TODO: it's unclear which of these need the "lots of damage" calculation applied - IDs that point to an
		// ability or status will probably be safe for the forseeable future, since there's nowhere close to
		// 65536 statuses.
		switch (effectTypeByte) {
			case 0:
				// nothing
				return null;
			case 1:
				return new MissEffect(flags, value);

			case 2:
				return new FullyResistedEffect(flags, value);

			case 3:
				return new DamageTakenEffect(flags, value, calcSeverity(severityByte), calcDamage(value));

			case 4:
				return new HealEffect(flags, value, calcSeverity(healSeverityByte), calcDamage(value));

			case 5:
				return new BlockedDamageEffect(flags, value, calcDamage(value), calcSeverity(severityByte));

			case 6:
				return new ParriedDamageEffect(flags, value, calcDamage(value), calcSeverity(severityByte));

			case 7:
				return new InvulnBlockedDamageEffect(flags, value, calcDamage(value), calcSeverity(severityByte));

			case 8:
				return new NoEffect(flags, value);

			case 10:
				return new MpLoss(flags, value, calcDamage(value));

			case 11:
				return new MpGain(flags, value, calcDamage(value));
			

				/*
					Notes specific to 0e/0f:
					flags:
					stacks, a, b, 0e/0f
					value: status id MSB, status id LSB, x, y

					a = ?
					b = ?
					x = ?
					y = ?

					Examples:
					00F4300E 0A380000: E Dosis (single target, instant, on target, no stacks, 30s)
					0000F60E 0A3A0000: Kera mit (aoe, on target, no stacks, 15s)
					00F4F20E 0B7A0000: Kera regen (aoe, on target, no stacks, 15s)
					0300000F 076E8000: Royal Auth/Sword Oath (st, instant, on self, 3 stacks, 30s)
					00C9090E 0A350000: panhaima persistent buff (aoe, on target, no stacks, 15s)
					05C9090E 0A530000: panhaima stack buff (aoe, on target, 5 stacks, 15s)
					0000000E 00520000: halloed (st, 10s, no stacks)

				 */


			case 14: //0e
				return new StatusAppliedEffect(flags, value, value >> 16, unknownByte, true);

			case 15: //0f
				return new StatusAppliedEffect(flags, value, value >> 16, unknownByte, false);

			case 20: //14
				return new StatusNoEffect(flags, value, value >> 16);

			// 1d,0x60000 = reflect?
			case 27: //1B
				// Not sure - seems to do "combo" as well as certain boss mechanics like "Subtract" from the math boss

			case 32:
				// Super cyclone did it
			case 40:
//					 mount -- TODO maybe use the actual mount icons?
//					
			case 60:
//					// bunch of random stuff like Aether Compass
//					
			case 61:
//					// Gauge build?
//					
//
			case 74:
				// Don't know - saw it on Superbolide
				// Okay, not HP set - saw it on Machinist Hypercharge + other MCH abilities
//					return (new CurrentHpSetEffect(calcDamage(value)));

			default:
				return new OtherEffect(flags, value);

		}
	}

	private static HitSeverity calcSeverity(byte severity) {
		return switch (severity) {
			// Fortunately, the pre- and post-6.1 values do not overlap, so they can both be supported simultaneously
			// Unfortunately, they seem to conflict with other bit flags, so disabling for now
//			case 0x20, 1 -> HitSeverity.CRIT;
//			case 0x40, 2 -> HitSeverity.DHIT;
//			case 0x60, 3 -> HitSeverity.CRIT_DHIT;
			case 0x20 -> HitSeverity.CRIT;
			case 0x40 -> HitSeverity.DHIT;
			case 0x60 -> HitSeverity.CRIT_DHIT;

			default -> HitSeverity.NORMAL;
		};
	}

	@SuppressWarnings("NumericCastThatLosesPrecision")
	private static long calcDamage(long damageRaw) {
		if (damageRaw < 65536) {
			return 0;
		}
		// Get the left two bytes as damage.
		// Check for third byte == 0x40.
		int[] data = new int[4];
		long damageRawTmp = damageRaw;
		data[3] = 0xff & (byte) damageRawTmp;
		data[2] = 0xff & (byte) (damageRawTmp >>= 8);
		data[1] = 0xff & (byte) (damageRawTmp >>= 8);
		data[0] = 0xff & (byte) (damageRawTmp >> 8);
		if (data[2] == 0x40) {
			return (long) (data[3] << 16) + (data[0] << 8) + (data[1] - data[3]);
		}
		else {
			return damageRaw >> 16;
		}
	}
}
