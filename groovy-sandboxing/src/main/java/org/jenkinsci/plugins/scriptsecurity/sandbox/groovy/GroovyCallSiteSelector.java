/*
 * The MIT License
 *
 * Copyright 2014 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

import groovy.lang.GString;
import groovy.lang.GroovyInterceptable;
import org.apache.commons.lang3.ClassUtils;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Assists in determination of which method or other JVM element is actually about to be called by Groovy.
 * Most of this just duplicates what {@link java.lang.invoke.MethodHandles.Lookup} and {@link java.lang.invoke.MethodHandle#asType} do,
 * but {@link org.codehaus.groovy.vmplugin.v7.TypeTransformers} shows that there are Groovy-specific complications.
 * Comments in https://github.com/kohsuke/groovy-sandbox/issues/7 note that it would be great for the sandbox itself to just tell us what the call site is so we would not have to guess.
 */
class GroovyCallSiteSelector {

	private static boolean matches(@NotNull Class<?>[] parameterTypes, @NotNull Object[] parameters, boolean varargs) {
		if (varargs) {
			parameters = parametersForVarargs(parameterTypes, parameters);
		}
		if (parameters.length != parameterTypes.length) {
			return false;
		}
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> thisParamType = parameterTypes[i];
			Object thisParam = parameters[i];
			if (thisParam == null) {
				if (thisParamType.isPrimitive()) {
					return false;
				}
				else {
					// A null argument is assignable to any reference-typed parameter.
					continue;
				}
			}
			if (thisParamType.isInstance(thisParam)) {
				// OK, this parameter matches.
				continue;
			}
			if (
					thisParamType.isPrimitive()
					&& isInstancePrimitive(ClassUtils.primitiveToWrapper(thisParamType), thisParam)
			) {
				// Groovy passes primitive values as objects (for example, passes 0 as Integer(0))
				// The prior test fails as int.class.isInstance(new Integer(0)) returns false.
				continue;
			}
			// TODO what about a primitive parameter type and a wrapped parameter?
			if (thisParamType == String.class && thisParam instanceof GString) {
				// Cf. SandboxInterceptorTest and class Javadoc.
				continue;
			}
			if ((thisParamType == Double.class || thisParamType == double.class || thisParamType == Float.class || thisParamType == float.class) && thisParam instanceof BigDecimal) {
				continue;
			}
			// Mismatch.
			return false;
		}
		return true;
	}

	/**
	 * Translates a method parameter list with varargs possibly spliced into the end into the actual parameters to be passed to the JVM call.
	 */
	private static Object[] parametersForVarargs(Class<?>[] parameterTypes, Object[] parameters) {
		int fixedLen = parameterTypes.length - 1;
		Class<?> componentType = parameterTypes[fixedLen].getComponentType();
		assert componentType != null;
		if (componentType.isPrimitive()) {
			componentType = ClassUtils.primitiveToWrapper(componentType);
		}
		int arrayLength = parameters.length - fixedLen;

		if (arrayLength >= 0) {
			if (arrayLength == 1 && parameterTypes[fixedLen].isInstance(parameters[fixedLen])) {
				// not a varargs call
				return parameters;
			}
			else if ((arrayLength > 0 && (componentType.isInstance(parameters[fixedLen]) || parameters[fixedLen] == null)) ||
					arrayLength == 0) {
				Object array = DefaultTypeTransformation.castToVargsArray(parameters, fixedLen, parameterTypes[fixedLen]);
				Object[] parameters2 = new Object[fixedLen + 1];
				System.arraycopy(parameters, 0, parameters2, 0, fixedLen);
				parameters2[fixedLen] = array;

				return parameters2;
			}
			// TODO: other types?
			else if (componentType == Long.class && parameters[fixedLen] instanceof Integer) {
				Object array = DefaultTypeTransformation.castToVargsArray(parameters, fixedLen, parameterTypes[fixedLen]);
				Object[] parameters2 = new Object[fixedLen + 1];
				System.arraycopy(parameters, 0, parameters2, 0, fixedLen);
				parameters2[fixedLen] = array;
				return parameters2;
			}
		}
		return parameters;
	}

	/**
	 * {@link Class#isInstance} extended to handle some important cases of primitive types.
	 */
	private static boolean isInstancePrimitive(@NotNull Class<?> type, @NotNull Object instance) {
		if (type.isInstance(instance)) {
			return true;
		}
		// https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2
		if (instance instanceof Number) {
			if (type == Long.class && instance instanceof Integer) {
				return true; // widening
			}
			if (type == Integer.class && instance instanceof Long) {
				Long n = (Long) instance;
				if (n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
					return true; // safe narrowing
				}
			}
			// TODO etc. for other conversions if they ever come up
		}
		return false;
	}

	/**
	 * Looks up the most general possible definition of a given method call.
	 * Preferentially searches for compatible definitions in supertypes.
	 *
	 * @param receiver an actual receiver object
	 * @param method   the method name
	 * @param args     a set of actual arguments
	 */
	public static @Nullable Method method(@NotNull Object receiver, @NotNull String method, @NotNull Object[] args) {
		Set<Class<?>> types = types(receiver);
		if (types.contains(GroovyInterceptable.class) && !"invokeMethod".equals(method)) {
			return method(receiver, "invokeMethod", new Object[]{method, args});
		}
		for (Class<?> c : types) {
			Method candidate = findMatchingMethod(c, method, args);
			if (candidate != null) {
				return candidate;
			}
		}
		if (receiver instanceof GString) { // cf. GString.invokeMethod
			Method candidate = findMatchingMethod(String.class, method, args);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}

	public static @Nullable Constructor<?> constructor(@NotNull Class<?> receiver, @NotNull Object[] args) {
		Constructor<?>[] constructors = receiver.getDeclaredConstructors();
		Constructor<?> candidate = null;
		for (Constructor<?> c : constructors) {
			if (matches(c.getParameterTypes(), args, c.isVarArgs())) {
				if (candidate == null || isMoreSpecific(c, c.getParameterTypes(), c.isVarArgs(), candidate, candidate.getParameterTypes(), candidate.isVarArgs())) {
					candidate = c;
				}
			}
		}
		if (candidate != null) {
			return candidate;
		}

		// Only check for the magic Map constructor if we haven't already found a real constructor.
		// Also note that this logic is derived from how Groovy itself decides to use the magic Map constructor, at
		// MetaClassImpl#invokeConstructor(Class, Object[]).
		if (args.length == 1 && args[0] instanceof Map) {
			for (Constructor<?> c : constructors) {
				if (c.getParameterTypes().length == 0 && !c.isVarArgs()) {
					return c;
				}
			}
		}

		return null;
	}

	public static @Nullable Method staticMethod(@NotNull Class<?> receiver, @NotNull String method, @NotNull Object[] args) {
		return findMatchingMethod(receiver, method, args);
	}

	private static Method findMatchingMethod(@NotNull Class<?> receiver, @NotNull String method, @NotNull Object[] args) {
		Method candidate = null;

		for (Method m : receiver.getDeclaredMethods()) {
			if (m != null) {
				boolean isVarArgs = isVarArgsMethod(m, args);
				if (m.getName().equals(method) && (matches(m.getParameterTypes(), args, isVarArgs))) {
					if (candidate == null || isMoreSpecific(m, m.getParameterTypes(), isVarArgs, candidate,
							candidate.getParameterTypes(), isVarArgsMethod(candidate, args))) {
						candidate = m;
					}
				}
			}
		}
		return candidate;
	}

	/**
	 * Emulates, with some tweaks, {@link org.codehaus.groovy.reflection.ParameterTypes#isVargsMethod(Object[])}
	 */
	private static boolean isVarArgsMethod(@NotNull Method m, @NotNull Object[] args) {
		if (m.isVarArgs()) {
			return true;
		}
		Class<?>[] paramTypes = m.getParameterTypes();

		// If there's 0 or only 1 parameter type, we don't want to do varargs magic. Normal callsite selector logic works then.
		if (paramTypes.length < 2) {
			return false;
		}

		int lastIndex = paramTypes.length - 1;
		// If there are more arguments than parameter types and the last parameter type is an array, we may be vargy.
		if (paramTypes[lastIndex].isArray() && args.length > paramTypes.length) {
			Class<?> lastClass = paramTypes[lastIndex].getComponentType();
			// Check each possible vararg to see if it can be cast to the array's component type or is null. If not, we're not vargy.
			for (int i = lastIndex; i < args.length; i++) {
				if (args[i] != null && !lastClass.isAssignableFrom(args[i].getClass())) {
					return false;
				}
			}
			// Otherwise, we are.
			return true;
		}

		// Fallback to we're not vargy.
		return false;
	}

	public static @Nullable Field field(@NotNull Object receiver, @NotNull String field) {
		for (Class<?> c : types(receiver)) {
			for (Field f : c.getDeclaredFields()) {
				if (f.getName().equals(field)) {
					return f;
				}
			}
		}
		return null;
	}

	public static @Nullable Field staticField(@NotNull Class<?> receiver, @NotNull String field) {
		for (Field f : receiver.getDeclaredFields()) {
			if (f.getName().equals(field)) {
				return f;
			}
		}
		return null;
	}

	private static Set<Class<?>> types(@NotNull Object o) {
		Set<Class<?>> types = new LinkedHashSet<>();
		visitTypes(types, o.getClass());
		return types;
	}

	private static void visitTypes(@NotNull Set<Class<?>> types, @NotNull Class<?> c) {
		Class<?> s = c.getSuperclass();
		if (s != null) {
			visitTypes(types, s);
		}
		for (Class<?> i : c.getInterfaces()) {
			visitTypes(types, i);
		}
		// Visit supertypes first.
		types.add(c);
	}

	// TODO nowhere close to implementing http://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2.5
	private static boolean isMoreSpecific(AccessibleObject more, Class<?>[] moreParams, boolean moreVarArgs, AccessibleObject less, Class<?>[] lessParams, boolean lessVarArgs) { // TODO clumsy arguments pending Executable in Java 8
		if (lessVarArgs && !moreVarArgs) {
			return true; // main() vs. main(String...) on []
		}
		else if (!lessVarArgs && moreVarArgs) {
			return false;
		}
		// TODO what about passing [arg] to log(String...) vs. log(String, String...)?
		if (moreParams.length != lessParams.length) {
			throw new IllegalStateException("cannot compare " + more + " to " + less);
		}
		for (int i = 0; i < moreParams.length; i++) {
			Class<?> moreParam = wrap(moreParams[i]);
			Class<?> lessParam = wrap(lessParams[i]);
			if (moreParam.isAssignableFrom(lessParam)) {
				return false;
			}
			else if (lessParam.isAssignableFrom(moreParam)) {
				return true;
			}
			if (moreParam == Long.class && lessParam == Integer.class) {
				return false;
			}
			else if (moreParam == Integer.class && lessParam == Long.class) {
				return true;
			}
		}
		// Incomparable. Arbitrarily pick one of them.
		return more.toString().compareTo(less.toString()) > 0;
	}

	private GroovyCallSiteSelector() {
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
	private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

	private static void add(Map<Class<?>, Class<?>> forward, Map<Class<?>, Class<?>> backward, Class<?> key, Class<?> value) {
		forward.put(key, value);
		backward.put(value, key);
	}

	public static Set<Class<?>> allPrimitiveTypes() {
		return PRIMITIVE_TO_WRAPPER_TYPE.keySet();
	}

	public static Set<Class<?>> allWrapperTypes() {
		return WRAPPER_TO_PRIMITIVE_TYPE.keySet();
	}

	public static boolean isWrapperType(Class<?> type) {
		return WRAPPER_TO_PRIMITIVE_TYPE.containsKey((type));
	}

	public static <T> Class<T> wrap(Class<T> type) {
		Class<T> wrapped = (Class) PRIMITIVE_TO_WRAPPER_TYPE.get(type);
		return wrapped == null ? type : wrapped;
	}

	public static <T> Class<T> unwrap(Class<T> type) {
		Class<T> unwrapped = (Class) WRAPPER_TO_PRIMITIVE_TYPE.get(type);
		return unwrapped == null ? type : unwrapped;
	}

	static {
		Map<Class<?>, Class<?>> primToWrap = new LinkedHashMap(16);
		Map<Class<?>, Class<?>> wrapToPrim = new LinkedHashMap(16);
		add(primToWrap, wrapToPrim, Boolean.TYPE, Boolean.class);
		add(primToWrap, wrapToPrim, Byte.TYPE, Byte.class);
		add(primToWrap, wrapToPrim, Character.TYPE, Character.class);
		add(primToWrap, wrapToPrim, Double.TYPE, Double.class);
		add(primToWrap, wrapToPrim, Float.TYPE, Float.class);
		add(primToWrap, wrapToPrim, Integer.TYPE, Integer.class);
		add(primToWrap, wrapToPrim, Long.TYPE, Long.class);
		add(primToWrap, wrapToPrim, Short.TYPE, Short.class);
		add(primToWrap, wrapToPrim, Void.TYPE, Void.class);
		PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
		WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(wrapToPrim);
	}

}
