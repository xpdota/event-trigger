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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.SubTypes;

public class AutoScan {

	private static final Logger log = LoggerFactory.getLogger(AutoScan.class);
	private final AutoHandlerInstanceProvider instanceProvider;
	private final AutoHandlerConfig config;
	private static final Pattern jarFileName = Pattern.compile("([a-zA-Z0-9\\-.]+)\\.jar");
	private static final List<String> scanBlacklist = List.of(
			"annotations",
			"caffeine",
			"commons",
			"flatlaf",
			"groovy",
			"http",
			"jackson",
			"Java-WebSocket",
			"javaassist",
			"jna",
			"jsr",
			"nonexistent",
			"logback",
			"opencsv",
			"picocontainer",
			"reflections",
			"rsyntaxtextarea",
			"sfl4j",
			"xivdata"
	);
	private boolean scanned;

	public AutoScan(AutoHandlerInstanceProvider instanceProvider, AutoHandlerConfig config) {
		this.instanceProvider = instanceProvider;
		this.config = config;
	}

	private List<URL> findAddonJars() {
		return config.getAddonJars();
	}

	public void doScanIfNeeded() {
		if (!scanned) {
			doScan();
		}
	}

	public void doScan() {
		log.info("Scanning packages");
		List<AutoHandler> out = new ArrayList<>();
//		ClassLoader loader = new ForceReloadClassLoader();
//		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		//noinspection EmptyFinallyBlock
		// TODO: Reload of existing classes is broken because reloading basic classes such as 'Event'
		// in a new classloader causes the JVM to no longer see it as the same class, causing signature
		// mismatches.
		// This will become significantly less of an issue when stuff is built into separate JARs, but for
		// now, only hot add/remove will be supported, no hot modify.
//			Thread.currentThread().setContextClassLoader(loader);
		Collection<URL> urls = ClasspathHelper.forJavaClassPath();
		// TODO: make package blacklist a setting
		List<URL> addonUrls = findAddonJars();
		urls = Stream.concat(urls.stream(), addonUrls.stream()).filter(u -> {
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
		URLClassLoader newClassLoader = new URLClassLoader(addonUrls.toArray(new URL[]{}));
		ClassLoader[] loaders = {Thread.currentThread().getContextClassLoader(), newClassLoader};
		final Set<Method> annotatedMethods = ConcurrentHashMap.newKeySet();
		final Set<Class<?>> annotatedClasses = ConcurrentHashMap.newKeySet();
		final Map<String, Throwable> failedModules = new ConcurrentHashMap<>();
		// TODO: make these changes in the Groovy side too
		urls.parallelStream().forEach(url -> {
			log.info("URL: '{}'", url);
			Reflections reflections = new Reflections(
					new ConfigurationBuilder()
							.setUrls(Collections.singletonList(url))
							.setParallel(true)
							.setScanners(Scanners.TypesAnnotated, MethodsAnnotated, SubTypes));
			try {
				annotatedMethods.addAll(reflections.get(MethodsAnnotated.with(HandleEvents.class).as(Method.class, loaders)));
				annotatedClasses.addAll(reflections.get(Scanners.TypesAnnotated.with(ScanMe.class).asClass(loaders)));
			}
			catch (Throwable t) {
				String jarName = getJarName(url.toString());
				failedModules.put(jarName, new RuntimeException("Module " + jarName + " failed to load: " + t, t));
			}
		});
		if (!failedModules.isEmpty()) {
			StringBuilder sb = new StringBuilder("One or more modules failed to load. If this problem persists, try deleting them: \n");
			failedModules.keySet().forEach(jarName -> sb.append("  - ").append(jarName));
			RuntimeException combined = new RuntimeException(sb.toString());
			failedModules.values().forEach(combined::addSuppressed);
			throw combined;
		}
		log.info("Scan done, setting up topology now");
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.setUrls(urls)
						.setParallel(true)
						.setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated, Scanners.SubTypes));

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
			Set<Class<?>> implementingClasses = reflections.get(SubTypes.of(clazz).asClass(loaders));
			if (!implementingClasses.isEmpty()) {
				log.info("Class {} has implementors: {}", clazz, implementingClasses);
			}
			//noinspection SimplifyForEach
			Stream.concat(Stream.of(clazz), implementingClasses.stream())
					.filter(AutoScan::isClassInstantiable)
					.forEach(cls -> classMethodMap.computeIfAbsent(cls, unused -> new ArrayList<>()).add(method));
		}
		log.info("Methods: {}", methodCount);


		log.info("Loading instances");
		// Preload class instances
		classMethodMap.keySet().forEach(instanceProvider::preAdd);
		classMethodMap.keySet().forEach(instanceProvider::getInstance);
		log.info("Loaded instances");
		scanned = true;
	}


	// Filter out interfaces, abstract classes, and other junk
	private static boolean isClassInstantiable(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isAnonymousClass() && (clazz.getDeclaringClass() == null);
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
