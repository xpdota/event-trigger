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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
//import org.kohsuke.accmod.Restricted;
//import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Whitelist based on a static file.
 */
public final class StaticWhitelist extends EnumeratingWhitelist {
	private static final String[] PERMANENTLY_BLACKLISTED_METHODS = {
			"method java.lang.Runtime exit int",
			"method java.lang.Runtime halt int",
	};

	private static final String[] PERMANENTLY_BLACKLISTED_STATIC_METHODS = {
			"staticMethod java.lang.System exit int",
	};

	private static final String[] PERMANENTLY_BLACKLISTED_CONSTRUCTORS = {
			"new org.kohsuke.groovy.sandbox.impl.Checker$SuperConstructorWrapper java.lang.Object[]",
			"new org.kohsuke.groovy.sandbox.impl.Checker$ThisConstructorWrapper java.lang.Object[]"
	};

	final List<MethodSignature> methodSignatures = new ArrayList<>();
	final List<NewSignature> newSignatures = new ArrayList<>();
	final List<MethodSignature> staticMethodSignatures = new ArrayList<>();
	final List<FieldSignature> fieldSignatures = new ArrayList<>();
	final List<FieldSignature> staticFieldSignatures = new ArrayList<>();

	public StaticWhitelist(Reader definition) throws IOException {
		BufferedReader br = new BufferedReader(definition);
		String line;
		while ((line = br.readLine()) != null) {
			line = filter(line);
			if (line != null) {
				add(line);
			}
		}
	}

	public StaticWhitelist(Collection<? extends String> lines) throws IOException {
		for (String line : lines) {
			add(line);
		}
	}

	public StaticWhitelist(String... lines) throws IOException {
		this(asList(lines));
	}

	/**
	 * Filters a line, returning the content that must be processed.
	 *
	 * @param line Line to filter.
	 * @return {@code null} if the like must be skipped or the content to process if not.
	 */
	static @CheckForNull String filter(@NonNull String line) {
		line = line.trim();
		if (line.isEmpty() || line.startsWith("#")) {
			return null;
		}
		return line;
	}

	/**
	 * Returns true if the given method is permanently blacklisted in {@link #PERMANENTLY_BLACKLISTED_METHODS}
	 */
	public static boolean isPermanentlyBlacklistedMethod(@NonNull Method m) {
		String signature = canonicalMethodSig(m);
		return asList(PERMANENTLY_BLACKLISTED_METHODS).contains(signature);
	}

	/**
	 * Returns true if the given method is permanently blacklisted in {@link #PERMANENTLY_BLACKLISTED_STATIC_METHODS}
	 */
	public static boolean isPermanentlyBlacklistedStaticMethod(@NonNull Method m) {
		String signature = canonicalStaticMethodSig(m);
		return asList(PERMANENTLY_BLACKLISTED_STATIC_METHODS).contains(signature);
	}

	/**
	 * Returns true if the given constructor is permanently blacklisted in {@link #PERMANENTLY_BLACKLISTED_CONSTRUCTORS}
	 */
	public static boolean isPermanentlyBlacklistedConstructor(@NonNull Constructor c) {
		String signature = canonicalConstructorSig(c);
		return asList(PERMANENTLY_BLACKLISTED_CONSTRUCTORS).contains(signature);
	}

	/**
	 * Parse a signature line into a {@link Signature}.
	 *
	 * @param line The signature string
	 * @return the equivalent {@link Signature}
	 * @throws IOException if the signature string could not be parsed.
	 */
	public static Signature parse(@NonNull String line) throws IOException {
		String[] toks = line.split(" ");
		switch (toks[0]) {
			case "method":
				if (toks.length < 3) {
					throw new IOException(line);
				}
				return new MethodSignature(toks[1], toks[2], Arrays.copyOfRange(toks, 3, toks.length));
			case "new":
				if (toks.length < 2) {
					throw new IOException(line);
				}
				return new NewSignature(toks[1], Arrays.copyOfRange(toks, 2, toks.length));
			case "staticMethod":
				if (toks.length < 3) {
					throw new IOException(line);
				}
				return new StaticMethodSignature(toks[1], toks[2], Arrays.copyOfRange(toks, 3, toks.length));
			case "field":
				if (toks.length != 3) {
					throw new IOException(line);
				}
				return new FieldSignature(toks[1], toks[2]);
			case "staticField":
				if (toks.length != 3) {
					throw new IOException(line);
				}
				return new StaticFieldSignature(toks[1], toks[2]);
			default:
				throw new IOException(line);
		}
	}

	/**
	 * Checks if the signature is permanently blacklisted, and so shouldn't show up in the pending approval list.
	 *
	 * @param signature the signature to check
	 * @return true if the signature is permanently blacklisted, false otherwise.
	 */
	public static boolean isPermanentlyBlacklisted(String signature) {
		return asList(PERMANENTLY_BLACKLISTED_METHODS).contains(signature)
				|| asList(PERMANENTLY_BLACKLISTED_STATIC_METHODS).contains(signature)
				|| asList(PERMANENTLY_BLACKLISTED_CONSTRUCTORS).contains(signature);
	}

	private void add(String line) throws IOException {
		Signature s = parse(line);
		if (s instanceof StaticMethodSignature) {
			staticMethodSignatures.add((StaticMethodSignature) s);
		}
		else if (s instanceof MethodSignature) {
			methodSignatures.add((MethodSignature) s);
		}
		else if (s instanceof StaticFieldSignature) {
			staticFieldSignatures.add((StaticFieldSignature) s);
		}
		else if (s instanceof FieldSignature) {
			fieldSignatures.add((FieldSignature) s);
		}
		else {
			newSignatures.add((NewSignature) s);
		}
	}

	public static StaticWhitelist from(URL definition) throws IOException {
		try (InputStream is = definition.openStream(); InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			return new StaticWhitelist(isr);
		}
	}

	@Override
	protected List<MethodSignature> methodSignatures() {
		return methodSignatures;
	}

	@Override
	protected List<NewSignature> newSignatures() {
		return newSignatures;
	}

	@Override
	protected List<MethodSignature> staticMethodSignatures() {
		return staticMethodSignatures;
	}

	@Override
	protected List<FieldSignature> fieldSignatures() {
		return fieldSignatures;
	}

	@Override
	protected List<FieldSignature> staticFieldSignatures() {
		return staticFieldSignatures;
	}

	public static RejectedAccessException rejectMethod(@NonNull Method m) {
		assert (m.getModifiers() & Modifier.STATIC) == 0;
		return blacklist(new RejectedAccessException("method", EnumeratingWhitelist.getName(m.getDeclaringClass()) + " " + m.getName() + printArgumentTypes(m.getParameterTypes())));
	}

	public static RejectedAccessException rejectMethod(@NonNull Method m, String info) {
		assert (m.getModifiers() & Modifier.STATIC) == 0;
		return blacklist(new RejectedAccessException("method", EnumeratingWhitelist.getName(m.getDeclaringClass()) + " " + m.getName() + printArgumentTypes(m.getParameterTypes()), info));
	}

	public static RejectedAccessException rejectNew(@NonNull Constructor<?> c) {
		return blacklist(new RejectedAccessException("new", EnumeratingWhitelist.getName(c.getDeclaringClass()) + printArgumentTypes(c.getParameterTypes())));
	}

	public static RejectedAccessException rejectStaticMethod(@NonNull Method m) {
		assert (m.getModifiers() & Modifier.STATIC) != 0;
		return blacklist(new RejectedAccessException("staticMethod", EnumeratingWhitelist.getName(m.getDeclaringClass()) + " " + m.getName() + printArgumentTypes(m.getParameterTypes())));
	}

	public static RejectedAccessException rejectField(@NonNull Field f) {
		assert (f.getModifiers() & Modifier.STATIC) == 0;
		return blacklist(new RejectedAccessException("field", EnumeratingWhitelist.getName(f.getDeclaringClass()) + " " + f.getName()));
	}

	public static RejectedAccessException rejectStaticField(@NonNull Field f) {
		assert (f.getModifiers() & Modifier.STATIC) != 0;
		return blacklist(new RejectedAccessException("staticField", EnumeratingWhitelist.getName(f.getDeclaringClass()) + " " + f.getName()));
	}

	private static String printArgumentTypes(Class<?>[] parameterTypes) {
		StringBuilder b = new StringBuilder();
		for (Class<?> c : parameterTypes) {
			b.append(' ');
			b.append(EnumeratingWhitelist.getName(c));
		}
		return b.toString();
	}

	private static final Set<String> BLACKLIST;

	@SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "https://sourceforge.net/p/findbugs/bugs/786/")
	private static Set<String> loadBlacklist() throws IOException {
		try (InputStream is = StaticWhitelist.class.getResourceAsStream("blacklist"); InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII); BufferedReader br = new BufferedReader(isr)) {
			Set<String> blacklist = new HashSet<>();
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				// TODO could consider trying to load the AccessibleObject, assuming the defining Class is accessible, as a defense against typos
				blacklist.add(line);
			}
			return blacklist;
		}
	}

	static {
		try {
			BLACKLIST = loadBlacklist();
		}
		catch (IOException x) {
			throw new ExceptionInInitializerError(x);
		}
	}

	private static RejectedAccessException blacklist(RejectedAccessException x) {
		if (BLACKLIST.contains(x.getSignature())) {
			x.setDangerous(true);
		}
		return x;
	}

//	@Restricted(NoExternalUse.class) // ScriptApproval
	public static boolean isBlacklisted(String signature) {
		return BLACKLIST.contains(signature);
	}

}
