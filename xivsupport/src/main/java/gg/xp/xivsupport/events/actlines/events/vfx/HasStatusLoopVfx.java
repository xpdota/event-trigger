package gg.xp.xivsupport.events.actlines.events.vfx;

import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

public interface HasStatusLoopVfx extends HasPrimaryValue {

	StatusLoopVfx getStatusLoopVfx();

	@Override
	default String getPrimaryValue() {
		return String.valueOf(getStatusLoopVfx());
	};
}
