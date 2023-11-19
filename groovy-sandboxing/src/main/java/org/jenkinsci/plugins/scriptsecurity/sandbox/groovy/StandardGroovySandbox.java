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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import groovy.grape.GrabAnnotationTransformation;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Allows Groovy scripts (including Groovy Templates) to be run inside a sandbox.
 */
public class StandardGroovySandbox implements GroovySandbox {

	public static final Logger LOGGER = Logger.getLogger(StandardGroovySandbox.class.getName());

	private @Nullable Whitelist whitelist;

	/**
	 * Creates a sandbox with default settings.
	 */
	public StandardGroovySandbox() {
	}

	/**
	 * Specify a whitelist.
	 * By default {@link Whitelist#all} is used.
	 *
	 * @return {@code this}
	 */
	public StandardGroovySandbox withWhitelist(@Nullable Whitelist whitelist) {
		this.whitelist = whitelist;
		return this;
	}



	private @NotNull Whitelist whitelist() {
		return whitelist != null ? whitelist : Whitelist.all();
	}

	/**
	 * Starts a dynamic scope within which calls will be sandboxed.
	 *
	 * @return a scope object, useful for putting this into a {@code try}-with-resources block
	 */
	@Override
	public SandboxScope enter() {
		GroovyInterceptor sandbox = new SandboxInterceptor(whitelist());
		sandbox.register();
		return sandbox::unregister;
	}

	/**
	 * Compiles and runs a script within the sandbox.
	 *
	 * @param shell  the shell to be used; see {@link #createSecureCompilerConfiguration} and similar methods
	 * @param script the script to run
	 * @return the return value of the script
	 */
	public Object runScript(@NotNull GroovyShell shell, @NotNull String script) {
		StandardGroovySandbox derived = new StandardGroovySandbox();
//				withApprovalContext(context).
//            withTaskListener(listener).
		withWhitelist(new ProxyWhitelist(new ClassLoaderWhitelist(shell.getClassLoader()), whitelist()));
		try (SandboxScope scope = derived.enter()) {
			return shell.parse(script).run();
		}
	}

	/**
	 * Prepares a compiler configuration the sandbox.
	 *
	 * CAUTION
	 * <p>
	 * When creating {@link GroovyShell} with this {@link CompilerConfiguration},
	 * you also have to use {@link #createSecureClassLoader(ClassLoader)} to wrap
	 * a classloader of your choice into sandbox-aware one.
	 *
	 * <p>
	 * Otherwise the classloader that you provide to {@link GroovyShell} might
	 * have its own copy of groovy-sandbox, which lets the code escape the sandbox.
	 *
	 * @return a compiler configuration set up to use the sandbox
	 */
	// TODO: use this
	public static @NotNull CompilerConfiguration createSecureCompilerConfiguration() {
		CompilerConfiguration cc = createBaseCompilerConfiguration();
		cc.addCompilationCustomizers(new SandboxTransformer());
		return cc;
	}

	/**
	 * Prepares a compiler configuration that rejects certain AST transformations. Used by {@link #createSecureCompilerConfiguration()}.
	 */
	public static @NotNull CompilerConfiguration createBaseCompilerConfiguration() {
		CompilerConfiguration cc = new CompilerConfiguration();
		cc.addCompilationCustomizers(new RejectASTTransformsCustomizer());
		cc.setDisabledGlobalASTTransformations(new HashSet<>(Collections.singletonList(GrabAnnotationTransformation.class.getName())));
		return cc;
	}

	/**
	 * Prepares a classloader for Groovy shell for sandboxing.
	 * <p>
	 * See {@link #createSecureCompilerConfiguration()} for the discussion.
	 */
	public static @NotNull ClassLoader createSecureClassLoader(ClassLoader base) {
		return new SandboxResolvingClassLoader(base);
	}

}
