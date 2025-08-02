package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.reflect.Method;

public interface SimpleCondition<X> extends Condition<X> {
	@Override
	default boolean test(EasyTriggerContext context, X event) {
		return test(event);
	}

	boolean test(X event);

	@JsonIgnore
	@Override
	default Class<X> getEventType() {
		// Reflectively determine the event type by looking at the
		Class<SimpleCondition<X>> clazz = (Class<SimpleCondition<X>>) getClass();
		for (Method method : clazz.getMethods()) {
			if (!method.getName().equals("test")) {
				continue;
			}
			if (method.getParameterCount() != 1) {
				continue;
			}
			Class<?>[] ptypes = method.getParameterTypes();
			if (!ptypes[0].equals(Object.class)) {
				return (Class<X>) ptypes[0];
			}
		}
		return (Class<X>) Object.class;
	}
}
