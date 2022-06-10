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

package org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A whitelist based on listing signatures and searching them. Lists of signatures should not change
 * from invocation to invocation.
 *
 * If that's a need it is better to directly extend {@link Whitelist} and roll a custom implementation OR
 *  extend ProxyWhitelist and add some custom delegates.
 */
public abstract class EnumeratingWhitelist extends Whitelist {

    protected abstract List<MethodSignature> methodSignatures();

    protected abstract List<NewSignature> newSignatures();

    protected abstract List<MethodSignature> staticMethodSignatures();

    protected abstract List<FieldSignature> fieldSignatures();

    protected abstract List<FieldSignature> staticFieldSignatures();

    ConcurrentHashMap<String, Boolean> permittedCache = new ConcurrentHashMap<>();  // Not private to facilitate testing

    @SafeVarargs
    private final void cacheSignatureList(List<Signature> ...sigs) {
        for (List<Signature> list : sigs) {
            for (Signature s : list) {
                if (!s.isWildcard()) { // Cache entries for wildcard signatures will never be accessed and just waste space
                    permittedCache.put(s.toString(), Boolean.TRUE);
                }
            }
        }
    }

    /** Prepopulates the "permitted" cache, resetting if populated already.  Should be called when method signatures change or after initialization. */
    final void precache() {
        if (!permittedCache.isEmpty()) {
            this.permittedCache.clear();  // No sense calling clearCache
        }
        cacheSignatureList((List)methodSignatures(), (List)(newSignatures()),
                           (List)(staticMethodSignatures()), (List)(fieldSignatures()),
                           (List)(staticFieldSignatures()));
    }

    /** Frees up nearly all memory used for the cache.  MUST BE CALLED if you change the result of the xxSignatures() methods. */
    final void clearCache() {
        this.permittedCache.clear();
        this.permittedCache = new ConcurrentHashMap<>();
    }

    @Override public final boolean permitsMethod(Method method, Object receiver, Object[] args) {
        String key = canonicalMethodSig(method);
        Boolean b = permittedCache.get(key);
        if (b != null) {
            return b;
        }

        boolean output = false;
        for (MethodSignature s : methodSignatures()) {
            if (s.matches(method)) {
                output = true;
                break;
            }
        }
        permittedCache.put(key, output);
        return output;
    }

    @Override public final boolean permitsConstructor(Constructor<?> constructor, Object[] args) {
        String key = canonicalConstructorSig(constructor);
        Boolean b = permittedCache.get(key);
        if (b != null) {
            return b;
        }

        boolean output = false;
        for (NewSignature s : newSignatures()) {
            if (s.matches(constructor)) {
                output = true;
                break;
            }
        }
        permittedCache.put(key, output);
        return output;
    }

    @Override public final boolean permitsStaticMethod(Method method, Object[] args) {
        String key = canonicalStaticMethodSig(method);
        Boolean b = permittedCache.get(key);
        if (b != null) {
            return b;
        }

        boolean output = false;
        for (MethodSignature s : staticMethodSignatures()) {
            if (s.matches(method)) {
                output = true;
                break;
            }
        }
        permittedCache.put(key, output);
        return output;
    }

    @Override public final boolean permitsFieldGet(Field field, Object receiver) {
        String key = canonicalFieldSig(field);
        Boolean b = permittedCache.get(key);
        if (b != null) {
            return b;
        }

        boolean output = false;
        for (FieldSignature s : fieldSignatures()) {
            if (s.matches(field)) {
                output = true;
                break;
            }
        }
        permittedCache.put(key, output);
        return output;
    }

    @Override public final boolean permitsFieldSet(Field field, Object receiver, Object value) {
        return permitsFieldGet(field, receiver);
    }

    @Override public final boolean permitsStaticFieldGet(Field field) {
        String key = canonicalStaticFieldSig(field);
        Boolean b = permittedCache.get(key);
        if (b != null) {
            return b;
        }

        boolean output = false;
        for (FieldSignature s : staticFieldSignatures()) {
            if (s.matches(field)) {
                output = true;
                break;
            }
        }
        permittedCache.put(key, output);
        return output;
    }

    @Override public final boolean permitsStaticFieldSet(Field field, Object value) {
        return permitsStaticFieldGet(field);
    }

    public static @NonNull String getName(@NonNull Class<?> c) {
        Class<?> e = c.getComponentType();
        if (e == null) {
            return c.getName();
        } else {
            return getName(e) + "[]";
        }
    }

    public static @NonNull String getName(@CheckForNull Object o) {
        return o == null ? "null" : getName(o.getClass());
    }

    private static boolean is(String thisIdentifier, String identifier) {
        return thisIdentifier.equals("*") || identifier.equals(thisIdentifier);
    }

    public static abstract class Signature implements Comparable<Signature> {
        /** Form as in {@link StaticWhitelist} entries. */
        @Override public abstract String toString();

        abstract String signaturePart();
        @Override public int compareTo(Signature o) {
            int r = signaturePart().compareTo(o.signaturePart());
            return r != 0 ? r : toString().compareTo(o.toString());
        }
        @Override public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass() && toString().equals(obj.toString());
        }
        @Override public int hashCode() {
            return toString().hashCode();
        }
        abstract boolean exists() throws Exception;
        /** opposite of {@link #getName(Class)} */
        static final Class<?> type(String name) throws Exception {
            // ClassUtils.getClass is too lax: permits Outer.Inner where we require Outer$Inner.
            if (name.endsWith("[]")) {
                // https://stackoverflow.com/q/1679421/12916; TODO Java 12+ use Class.arrayType
                return Array.newInstance(type(name.substring(0, name.length() - 2)), 0).getClass();
            }
            switch (name) {
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                return Class.forName(name);
            }
        }
        final Class<?>[] types(String[] names) throws Exception {
            Class<?>[] r = new Class<?>[names.length];
            for (int i = 0; i < names.length; i++) {
                r[i] = type(names[i]);
            }
            return r;
        }

        public boolean isWildcard() {
            return false;
        }
    }

    // Utility methods for creating canonical string representations of the signature
    static final StringBuilder joinWithSpaces(StringBuilder b, String[] types) {
        for (String type : types) {
            b.append(' ').append(type);
        }
        return b;
    }

    static String[] argumentTypes(Class<?>[] argumentTypes) {
        String[] s = new String[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) {
            s[i] = getName(argumentTypes[i]);
        }
        return s;
    }

    /** Canonical name for a field access. */
    static String canonicalFieldString(@NonNull Field field) {
        return getName(field.getDeclaringClass()) + ' ' + field.getName();
    }

    /** Canonical name for a method call. */
    static String canonicalMethodString(@NonNull Method method) {
        return joinWithSpaces(new StringBuilder(getName(method.getDeclaringClass())).append(' ').append(method.getName()), argumentTypes(method.getParameterTypes())).toString();
    }

    /** Canonical name for a constructor call. */
    static String canonicalConstructorString(@NonNull Constructor cons) {
        return joinWithSpaces(new StringBuilder(getName(cons.getDeclaringClass())), argumentTypes(cons.getParameterTypes())).toString();
    }

    static String canonicalMethodSig(@NonNull Method method) {
        return "method "+canonicalMethodString(method);
    }

    static String canonicalStaticMethodSig(@NonNull Method method) {
        return "staticMethod "+canonicalMethodString(method);
    }

    static String canonicalConstructorSig(@NonNull Constructor cons) {
        return "new "+canonicalConstructorString(cons);
    }

    static String canonicalFieldSig(@NonNull Field field) {
        return "field "+canonicalFieldString(field);
    }

    static String canonicalStaticFieldSig(@NonNull Field field) {
        return "staticField "+canonicalFieldString(field);
    }

    public static class MethodSignature extends Signature {
        final String receiverType, method;
        final String[] argumentTypes;
        public MethodSignature(String receiverType, String method, String... argumentTypes) {
            this.receiverType = receiverType;
            this.method = method;
            this.argumentTypes = argumentTypes.clone();
        }
        public MethodSignature(Class<?> receiverType, String method, Class<?>... argumentTypes) {
            this(getName(receiverType), method, argumentTypes(argumentTypes));
        }
        boolean matches(Method m) {
            return is(method, m.getName()) && getName(m.getDeclaringClass()).equals(receiverType) && Arrays.equals(argumentTypes(m.getParameterTypes()), argumentTypes);
        }
        @Override public String toString() {
            return "method " + signaturePart();
        }
        @Override String signaturePart() {
            return joinWithSpaces(new StringBuilder(receiverType).append(' ').append(method), argumentTypes).toString();
        }
        @Override boolean exists() throws Exception {
            return exists(type(receiverType), true);
        }
        // Cf. GroovyCallSiteSelector.visitTypes.
        @SuppressWarnings("InfiniteRecursion")
        private boolean exists(Class<?> c, boolean start) throws Exception {
            Class<?> s = c.getSuperclass();
            if (s != null && exists(s, false)) {
                return !start;
            }
            for (Class<?> i : c.getInterfaces()) {
                if (exists(i, false)) {
                    return !start;
                }
            }
            try {
                return !Modifier.isStatic(c.getDeclaredMethod(method, types(argumentTypes)).getModifiers());
            } catch (NoSuchMethodException x) {
                return false;
            }
        }

        @Override
        public boolean isWildcard() {
            return "*".equals(method);
        }
    }

    static class StaticMethodSignature extends MethodSignature {
        StaticMethodSignature(String receiverType, String method, String... argumentTypes) {
            super(receiverType, method, argumentTypes);
        }
        @Override public String toString() {
            return "staticMethod " + signaturePart();
        }
        @Override boolean exists() throws Exception {
            try {
                return Modifier.isStatic(type(receiverType).getDeclaredMethod(method, types(argumentTypes)).getModifiers());
            } catch (NoSuchMethodException x) {
                return false;
            }
        }
    }

    public static final class NewSignature extends Signature  {
        private final String type;
        private final String[] argumentTypes;
        public NewSignature(String type, String[] argumentTypes) {
            this.type = type;
            this.argumentTypes = argumentTypes.clone();
        }
        public NewSignature(Class<?> type, Class<?>... argumentTypes) {
            this(getName(type), argumentTypes(argumentTypes));
        }
        boolean matches(Constructor c) {
            return getName(c.getDeclaringClass()).equals(type) && Arrays.equals(argumentTypes(c.getParameterTypes()), argumentTypes);
        }
        @Override String signaturePart() {
            return joinWithSpaces(new StringBuilder(type), argumentTypes).toString();
        }
        @Override public String toString() {
            return "new " + signaturePart();
        }
        @Override boolean exists() throws Exception {
            try {
                type(type).getDeclaredConstructor(types(argumentTypes));
                return true;
            } catch (NoSuchMethodException x) {
                return false;
            }
        }
    }

    public static class FieldSignature extends Signature {
        final String type, field;
        public FieldSignature(String type, String field) {
            this.type = type;
            this.field = field;
        }
        public FieldSignature(Class<?> type, String field) {
            this(getName(type), field);
        }
        boolean matches(Field f) {
            return is(field, f.getName()) && getName(f.getDeclaringClass()).equals(type);
        }
        @Override String signaturePart() {
            return type + ' ' + field;
        }
        @Override public String toString() {
            return "field " + signaturePart();
        }
        @Override boolean exists() throws Exception {
            try {
                type(type).getField(field);
                return true;
            } catch (NoSuchFieldException x) {
                return false;
            }
        }

        @Override
        public boolean isWildcard() {
            return "*".equals(field);
        }
    }

    static class StaticFieldSignature extends FieldSignature {
        StaticFieldSignature(String type, String field) {
            super(type, field);
        }
        @Override public String toString() {
            return "staticField " + signaturePart();
        }
    }

}
