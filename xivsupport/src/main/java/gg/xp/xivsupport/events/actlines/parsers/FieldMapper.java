package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffectContext;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffects;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
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
	private final EntityLookupMissBehavior entityLookupMissBehavior;
	private final String[] rawLineSplit;
	private final List<Long> combatantsToUpdate = new ArrayList<>();
	private boolean recalcNeeded;

	public FieldMapper(Map<K, String> raw, XivState state, EntityLookupMissBehavior entityLookupMissBehavior, String[] rawLineSplit) {
		this.raw = new EnumMap<>(raw);
		this.state = state;
		this.entityLookupMissBehavior = entityLookupMissBehavior;
		this.rawLineSplit = rawLineSplit;
	}

	public String getString(K key) {
		return raw.get(key);
	}

	public long getLong(K key) {
		return Long.parseLong(raw.get(key), 10);
	}

	public int getInt(K key) {
		return Integer.parseInt(raw.get(key), 10);
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

	public int fieldCount() {
		return rawLineSplit.length;
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
		return getEntity(idKey, nameKey, currentHpKey, maxHpKey, currentMpKey, maxMpKey, posXKey, posYKey, posZKey, headingKey, null);
	}

	public boolean hasField(K field) {
		return raw.containsKey(field);
	}

	public XivCombatant getEntity(K idKey, K nameKey, K currentHpKey, K maxHpKey, K currentMpKey, K maxMpKey, K posXKey, K posYKey, K posZKey, K headingKey, K shieldPctKey) {
		XivCombatant cbt = getEntity(idKey, nameKey);
		if (cbt.isEnvironment()) {
			return cbt;
		}
		long id = cbt.getId();
		// Only bother with HP if we either have no idea what it is, or this is a 'trusted' hp line (i.e. not just reading from memory)
		if (cbt.getHp() == null || trustedHp) {
			if (currentHpKey != null && maxHpKey != null) {
				Long curHp = getOptionalLong(currentHpKey);
				if (curHp != null) {
					// Plan A - both the current and max fields are present
					Long maxHp = getOptionalLong(maxHpKey);
					if (maxHp != null) {
						state.provideCombatantHP(cbt, new HitPoints(curHp, maxHp));
					}
					// Plan B - we only have current available, so use stored max and assume it's the same (since max HP changes
					// are not that common).
					else {
						if (cbt.getHp() != null) {
							state.provideCombatantHP(cbt, new HitPoints(curHp, cbt.getHp().max()));
						}
					}
				}
			}
		}
		if (currentMpKey != null && maxMpKey != null) {
			Long curMp = getOptionalLong(currentMpKey);
			if (curMp != null) {
				// Plan A - both the current and max fields are present
				Long maxMp = getOptionalLong(maxMpKey);
				if (maxMp != null) {
					state.provideCombatantMP(cbt, ManaPoints.of(curMp, maxMp));
				}
				// Plan B - we only have current available, so use stored max and assume it's the same (since max MP changes
				// are not that common).
				else {
					if (cbt.getMp() != null) {
						state.provideCombatantMP(cbt, ManaPoints.of(curMp, cbt.getMp().max()));
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
				Position pos = new Position(x, y, z, h);
				state.provideCombatantPos(cbt, pos);
//				cbt = new XivCombatant(cbt.getId(), cbt.getName(), cbt.isPc(), cbt.isThePlayer(), cbt.getRawType(), cbt.getHp(), cbt.getMp(), pos, cbt.getbNpcId(), cbt.getbNpcNameId(), cbt.getPartyType(), cbt.getLevel(), cbt.getOwnerId());
			}
		}
		if (shieldPctKey != null) {
			Long shieldPct = getOptionalLong(shieldPctKey);
			if (shieldPct != null) {
				state.provideCombatantShieldPct(cbt, shieldPct);
			}
		}
		state.provideActFallbackCombatant(cbt);
		XivCombatant stateCbt = state.getCombatant(id);
		if (stateCbt != null) {
			return stateCbt;
		}
		return cbt;
	}

	public XivCombatant getEntity(K idKey) {
		long id = getHex(idKey);
		return getEntity(id);
	}

	public XivCombatant getEntity(long id) {
		XivCombatant xivCombatant = state.getCombatant(id);
		if (xivCombatant != null) {
			return xivCombatant;
		}
		else {
			if (id == 0xE0000000L) {
				return XivCombatant.ENVIRONMENT;
			}
			switch (entityLookupMissBehavior) {
				case GET_AND_WARN:
					log.trace("Did not find combatant info for id {}, guessing", Long.toString(id, 16));
				case GET:
					combatantsToUpdate.add(id);
			}
			XivCombatant cbt = new XivCombatant(id, "Unknown");
			state.provideActFallbackCombatant(cbt);
			XivCombatant stateCbtNew = state.getCombatant(id);
			return stateCbtNew == null ? cbt : stateCbtNew;
		}
	}

	public XivCombatant getEntity(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		XivCombatant xivCombatant = state.getCombatant(id);
		if (xivCombatant != null) {
			return xivCombatant;
		}
		else {
			if (id == 0xE0000000L) {
				return XivCombatant.ENVIRONMENT;
			}
			switch (entityLookupMissBehavior) {
				case GET_AND_WARN:
					log.trace("Did not find combatant info for id {} name '{}', guessing", Long.toString(id, 16), name);
				case GET:
					combatantsToUpdate.add(id);
			}
			XivCombatant cbt = new XivCombatant(id, name);
			state.provideActFallbackCombatant(cbt);
			XivCombatant stateCbtNew = state.getCombatant(id);
			return stateCbtNew == null ? cbt : stateCbtNew;
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
		AbilityEffectContext ctx = new AbilityEffectContext();
		ArrayList<AbilityEffect> out = new ArrayList<>(count);
		for (int i = startIndex; i < startIndex + 2 * count; i += 2) {
			long flags = getRawHex(i);
			long value = getRawHex(i + 1);
			AbilityEffect effect = AbilityEffects.of(flags, value, ctx);
			if (effect != null) {
				out.add(effect);
			}
		}
		out.trimToSize();
		return out;
	}


	public List<Long> getCombatantsToUpdate() {
		return Collections.unmodifiableList(combatantsToUpdate);
	}

	public void flushStateOverrides() {
		state.flushProvidedValues();
	}
}
