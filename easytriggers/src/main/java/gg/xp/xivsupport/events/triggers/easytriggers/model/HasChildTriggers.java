package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasChildTriggers extends HasEventType {

	List<BaseTrigger<?>> getChildren();

	void setChildren(List<BaseTrigger<?>> children);

	void addChild(BaseTrigger<?> child);

	void addChild(BaseTrigger<?> child, int index);

	void removeChild(BaseTrigger<?> child);

}
