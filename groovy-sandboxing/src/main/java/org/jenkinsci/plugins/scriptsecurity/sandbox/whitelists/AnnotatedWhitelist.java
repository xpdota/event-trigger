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

//import hudson.Extension;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
//import org.kohsuke.accmod.Restricted;
//import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Whitelists anything marked with {@link Whitelisted}.
 */
//@Restricted(NoExternalUse.class)
//@Extension
public final class AnnotatedWhitelist extends AclAwareWhitelist {

	public AnnotatedWhitelist() {
		super(new Impl(false), new Impl(true));
	}

	private static final class Impl extends Whitelist {

		private final boolean restricted;

		Impl(boolean restricted) {
			this.restricted = restricted;
		}

		private boolean allowed(@NonNull AccessibleObject o) {
			Whitelisted ann = o.getAnnotation(Whitelisted.class);
			if (ann == null) {
				return false;
			}
			return ann.restricted() == restricted;
		}

		@Override
		public boolean permitsMethod(Method method, Object receiver, Object[] args) {
			return allowed(method);
		}

		@Override
		public boolean permitsConstructor(Constructor<?> constructor, Object[] args) {
			return allowed(constructor);
		}

		@Override
		public boolean permitsStaticMethod(Method method, Object[] args) {
			return allowed(method);
		}

		@Override
		public boolean permitsFieldGet(Field field, Object receiver) {
			return allowed(field);
		}

		@Override
		public boolean permitsFieldSet(Field field, Object receiver, Object value) {
			return allowed(field);
		}

		@Override
		public boolean permitsStaticFieldGet(Field field) {
			return allowed(field);
		}

		@Override
		public boolean permitsStaticFieldSet(Field field, Object value) {
			return allowed(field);
		}

	}

}
