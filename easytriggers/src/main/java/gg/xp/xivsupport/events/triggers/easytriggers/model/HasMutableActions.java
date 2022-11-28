package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasMutableActions<X> {

	List<Action<? super X>> getActions();

	void setActions(List<Action<? super X>> actions);

	void addAction(Action<? super X> action);

	void addAction(Action<? super X> action, int index);

	void removeAction(Action<? super X> action);

	Class<X> classForActions();
}
