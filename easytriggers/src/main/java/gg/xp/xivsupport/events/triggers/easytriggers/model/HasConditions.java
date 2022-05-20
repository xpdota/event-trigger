package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasConditions<X> {
	List<Condition<? super X>> getConditions();
}
