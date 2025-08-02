package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasChildTriggers extends HasEventType {

	List<BaseTrigger<?>> getChildTriggers();

	void setChildTriggers(List<BaseTrigger<?>> children);

	void addChildTrigger(BaseTrigger<?> child);

	void addChildTrigger(BaseTrigger<?> child, int index);

	void removeChildTriggers(BaseTrigger<?> child);

}
