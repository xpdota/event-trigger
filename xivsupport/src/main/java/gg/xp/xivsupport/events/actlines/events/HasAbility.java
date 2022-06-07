package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.models.XivAbility;

public interface HasAbility {
	XivAbility getAbility();

	default boolean abilityIdMatches(long... expected) {
		long id = getAbility().getId();
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
	}
}
