package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasMutableConditions<X> extends HasConditions<X> {

	void setConditions(List<Condition<? super X>> conditions);

	void addCondition(Condition<? super X> condition);

	void removeCondition(Condition<? super X> condition);

	Class<X> classForConditions();
}
