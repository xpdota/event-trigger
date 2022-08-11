package gg.xp.reevent.scan;

import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.SubTypes;

@SuppressWarnings("ClassWithMultipleLoggers")
public class AutoHandlerScan {

	private static final Logger log = LoggerFactory.getLogger(AutoHandlerScan.class);
	// Secondary logger for topology so it can be controlled differently
	private static final Logger log_topo = LoggerFactory.getLogger(AutoHandlerScan.class.getCanonicalName() + ".Topology");
	private final AutoHandlerInstanceProvider instanceProvider;
	private final AutoHandlerConfig config;
	private static final Pattern jarFileName = Pattern.compile("([a-zA-Z0-9\\-.]+)\\.jar");
	private static final List<String> scanBlacklist = List.of("groovy", "jna", "jackson", "javaassist", "httpclient", "commons", "xivdata", "sfl4j", "logback", "picocontainer", "opencsv", "Java-WebSocket", "reflections", "annotations", "httpcore");

	public AutoHandlerScan(AutoHandlerInstanceProvider instanceProvider, AutoHandlerConfig config) {
		this.instanceProvider = instanceProvider;
		this.config = config;
	}

	public List<AutoHandler> build() {
		log.info("Scanning packages");
		List<AutoHandler> out = new ArrayList<>();
//		ClassLoader loader = new ForceReloadClassLoader();
//		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		//noinspection EmptyFinallyBlock
		try {
			// TODO: Reload of existing classes is broken because reloading basic classes such as 'Event'
			// in a new classloader causes the JVM to no longer see it as the same class, causing signature
			// mismatches.
			// This will become significantly less of an issue when stuff is built into separate JARs, but for
			// now, only hot add/remove will be supported, no hot modify.
//			Thread.currentThread().setContextClassLoader(loader);
			Collection<URL> urls = ClasspathHelper.forJavaClassPath();
			// TODO: make package blacklist a setting
			urls = urls.stream().filter(u -> {
				String jarName = getJarName(u.toString());
				if (jarName == null) {
					return true;
				}
				else {
					return scanBlacklist.stream().noneMatch(jarName::startsWith);
				}
			}).collect(Collectors.toList());
			log.info("URLs: {}", urls);
			// TODO: make this public so that we aren't doing as much re-scanning
			Reflections reflections = new Reflections(
					new ConfigurationBuilder()
//							.setClassLoaders(new ClassLoader[]{loader})
//						.addClassLoaders(new ForceReloadClassLoader(Thread.currentThread().getContextClassLoader()))
							.setUrls(urls)
							.setParallel(true)
//							.forPackages("")
							.setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated, Scanners.SubTypes));
			Set<Method> annotatedMethods = reflections.get(MethodsAnnotated.with(HandleEvents.class).as(Method.class));
			Set<Class<?>> annotatedClasses = reflections.get(Scanners.TypesAnnotated.with(ScanMe.class).asClass());
			log.info("Scan done, setting up topology now");

			Map<Class<?>, List<Method>> classMethodMap = new LinkedHashMap<>();
			for (Class<?> annotatedClass : annotatedClasses) {
				if (isClassInstantiable(annotatedClass)) {
					classMethodMap.computeIfAbsent(annotatedClass, unused -> new ArrayList<>());
				}
				else {
					log.warn("Not adding @ScanMe class {} because it is not instantiable", annotatedClass);
				}

			}
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


			log.info("Preloading instances");
			StringBuilder topo = new StringBuilder();
			// Preload class instances
			classMethodMap.keySet().forEach(instanceProvider::preAdd);
			log.info("Preloaded instances");
			classMethodMap.forEach((clazz, methods) -> {
				// TODO: error handling
				topo.append("Class: ").append(clazz.getSimpleName()).append('\n');
				// TODO: move this to AutoHandler so scope can be implemented
				Object clazzInstance = instanceProvider.getInstance(clazz);
				for (Method method : methods) {
					AutoHandler rawEvh = new AutoHandler(clazz, method, clazzInstance, config);
					out.add(rawEvh);
					topo.append(" - Method: ").append(rawEvh.getTopoLabel()).append('\n');
				}
			});
			if (out.isEmpty()) {
				log.warn("Nothing got auto-scanned!");
			}
			log_topo.info("Topology:\n{}", topo);
			return out;
		}
		finally {
//			Thread.currentThread().setContextClassLoader(oldLoader);
		}


	}


	// Filter out interfaces, abstract classes, and other junk
	private static boolean isClassInstantiable(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isAnonymousClass();
	}

	private static @Nullable String getJarName(String uriStr) {
		Matcher matcher = jarFileName.matcher(uriStr);
		if (matcher.find()) {
			return matcher.group(1);
		}
		else {
			return null;
		}
	}
}
