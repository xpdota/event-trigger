package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.models.XivStatusEffect;

public interface HasStatusEffect {
	XivStatusEffect getBuff();

	long getStacks();

	default boolean buffIdMatches(long... expected) {
		long id = getBuff().getId();
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
	}

	long getRawStacks();
}
