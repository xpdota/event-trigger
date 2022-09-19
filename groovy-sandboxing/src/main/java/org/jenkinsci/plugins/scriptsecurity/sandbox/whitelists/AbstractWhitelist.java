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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

/**
 * Convenience whitelist base class that denies everything by default.
 * Thus you need only override things you wish to explicitly allow.
 * Also reduces the risk of incompatibilities in case further {@code abstract} methods are added to {@link Whitelist}.
 */
public abstract class AbstractWhitelist extends Whitelist {

    @Override public boolean permitsMethod(Method method, Object receiver, Object[] args) {
        return false;
    }

    @Override public boolean permitsConstructor(Constructor<?> constructor, Object[] args) {
        return false;
    }

    @Override public boolean permitsStaticMethod(Method method, Object[] args) {
        return false;
    }

    @Override public boolean permitsFieldSet(Field field, Object receiver, Object value) {
        return false;
    }

    @Override public boolean permitsFieldGet(Field field, Object receiver) {
        return false;
    }

    @Override public boolean permitsStaticFieldSet(Field field, Object value) {
        return false;
    }

    @Override public boolean permitsStaticFieldGet(Field field) {
        return false;
    }

}
