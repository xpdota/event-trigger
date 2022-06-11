package gg.xp.xivsupport.gui.groovy;

import gg.xp.reevent.scan.ScanMe;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("Convert2streamapi")
@ScanMe
public class GroovyWhitelist extends Whitelist {

	private static final Logger log = LoggerFactory.getLogger(GroovyWhitelist.class);

	private final List<String> pkgBlacklist = List.of(
			"java.io",
			"java.nio",
			"java.awt.Desktop",
			"java.lang.Thread",
			"org.slf4j",
			"java.lang.Process"
	);
	private final List<Class<?>> classBlacklist = List.of(
			Thread.class,
			Process.class,
			ProcessBuilder.class
	);

	private boolean nameAllowed(String thing) {
		String stringifiedThing = stringify(thing);
		for (String s : pkgBlacklist) {
			if (stringifiedThing.startsWith(s)) {
				return false;
			}
		}
		return true;
	}

	private boolean classesAllowed(Class<?>[] classes) {
		for (Class<?> aClass : classes) {
			if (!classAllowed(aClass)) {
				return false;
			}
		}
		return true;
	}

	private boolean classAllowed(Class<?> clazz) {
		for (Class<?> blClass : classBlacklist) {
			if (blClass.isAssignableFrom(clazz)) {
				return false;
			}
		}
		Class<?> decl = clazz.getDeclaringClass();
		// If this is not null, it won't have an FQN, so no need to check
		if (decl != null) {
			return classAllowed(decl);
		}
		return nameAllowed(clazz.getCanonicalName());
	}

	private boolean fieldAllowed(Field field) {
		return classAllowed(field.getType()) && classAllowed(field.getDeclaringClass());
	}

	private boolean methodAllowed(Method method) {
		return classAllowed(method.getReturnType()) && classesAllowed(method.getParameterTypes()) && classAllowed(method.getDeclaringClass());
	}

	private boolean ctorAllowed(Constructor<?> constructor) {
		return classAllowed(constructor.getDeclaringClass()) && classesAllowed(constructor.getParameterTypes());
	}

	private String stringify(Object thing) {
		if (thing instanceof Class clazz) {
			String classFqn = ((Class<?>) thing).getCanonicalName();
			if (classFqn == null) {
				return stringify(clazz.getDeclaringClass().getCanonicalName());
			}
			return classFqn;
		}
		else if (thing instanceof Member member) {
			return stringify(member.getDeclaringClass());
		}
		else {
			return stringify(thing.getClass());
		}
	}

	private boolean objectAllowed(Object object) {
		if (object == null) {
			return false;
		}
		for (Class<?> cls : classBlacklist) {
			if (cls.isInstance(object)) {
				return false;
			}
		}
		return classAllowed(object.getClass());
	}

	private boolean objectsAllowed(Object[] objects) {
		for (Object object : objects) {
			if (!objectAllowed(object)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean permitsMethod(@NotNull Method method, @NotNull Object receiver, @NotNull Object[] args) {
		log.info("{}", method);
		return methodAllowed(method) && objectAllowed(receiver) && objectsAllowed(args);
	}

	@Override
	public boolean permitsConstructor(@NotNull Constructor<?> constructor, @NotNull Object[] args) {
		log.info("{}", constructor);
		return ctorAllowed(constructor) && objectsAllowed(args);
	}

	@Override
	public boolean permitsStaticMethod(@NotNull Method method, @NotNull Object[] args) {
		log.info("{}", method);
		return methodAllowed(method) && objectsAllowed(args);
	}

	@Override
	public boolean permitsFieldGet(@NotNull Field field, @NotNull Object receiver) {
		log.info("{}", field);
		return fieldAllowed(field) && objectAllowed(receiver);
	}

	@Override
	public boolean permitsFieldSet(@NotNull Field field, @NotNull Object receiver, Object value) {
		log.info("{}", field);
		return fieldAllowed(field) && objectAllowed(receiver) && objectAllowed(value);
	}

	@Override
	public boolean permitsStaticFieldGet(@NotNull Field field) {
		log.info("{}", field);
		try {
			return fieldAllowed(field) && objectAllowed(field.get(null));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean permitsStaticFieldSet(@NotNull Field field, Object value) {
		log.info("{}", field);
		return fieldAllowed(field) && objectAllowed(value);
	}
}
