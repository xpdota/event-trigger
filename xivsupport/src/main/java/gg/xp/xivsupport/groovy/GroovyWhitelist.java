package gg.xp.xivsupport.groovy;

import gg.xp.reevent.scan.ScanMe;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.awt.*;
import java.io.File;
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
			"org.apache.commons.io",
			"java.lang.Process",
			"sun.misc.Unsafe"
	);
	private final List<Class<?>> classBlacklist = List.of(
			Thread.class,
			Process.class,
			ProcessBuilder.class,
			File.class,
			FileUtils.class,
			IOUtils.class,
			Unsafe.class,
			Desktop.class,
			GroovyShell.class,
			GroovyClassLoader.class
	);

	private boolean nameAllowed(String thing) {
		if (thing == null) {
			return true;
		}
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

	private boolean receiverObjectAllowed(Object object) {
		// This is fine because a receiver should never be null. That doesn't make sense, and it's a bit suspicious
		// if it gets to this point, so be safe.
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

	private boolean generalObjectAllowed(Object object) {
		if (object == null) {
			// General objects such as arguments and field values may be null
			return true;
		}
		for (Class<?> cls : classBlacklist) {
			if (cls.isInstance(object)) {
				return false;
			}
		}
		return classAllowed(object.getClass());
	}

	private boolean generalObjectsAllowed(Object[] objects) {
		for (Object object : objects) {
			if (!generalObjectAllowed(object)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean permitsMethod(@NotNull Method method, @NotNull Object receiver, @NotNull Object[] args) {
		log.trace("{}", method);
		return methodAllowed(method) && receiverObjectAllowed(receiver) && generalObjectsAllowed(args);
	}

	@Override
	public boolean permitsConstructor(@NotNull Constructor<?> constructor, @NotNull Object[] args) {
		log.trace("{}", constructor);
		return ctorAllowed(constructor) && generalObjectsAllowed(args);
	}

	@Override
	public boolean permitsStaticMethod(@NotNull Method method, @NotNull Object[] args) {
		log.trace("{}", method);
		return methodAllowed(method) && generalObjectsAllowed(args);
	}

	@Override
	public boolean permitsFieldGet(@NotNull Field field, @NotNull Object receiver) {
		log.trace("{}", field);
		return fieldAllowed(field) && receiverObjectAllowed(receiver);
	}

	@Override
	public boolean permitsFieldSet(@NotNull Field field, @NotNull Object receiver, Object value) {
		log.trace("{}", field);
		return fieldAllowed(field) && receiverObjectAllowed(receiver) && generalObjectAllowed(value);
	}

	@Override
	public boolean permitsStaticFieldGet(@NotNull Field field) {
		log.trace("{}", field);
		try {
			return fieldAllowed(field) && generalObjectAllowed(field.get(null));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean permitsStaticFieldSet(@NotNull Field field, Object value) {
		log.trace("{}", field);
		return fieldAllowed(field) && generalObjectAllowed(value);
	}
}
