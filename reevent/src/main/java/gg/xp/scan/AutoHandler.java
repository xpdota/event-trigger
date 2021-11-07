package gg.xp.scan;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AutoHandler implements EventHandler<Event> {

	private static final Logger log = LoggerFactory.getLogger(AutoHandler.class);

	private final Method method;
	private final String methodLabel;
	private final Object clazzInstance;
	private final Class<? extends Event> eventClass;

	public AutoHandler(Method method, Object clazzInstance) {
		// TODO: exception types
		Class<?>[] paramTypes = method.getParameterTypes();
		String tmpMethodLabel = method.getDeclaringClass().getSimpleName() + '.' + method.getName();
		log.trace("Setting up method {}", tmpMethodLabel);
		if (paramTypes.length != 2) {
			throw new IllegalStateException("Error setting up method " + tmpMethodLabel + ": wrong number of parameters (should be 2)");
		}
		if (!EventContext.class.isAssignableFrom(paramTypes[0]) || !Event.class.isAssignableFrom(paramTypes[1])) {
			throw new IllegalStateException("Error setting up method " + tmpMethodLabel + ": method signature must be (EventContext, Event)");
		}
		this.eventClass = (Class<? extends Event>) paramTypes[1];
		this.method = method;
		String fullMethodLabel = method.getDeclaringClass().getSimpleName() + '.' + method.getName() + ':' + eventClass.getSimpleName();
		this.methodLabel = fullMethodLabel;
		this.clazzInstance = clazzInstance;
	}

	@Override
	public String toString() {
		return String.format("AutoHandler(%s)", methodLabel);
	}

	public String getTopoLabel() {
		return String.format("%s(%s)", method.getName(), eventClass.getSimpleName());
	}


	@Override
	public void handle(EventContext<Event> context, Event event) {
		if (!eventClass.isInstance(event)) {
			return;
		}
		if (clazzInstance instanceof FilteredEventHandler) {
			if (!((FilteredEventHandler) clazzInstance).enabled(context)) {
				return;
			}
		}
		try {
			method.invoke(clazzInstance, context, event);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			log.error("Error invoking trigger method {}", methodLabel, e);
		}
	}
}
