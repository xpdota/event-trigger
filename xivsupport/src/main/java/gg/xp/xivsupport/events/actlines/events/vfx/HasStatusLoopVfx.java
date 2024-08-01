package gg.xp.xivsupport.events.actlines.events.vfx;

import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

public interface HasStatusLoopVfx extends HasPrimaryValue {

	StatusLoopVfx getStatusLoopVfx();

	@Override
	default String getPrimaryValue() {
		return String.valueOf(getStatusLoopVfx());
	};

	default boolean vfxIdMatches(long... expected) {
		long id = getStatusLoopVfx().getId();
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
	}
}
