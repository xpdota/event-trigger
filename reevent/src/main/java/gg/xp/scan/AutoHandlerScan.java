package gg.xp.scan;

import gg.xp.context.BasicStateStore;
import gg.xp.events.BasicEventDistributor;
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

	public static List<AutoHandler> listAll() {
		log.info("Scanning packages");
		List<AutoHandler> out = new ArrayList<>();
		ClassLoader loader = new ForceReloadClassLoader();
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			// TODO: Reload of existing classes is broken because reloading basic classes such as 'Event'
			// in a new classloader causes the JVM to no longer see it as the same class, causing signature
			// mismatches.
			// This will become significantly less of an issue when stuff is built into separate JARs, but for
			// now, only hot add/remove will be supported, no hot modify.
//			Thread.currentThread().setContextClassLoader(loader);
			Reflections reflections = new Reflections(
					new ConfigurationBuilder()
//							.setClassLoaders(new ClassLoader[]{loader})
//						.addClassLoaders(new ForceReloadClassLoader(Thread.currentThread().getContextClassLoader()))
							.forPackages("")
							.setScanners(Scanners.MethodsAnnotated, Scanners.SubTypes));
			Set<Method> annotatedMethods = reflections.get(MethodsAnnotated.with(HandleEvents.class).as(Method.class));
			log.info("Scan done, setting up topology now");

			Map<Class<?>, List<Method>> classMethodMap = new HashMap<>();
			int methodCount = 0;
			for (Method method : annotatedMethods) {
				if (method.isBridge() || method.isSynthetic()) {
					// If you both annotate the method, and implement EventHandler yourself, you'll get an extra bridge+synthetic
					// method lying around. Safest option is to just ignore stuff if it is synthetic or a bridge.
					// TODO: can this be removed now?
					continue;
				}
				methodCount++;
				Class<?> clazz = method.getDeclaringClass();
				// If you extend a class with an annotated method, the method still "belongs" to the superclass.
				// Thus, we need to explicitly scan for children of the class too.
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
					out.add(rawEvh);
					topo.append(" - Method: ").append(rawEvh.getTopoLabel()).append("\n");
				}
			});
			log_topo.info("Topology:\n{}", topo);
			return out;
		}
		finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}

	}

	public static EventDistributor create() {
		BasicEventDistributor distributor = new BasicEventDistributor(new BasicStateStore());
		listAll().forEach(distributor::registerHandler);
		return distributor;
	}

	// Filter out interfaces and abstract classes
	private static boolean isClassInstantiable(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
	}
}
