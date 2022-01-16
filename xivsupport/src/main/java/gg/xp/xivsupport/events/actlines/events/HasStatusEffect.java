package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.models.XivStatusEffect;

public interface HasStatusEffect {
	XivStatusEffect getBuff();

	long getStacks();
}
