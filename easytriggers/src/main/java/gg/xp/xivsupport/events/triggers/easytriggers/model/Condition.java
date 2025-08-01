package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Condition<X> {
	@SuppressWarnings("unused")
	boolean test(EasyTriggerContext context, X event);

	@Nullable String fixedLabel();

	String dynamicLabel();

	default void recalc() {
	}

	default int sortOrder() {
		return 0;
	}

	@JsonIgnore
	default Class<X> getEventType() {
		// Reflectively determine the event type by looking at the
		Class<Condition<X>> clazz = (Class<Condition<X>>) getClass();
		for (Method method : clazz.getMethods()) {
			if (!method.getName().equals("test")) {
				continue;
			}
			if (method.getParameterCount() != 2) {
				continue;
			}
			Class<?>[] ptypes = method.getParameterTypes();
			if (ptypes[0].equals(EasyTriggerContext.class) && !ptypes[1].equals(Object.class)) {
				return (Class<X>) ptypes[1];
			}
		}
		return (Class<X>) Object.class;
	}
}
