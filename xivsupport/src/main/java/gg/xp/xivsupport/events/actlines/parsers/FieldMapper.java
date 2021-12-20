package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.EventContext;
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
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FieldMapper<K extends Enum<K>> {

	private static final Logger log = LoggerFactory.getLogger(FieldMapper.class);

	private final Map<K, String> raw;
	private final XivState state;
	private final EventContext context;
	private final EntityLookupMissBehavior entityLookupMissBehavior;
	private final String[] rawLineSplit;
	private final List<Long> combatantsToUpdate = new ArrayList<>();
	private boolean recalcNeeded;

	public FieldMapper(Map<K, String> raw, XivState state, EventContext context, EntityLookupMissBehavior entityLookupMissBehavior, String[] rawLineSplit) {
		this.raw = new EnumMap<>(raw);
		this.state = state;
		this.context = context;
		this.entityLookupMissBehavior = entityLookupMissBehavior;
		this.rawLineSplit = rawLineSplit;
	}

	public String getString(K key) {
		return raw.get(key);
	}

	public long getLong(K key) {
		return Long.parseLong(raw.get(key), 10);
	}

	public @Nullable Long getOptionalHex(K key) {
		String rawStr = raw.get(key);
		if (rawStr.isEmpty()) {
			return null;
		}
		return Long.parseLong(rawStr, 16);
	}

	public @Nullable Long getOptionalLong(K key) {
		String rawStr = raw.get(key);
		if (rawStr.isEmpty()) {
			return null;
		}
		return Long.parseLong(rawStr, 10);
	}

	public @Nullable Double getOptionalDouble(K key) {
		String rawStr = raw.get(key);
		if (rawStr.isEmpty()) {
			return null;
		}
		return Double.parseDouble(rawStr);
	}

	public List<String> getRawLineSplit() {
		return Arrays.asList(rawLineSplit);
	}

	public long getHex(K key) {
		return Long.parseLong(raw.get(key), 16);
	}

	public double getDouble(K key) {
		return Double.parseDouble(raw.get(key));
	}

	public XivAbility getAbility(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		return new XivAbility(id, name);
	}

	public XivStatusEffect getStatus(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		return new XivStatusEffect(id, name);
	}

	private boolean trustedHp;

	/**
	 * Indicates that HP from this line is 'trusted' as it comes from network lines rather than memory (which may
	 * not have been updated yet). If not trusted, this line will ONLY be used if we don't already have HP info.
	 *
	 * @param trustedHp Whether to trust HP
	 */
	public void setTrustedHp(boolean trustedHp) {
		this.trustedHp = trustedHp;
	}

	public XivCombatant getEntity(K idKey, K nameKey, K currentHpKey, K maxHpKey, K currentMpKey, K maxMpKey, K posXKey, K posYKey, K posZKey, K headingKey) {
		XivCombatant cbt = getEntity(idKey, nameKey);
		if (cbt.getHp() == null || trustedHp) {
			if (currentHpKey != null && maxHpKey != null) {
				Long curHp = getOptionalLong(currentHpKey);
				if (curHp != null) {
					// Plan A - both the current and max fields are present
					Long maxHp = getOptionalLong(maxHpKey);
					if (maxHp != null) {
						// TODO: collect these all then do them once at the end
						state.provideCombatantHP(cbt, new HitPoints(curHp, maxHp));
						recalcNeeded = true;
					}
					// Plan B - we only have current available, so use stored max and assume it's the same (since max HP changes
					// are not that common).
					else {
						if (cbt.getHp() != null) {
							state.provideCombatantHP(cbt, new HitPoints(curHp, cbt.getHp().getMax()));
							recalcNeeded = true;
						}
					}
				}
			}
		}
		if (posXKey != null && posYKey != null && posZKey != null && headingKey != null) {
			Double x = getOptionalDouble(posXKey);
			Double y = getOptionalDouble(posYKey);
			Double z = getOptionalDouble(posZKey);
			Double h = getOptionalDouble(headingKey);

			if (x != null && y != null && z != null && h != null) {
				state.provideCombatantPos(cbt, new Position(x, y, z, h));
				recalcNeeded = true;
			}
		}
		return cbt;
	}

	public XivCombatant getEntity(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		XivState xivState = context.getStateInfo().get(XivState.class);
		XivCombatant xivCombatant = xivState.getCombatants().get(id);
		if (xivCombatant == null) {
			xivCombatant = xivState.getDeadCombatant(id);
		}
		if (xivCombatant != null) {
			return xivCombatant;
		}
		else {
			if (id == 0xE0000000L) {
				return XivCombatant.ENVIRONMENT;
			}
			switch (entityLookupMissBehavior) {
				// TODO: seems we need a "graveyard" of sorts since removed combatants can still have lingering
				// references to them.
				case GET_AND_WARN:
					log.trace("Did not find combatant info for id {} name '{}', guessing", Long.toString(id, 16), name);
				case GET:
					combatantsToUpdate.add(id);
			}
			return new XivCombatant(id, name);
		}
	}

	private String getRawField(int fieldIndex) {
		return rawLineSplit[fieldIndex];
	}

	public long getRawHex(int fieldIndex) {
		String rawField = getRawField(fieldIndex);
		if (rawField.isEmpty()) {
			return -1;
		}
		return Long.parseLong(rawField, 16);
	}

	public List<AbilityEffect> getAbilityEffects(int startIndex, int count) {

		/*
			[11:52 AM] Ravahn: the 0e suffix on flags means the effect type is 'status effect landed on target'
			[11:53 AM] Ravahn: gotta parse out that effect type and filter on damage effects / heal effect types
			[11:53 AM] Ravahn: 0xbd and 0x13b  are the status ids
			[11:54 AM] Ravahn: dont underestimate the complexity of effect result.  I have about 25 different classes now to parse various aspects of it.
			[11:54 AM] Ravahn: for example, you need to support large damage scaling and reactives / procs.
			[12:05 PM] xp: Hmm, what about when it ends with 0x3d? It seems the actual WD ability usage is 0x7003d for flags, and 0xef for damage, but I'm not sure what EF represents.
			[12:15 PM] Ravahn: so the best way to interpret the action effect data, in the absence of a struct / ui, is as zero-padded dwords:
			0007003d ef000000

			0x3d is the effect type.  in this case, it is something to do with pet actions if I recall correctly, i ignore it in ACT.  the other three bytes in the first dword (00 07 00) are parameters related to that.  the 0xef is probably the index of some cooldown for the pet, just guessing.

			Here are the sapphire action effect types, they are pretty accurate: https://github.com/SapphireServer/Sapphire/blob/aef56c9f336473472147cdeabc0cbc97f440f023/src/common/Common.h#L642
			[12:19 PM] xp: I see
			[12:19 PM] xp: How does it represent combinations of effects? e.g. a DoT that also has initial damage?
			[12:25 PM] Ravahn: multiple pairs of effect data
			[12:25 PM] Ravahn: like 00000003 10100000 (damage) acde000e bd000000 (dot status effect)
			[12:26 PM] Ravahn: might have my 0e and 0f effect types mixed up actually, but you get the point
			[12:26 PM] xp: ahh, so that's why there's so many normally-empty fields there?
			[12:26 PM] xp: that makes more sense, thanks
			[12:29 PM] Ravahn: yea, exactly, there's spots for 8 effects on the source and target.  historically the first 4 were on target, last 4 on source, but SE changed that for status effects and added an 0x80 flag in byte 7 to indicate the effect is on the source.
		 */
		ArrayList<AbilityEffect> out = new ArrayList<>(count);
		for (int i = startIndex; i < startIndex + 2 * count; i += 2) {
			long flags = getRawHex(i);
			long value = getRawHex(i + 1);

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
					continue;
				case 1:
					out.add(new MissEffect());
					break;
				case 2:
					out.add(new FullyResistedEffect());
					break;
				case 3:
					out.add(new DamageTakenEffect(calcSeverity(severityByte), calcDamage(value)));
					break;
				case 4:
					out.add(new HealEffect(calcSeverity(healSeverityByte), calcDamage(value)));
					break;
				case 5:
					out.add(new BlockedDamageEffect(calcDamage(value)));
					break;
				case 6:
					out.add(new ParriedDamageEffect(calcDamage(value)));
					break;
				case 7:
					out.add(new InvulnBlockedDamageEffect(calcDamage(value)));
					break;
				case 8:
					out.add(new NoEffect());
					break;
				case 10:
					out.add(new MpLoss(calcDamage(value)));
					break;
				case 11:
					out.add(new MpGain(calcDamage(value)));
					break;
				case 14: //0e
					out.add(new StatusAppliedEffect(value >> 16, true));
					break;
				case 15: //0f
					out.add(new StatusAppliedEffect(value >> 16, false));
					break;
				case 20: //14
					out.add(new StatusNoEffect(value >> 16));
					break;
				case 27:
					// Not sure - seems to do "combo" as well as certain boss mechanics like "Subtract" from the math boss
					break;
				case 32:
					// Super cyclone did it
				case 40:
//					 mount -- TODO maybe use the actual mount icons?
//					break;
				case 60:
//					// bunch of random stuff like Aether Compass
//					break;
				case 61:
//					// Gauge build?
//					break;
//
				case 74:
					// Don't know - saw it on Superbolide
					// Okay, not HP set - saw it on Machinist Hypercharge + other MCH abilities
//					out.add(new CurrentHpSetEffect(calcDamage(value)));

				default:
					out.add(new OtherEffect(flags, value));
					break;

			}

		}
		out.trimToSize();
		return out;
	}

	private static HitSeverity calcSeverity(byte severity) {
		switch (severity) {
			case 1:
				return HitSeverity.CRIT;
			case 2:
				return HitSeverity.DHIT;
			case 3:
				return HitSeverity.CRIT_DHIT;
			default:
				return HitSeverity.NORMAL;
		}
	}

	@SuppressWarnings("NumericCastThatLosesPrecision")
	private static long calcDamage(long damageRaw) {
		if (damageRaw < 65536) {
			return 0;
		}
		// Get the left two bytes as damage.
		// Check for third byte == 0x40.
		byte[] data = new byte[4];
		long damageRawTmp = damageRaw;
		data[3] = (byte) damageRawTmp;
		data[2] = (byte) (damageRawTmp >>= 8);
		data[1] = (byte) (damageRawTmp >>= 8);
		data[0] = (byte) (damageRawTmp >> 8);
		if (data[2] == 0x40) {
			return (long) (data[3] << 16) + (data[0] << 8) + (data[1] - data[3]);
		}
		else {
			return damageRaw >> 16;
		}
	}

	public List<Long> getCombatantsToUpdate() {
		return Collections.unmodifiableList(combatantsToUpdate);
	}

	public boolean isRecalcNeeded() {
		return recalcNeeded;
	}
}
