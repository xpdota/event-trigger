package gg.xp.scan;

import gg.xp.events.BasicEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.SubTypes;

@SuppressWarnings("ClassWithMultipleLoggers")
public class AutoHandlerScan {

	private static final Logger log = LoggerFactory.getLogger(AutoHandlerScan.class);
	// Secondary logger for topology so it can be controlled differently
	private static final Logger log_topo = LoggerFactory.getLogger(AutoHandlerScan.class.getCanonicalName() + ".Topology");

	public static EventDistributor<Event> create() {
		Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("gg").setScanners(Scanners.MethodsAnnotated, Scanners.SubTypes));
		Set<Method> annotatedMethods = reflections.get(MethodsAnnotated.with(HandleEvents.class).as(Method.class));

		Map<Class<?>, List<Method>> classMethodMap = new HashMap<>();
		int methodCount = 0;
		for (Method method : annotatedMethods) {
			if (method.isBridge() || method.isSynthetic()) {
				// If you both annotate the method, and implement EventHandler yourself, you'll get an extra bridge+synthetic
				// method lying around. Safest option is to just ignore stuff if it is synthetic or a bridge.
				continue;
			}
			methodCount++;
			Class<?> clazz = method.getDeclaringClass();
			Set<Class<?>> implementingClasses = reflections.get(SubTypes.of(clazz).asClass());
			if (!implementingClasses.isEmpty()) {
				log.info("Class {} has implementors: {}", clazz, implementingClasses);
			}
			//noinspection SimplifyForEach
			Stream.concat(Stream.of(clazz), implementingClasses.stream())
					.filter(AutoHandlerScan::isClassInstantiable)
					.forEach(cls -> classMethodMap.computeIfAbsent(cls, unused -> new ArrayList<>()).add(method));
		}
		log.info("Methods: {}", methodCount);

		BasicEventDistributor distributor = new BasicEventDistributor();

		StringBuilder topo = new StringBuilder();
		classMethodMap.forEach((clazz, methods) -> {
			// TODO: error handling
			topo.append("Class: ").append(clazz.getSimpleName()).append("\n");
			// TODO: move this to AutoHandler so scope can be implemented
			Object clazzInstance;
			try {
				clazzInstance = clazz.getConstructor((Class<?>[]) null).newInstance();
			}
			catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException("Error instantiating event handler class " + clazz.getSimpleName(), e);
			}
			for (Method method : methods) {
				AutoHandler rawEvh = new AutoHandler(method, clazzInstance);
				distributor.registerHandler(rawEvh);
				topo.append(" - Method: ").append(rawEvh.getTopoLabel()).append("\n");
			}
		});
		log_topo.info("Topology:\n{}", topo);
		return distributor;
	}

	private static boolean isClassInstantiable(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
	}
}
