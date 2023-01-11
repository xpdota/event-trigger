package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivdata.data.*;

public record PlayerStats(
		Job job,
		int strength,
		int dexterity,
		int vitality,
		int intelligence,
		int mind,
		int piety,
		int attackPower,
		int directHit,
		int criticalHit,
		int attackMagicPotency,
		int determination,
		int skillSpeed,
		int spellSpeed,
		int unknown0,
		int tenacity,
		int localContentId
) {

}
