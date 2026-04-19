package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;

import java.lang.reflect.Method;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface SqAction<X extends BaseEvent> extends Action<X> {
	void accept(SequentialTriggerController<X> stc, EasyTriggerContext context, X event);

	// TODO: try to untangle this more
	@Override
	default void accept(EasyTriggerContext context, X event) {
		throw new IllegalStateException("This method should not be called on SqAction");
	}

	@JsonIgnore
	default Class<X> getEventType() {
		// Reflectively determine the event type by looking at the
		Class<SqAction<X>> clazz = (Class<SqAction<X>>) getClass();
		for (Method method : clazz.getMethods()) {
			if (!method.getName().equals("accept")) {
				continue;
			}
			if (method.getParameterCount() != 3) {
				continue;
			}
			Class<?>[] ptypes = method.getParameterTypes();
			if (ptypes[1].equals(EasyTriggerContext.class) && !ptypes[2].equals(Object.class)) {
				return (Class<X>) ptypes[2];
			}
		}
		return (Class<X>) BaseEvent.class;
	}
}
