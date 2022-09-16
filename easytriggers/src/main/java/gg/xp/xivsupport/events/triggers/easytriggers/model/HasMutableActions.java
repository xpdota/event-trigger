package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.List;

public interface HasMutableActions<X> {

	List<Action<? super X>> getActions();

	void setActions(List<Action<? super X>> Actions);

	void addAction(Action<? super X> Action);

	void removeAction(Action<? super X> Action);

	Class<X> classForActions();
}
