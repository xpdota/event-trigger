package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivsupport.events.actlines.parsers.StatusRemovedEffect;
import org.jetbrains.annotations.Nullable;

public final class AbilityEffects {
	private AbilityEffects() {
	}

	public static @Nullable AbilityEffect of(long flags, long value, AbilityEffectContext ctx) {
		AbilityEffect ae = ofInner(flags, value);
		if (ae instanceof ReflectFlagEffect) {
			ctx.isReflect = true;
		}
		else if (ctx.isReflect && ae instanceof BaseDamageEffect de) {
			return new ReflectedDamageEffect(flags, value, de);
		}
		return ae;
	}

	private static @Nullable AbilityEffect ofInner(long flags, long value) {
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
		// TODO: reflected/punishment damage (vengeance, etc).
		// It appears to be the case that you get something like this (not necessarily adjacent, but always in this relative order):
		// 1. Original damage
		// 2. 1D|70000 (or 50000, not sure what the value actually means)
		// 3. Reflected damage
		// e.g.:
		// 21|2023-08-07T18:41:02.2330000-07:00|1024ABCD|Name Here|4784|Sonic Boom|40011BC8|Electric Aether|356003|D0E0000|1D|50000|1B|4D560000|550003|AAAA000|1B|47848000|0|0|0|0|0|0|
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
				// This actually has two different cases
				// If the value is 0, it means damage was blocked
				//  Omega-Invulnerable! Omega-F takes no damage.
				// If the value is >0, it means a status effect of that value was blocked
				//  Omega-M nullifies the effect of Eukrasian Dosis III.
				if (value == 0) {
					return new InvulnBlockedDamageEffect(flags, 0, 0, calcSeverity(severityByte));
				}
				else {
					// TODO: verify this is the correct flag placement for stacks
					return new StatusNoEffect(flags, value, value >> 16, (int) (flags >> 8) & 0xff);
				}

			case 8:
				return new NoEffect(flags, value);

			// 9 seems to be a miss of sorts
			// I saw it when using PVP deployment tactics on an enemy. Enemies would take the DoT,
			// allies took a 9. Perhaps it means "no effect because friendly fire"?

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

			case 16:
				// TODO: verify this is the correct flag placement for stacks
				return new StatusRemovedEffect(flags, value, value >> 16, (int) (flags >> 8) & 0xff);

			// TODO: 0x11 (17) is also status removed? Is it the same distinction as applying a status where
			// there's remove-from-target and remove-from-caster?

			case 20: //14
				// TODO: verify this is the correct flag placement for stacks
				return new StatusNoEffect(flags, value, value >> 16, (int) (flags >> 8) & 0xff);

			case 24:
				return new AggroIncrease(flags, value, calcDamage(value));

			case 29: //1D
				// reflect
				return new ReflectFlagEffect(flags, value);

			case 61:
				return new TeleportEffect(flags, value);

			// 1d,0x60000 = reflect?
			case 27: //1B
				// This seems to be on a lot of things that aren't involved in combos
				// It's almost always either "1b" or "11b" for flags, and the value is ((abilityId << 16) + 0x8000)
				// Not sure - seems to do "combo" as well as certain boss mechanics like "Subtract" from the math boss
				// This is probably animation related. If the flags are just the ability ID, it's probably animation
				// on the caster, while if there is a bit flipped just to the right of that (e.g. 4784 -> 47848000),
				// then it's on the target?


			case 32:
				// Super cyclone did it
			case 40:
//					 mount -- TODO maybe use the actual mount icons?

				// 59 (3B) - saw in pvp, unclear what it means
				// Deployment Tactics shows it as flags 0x13B, value either 0x566_0000 or 0x567_0000 (buffed vs unbuffed perhaps?)
				// Superflare shows flags 0x1_013B, value 0x553_0000 or 0x554_0000
				// Seems that 553 is the DoT, while 554 is the stack-based additional effect
//
			case 60:
//					// bunch of random stuff like Aether Compass
//					
//
			case 74:
				// Don't know - saw it on Superbolide
				// Okay, not HP set - saw it on Machinist Hypercharge + other MCH abilities
//					return (new CurrentHpSetEffect(calcDamage(value)));
				// Also saw it on EW EX7's set-hp-to-one mechanic, so it could be?
				return new OtherEffect(flags, value);

			case 76:
				return new CastInterruptEffect(flags, value);

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
			// Old?
//			return (long) (data[3] << 16) + (data[0] << 8) + (data[1] - data[3]);
			// New
			return (long) (data[3] << 16) + (data[0] << 8) + (data[1]);
		}
		else {
			return damageRaw >> 16;
		}
	}
}
