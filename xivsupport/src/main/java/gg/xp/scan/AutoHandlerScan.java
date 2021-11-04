package gg.xp.scan;

import gg.xp.events.BasicEvent;
import gg.xp.events.BasicEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventHandler;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.reflections.scanners.Scanners.MethodsAnnotated;

public class AutoHandlerScan {

	private static final Logger log = LoggerFactory.getLogger(AutoHandlerScan.class);

	public static EventDistributor<Event> create() {
		Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("gg").setScanners(MethodsAnnotated));
		Set<Method> annotatedMethods = reflections.get(MethodsAnnotated.with(HandleEvents.class).as(Method.class));

		Map<Class<?>, List<Method>> classMethodMap = new HashMap<>();
		int methodCount = 0;
		for (Method method : annotatedMethods) {
			if (method.isBridge() || method.isSynthetic()) {
				// If you both annotate the method, and implement EventHandler yourself, you'll get an extra bridge+synthetic
				// method lying around. Safest option is to just ignore stuff if it is synthetic or a bridge.
				continue;
			}
			methodCount ++;
			Class<?> clazz = method.getDeclaringClass();
			classMethodMap.computeIfAbsent(clazz, unused -> new ArrayList<>()).add(method);
		}
		log.info("Methods: {}", methodCount);

		BasicEventDistributor distributor = new BasicEventDistributor();

		classMethodMap.forEach((clazz, methods) -> {
			Object clazzInstance;
			try {
				clazzInstance = clazz.getConstructor((Class<?>[]) null).newInstance();
			}
			catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException("Error instantiating event handler class " + clazz.getSimpleName(), e);
			}
			for (Method method : methods) {
				// TODO: exception types
				Class<?>[] paramTypes = method.getParameterTypes();
				String methodLabel = method.getDeclaringClass().getSimpleName() + '.' + method.getName();
				log.info("Setting up method {}", methodLabel);
				if (paramTypes.length != 2) {
					throw new IllegalStateException("Error setting up method " + methodLabel + ": wrong number of parameters (should be 2)");
				}
				if (!EventContext.class.isAssignableFrom(paramTypes[0]) || !Event.class.isAssignableFrom(paramTypes[1])) {
					throw new IllegalStateException("Error setting up method " + methodLabel + ": method signature must be (EventContext, Event)");
				}
				Class<? extends Event> eventClass = (Class<? extends Event>) paramTypes[1];
				EventHandler<? extends Event> rawEvh = (context, event) -> {
					try {
						method.invoke(clazzInstance, context, event);
					}
					catch (IllegalAccessException | InvocationTargetException e) {
						log.error("Error invoking trigger method {}", methodLabel, e);
					}
				};
				distributor.registerHandler((Class) eventClass, rawEvh);
			}
		});
		return distributor;
	}

}
