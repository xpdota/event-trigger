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

//import hudson.security.ACL;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
//import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

///**
// * Delegating whitelist which allows certain calls to be made only when a non-{@link ACL#SYSTEM} user is making them.
// * <p>First there is a list of unrestricted signatures; these can always be run.
// * <p>Then there is a (probably much smaller) list of restricted signatures.
// * These can be run only when the {@linkplain Jenkins#getAuthentication current user} is a real user or even {@linkplain Jenkins#ANONYMOUS}, but not when {@link ACL#SYSTEM}.
// * Restricted methods should be limited to those which actually perform a permissions check, typically using {@link ACL#checkPermission}.
// * Allowing the system pseudo-user to run these would be dangerous, since we do not know “on whose behalf” a script is running, and this “user” is permitted to do anything.
// */
public class AclAwareWhitelist extends Whitelist {

    private final Whitelist unrestricted, restricted;

    /**
     * Creates a delegating whitelist.
     * @param unrestricted a general whitelist; anything permitted by this one will be permitted in any context
     * @param restricted a whitelist of method/constructor calls (field accesses never consulted) for which ACL checks are expected
     */
    public AclAwareWhitelist(Whitelist unrestricted, Whitelist restricted) {
        this.unrestricted = unrestricted;
        if (this.unrestricted instanceof EnumeratingWhitelist) {
            ((EnumeratingWhitelist) this.unrestricted).precache();
        }
        this.restricted = restricted;
    }

    private static boolean authenticated() {
        return true;
//        return !ACL.SYSTEM.equals(Jenkins.getAuthentication());
    }

    @Override public boolean permitsMethod(Method method, Object receiver, Object[] args) {
        return unrestricted.permitsMethod(method, receiver, args) || authenticated() && restricted.permitsMethod(method, receiver, args);
    }

    @Override public boolean permitsConstructor(Constructor<?> constructor, Object[] args) {
        return unrestricted.permitsConstructor(constructor, args) || authenticated() && restricted.permitsConstructor(constructor, args);
    }

    @Override public boolean permitsStaticMethod(Method method, Object[] args) {
        return unrestricted.permitsStaticMethod(method, args) || authenticated() && restricted.permitsStaticMethod(method, args);
    }

    @Override public boolean permitsFieldGet(Field field, Object receiver) {
        return unrestricted.permitsFieldGet(field, receiver);
    }

    @Override public boolean permitsFieldSet(Field field, Object receiver, Object value) {
        return unrestricted.permitsFieldSet(field, receiver, value);
    }

    @Override public boolean permitsStaticFieldGet(Field field) {
        return unrestricted.permitsStaticFieldGet(field);
    }

    @Override public boolean permitsStaticFieldSet(Field field, Object value) {
        return unrestricted.permitsStaticFieldSet(field, value);
    }

}
