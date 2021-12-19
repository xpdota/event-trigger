package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.models.XivCombatant;

enum NetworkAbilityFields {
	casterId, casterName, abilityId, abilityName, targetId, targetName,
	flags1, damage1,
	flags2, damage2,
	flags3, damage3,
	flags4, damage4,
	flags5, damage5,
	flags6, damage6,
	flags7, damage7,
	flags8, damage8,
	targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading,
	casterCurHp, casterMaxHp, casterCurMp, casterMaxMp, casterUnknown1, casterUnknown2, casterX, casterY, casterZ, casterHeading,
	sequenceId,
	targetIndex;

	public static Event convert(FieldMapper<NetworkAbilityFields> fields) {
		XivCombatant caster = fields.getEntity(casterId, casterName, casterCurHp, casterMaxHp, casterCurMp, casterMaxMp, casterX, casterY, casterZ, casterHeading);
		XivCombatant target = fields.getEntity(targetId, targetName, targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetX, targetY, targetZ, targetHeading);
		return new AbilityUsedEvent(
				fields.getAbility(abilityId, abilityName),
				caster,
				target,
				fields.getAbilityEffects(targetName.ordinal() + 3, 8),
				fields.getRawHex(44)
		);
	}
}
