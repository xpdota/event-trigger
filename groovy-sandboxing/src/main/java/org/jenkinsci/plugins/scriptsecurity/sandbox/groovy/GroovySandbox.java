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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public final class GroovySandbox {

	public static final Logger LOGGER = Logger.getLogger(GroovySandbox.class.getName());

	private @CheckForNull Whitelist whitelist;

	/**
	 * Creates a sandbox with default settings.
	 */
	public GroovySandbox() {
	}

	/**
	 * Specify a whitelist.
	 * By default {@link Whitelist#all} is used.
	 *
	 * @return {@code this}
	 */
	public GroovySandbox withWhitelist(@CheckForNull Whitelist whitelist) {
		this.whitelist = whitelist;
		return this;
	}



	private @NonNull Whitelist whitelist() {
		return whitelist != null ? whitelist : Whitelist.all();
	}

	/**
	 * Starts a dynamic scope within which calls will be sandboxed.
	 *
	 * @return a scope object, useful for putting this into a {@code try}-with-resources block
	 */
	@SuppressWarnings("deprecation") // internal use of accessRejected still valid
	public Scope enter() {
		GroovyInterceptor sandbox = new SandboxInterceptor(whitelist());
		sandbox.register();
//        ScriptApproval.pushRegistrationCallback(x -> {
//            if (ExtensionList.lookup(RootAction.class).get(ScriptApproval.class) == null) {
//                return; // running in unit test, ignore
//            }
//            String signature = x.getSignature();
//            if (!StaticWhitelist.isPermanentlyBlacklisted(signature)) {
//                ScriptApproval.get().accessRejected(x, _context);
//            }
//            if (listener != null) {
//                ScriptApprovalNote.print(listener, x);
//            }
//        });
		return () -> {
			sandbox.unregister();
		};
	}

	/**
	 * Handle for exiting the dynamic scope of the Groovy sandbox.
	 *
	 * @see #enter
	 */
	@FunctionalInterface
	public interface Scope extends AutoCloseable {

		@Override
		void close();

	}

	/**
	 * Compiles and runs a script within the sandbox.
	 *
	 * @param shell  the shell to be used; see {@link #createSecureCompilerConfiguration} and similar methods
	 * @param script the script to run
	 * @return the return value of the script
	 */
	public Object runScript(@NonNull GroovyShell shell, @NonNull String script) {
		GroovySandbox derived = new GroovySandbox();
//				withApprovalContext(context).
//            withTaskListener(listener).
		withWhitelist(new ProxyWhitelist(new ClassLoaderWhitelist(shell.getClassLoader()), whitelist()));
		try (Scope scope = derived.enter()) {
			return shell.parse(script).run();
		}
	}

	/**
	 * Prepares a compiler configuration the sandbox.
	 *
	 * <h2>CAUTION</h2>
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
	public static @NonNull CompilerConfiguration createSecureCompilerConfiguration() {
		CompilerConfiguration cc = createBaseCompilerConfiguration();
		cc.addCompilationCustomizers(new SandboxTransformer());
		return cc;
	}

	/**
	 * Prepares a compiler configuration that rejects certain AST transformations. Used by {@link #createSecureCompilerConfiguration()}.
	 */
	public static @NonNull CompilerConfiguration createBaseCompilerConfiguration() {
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
	@SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "Should be managed by the caller.")
	public static @NonNull ClassLoader createSecureClassLoader(ClassLoader base) {
		return new SandboxResolvingClassLoader(base);
	}

	/**
	 * Runs a block in the sandbox.
	 * You must have used {@link #createSecureCompilerConfiguration} to prepare the Groovy shell.
	 * Use {@link #run} instead whenever possible.
	 *
	 * @param r         a block of code during whose execution all calls are intercepted
	 * @param whitelist the whitelist to use, such as {@link Whitelist#all()}
	 * @throws RejectedAccessException in case an attempted call was not whitelisted
	 * @deprecated use {@link #enter}
	 */
	@Deprecated
	public static void runInSandbox(@NonNull Runnable r, @NonNull Whitelist whitelist) throws RejectedAccessException {
		try (Scope scope = new GroovySandbox().withWhitelist(whitelist).enter()) {
			r.run();
		}
	}

	/**
	 * Runs a function in the sandbox.
	 * You must have used {@link #createSecureCompilerConfiguration} to prepare the Groovy shell.
	 * Use {@link #run} instead whenever possible.
	 *
	 * @param c         a block of code during whose execution all calls are intercepted
	 * @param whitelist the whitelist to use, such as {@link Whitelist#all()}
	 * @return the return value of the block
	 * @throws RejectedAccessException in case an attempted call was not whitelisted
	 * @throws Exception               in case the block threw some other exception
	 * @deprecated use {@link #enter}
	 */
	@Deprecated
	public static <V> V runInSandbox(@NonNull Callable<V> c, @NonNull Whitelist whitelist) throws Exception {
		try (Scope scope = new GroovySandbox().withWhitelist(whitelist).enter()) {
			return c.call();
		}
	}

	// TODO: Delete after 2020-01-01 because the method is insecure in most scenarios. Known callers have already
	// migrated to safer methods, but we want to give users time to update plugins to avoid unnecessary breakage.

	/**
	 * @deprecated insecure; use {@link #run(GroovyShell, String, Whitelist)} or {@link #runScript}
	 */
	@Deprecated
	public static Object run(@NonNull Script script, @NonNull final Whitelist whitelist) throws RejectedAccessException {
//        LOGGER.log(Level.WARNING, null, new IllegalStateException(Messages.GroovySandbox_useOfInsecureRunOverload()));
		Whitelist wrapperWhitelist = new ProxyWhitelist(
				new ClassLoaderWhitelist(script.getClass().getClassLoader()),
				whitelist);
		try (Scope scope = new GroovySandbox().withWhitelist(wrapperWhitelist).enter()) {
			return script.run();
		}
	}

	/**
	 * Runs a script in the sandbox.
	 * You must have used {@link #createSecureCompilerConfiguration} to prepare the Groovy shell.
	 *
	 * @param shell     a shell ready for {@link GroovyShell#parse(String)}
	 * @param script    a script
	 * @param whitelist the whitelist to use, such as {@link Whitelist#all()}
	 * @return the value produced by the script, if any
	 * @throws RejectedAccessException in case an attempted call was not whitelisted
	 * @deprecated use {@link #runScript}
	 */
	@Deprecated
	public static Object run(@NonNull final GroovyShell shell, @NonNull final String script, @NonNull final Whitelist whitelist) throws RejectedAccessException {
		return new GroovySandbox().withWhitelist(whitelist).runScript(shell, script);
	}

//    /**
//     * Checks a script for compilation errors in a sandboxed environment, without going all the way to actual class
//     * creation or initialization.
//     * @param script The script to check
//     * @param classLoader The {@link GroovyClassLoader} to use during compilation.
//     * @return The {@link FormValidation} for the compilation check.
//     */
//    public static @NonNull FormValidation checkScriptForCompilationErrors(String script, GroovyClassLoader classLoader) {
//        try {
//            CompilationUnit cu = new CompilationUnit(
//                    createSecureCompilerConfiguration(),
//                    new CodeSource(new URL("file", "", DEFAULT_CODE_BASE), (Certificate[]) null),
//                    classLoader);
//            cu.addSource("Script1", script);
//            cu.compile(Phases.CANONICALIZATION);
//        } catch (MalformedURLException | CompilationFailedException e) {
//            return FormValidation.error(e.getLocalizedMessage());
//        }
//
//        return FormValidation.ok();
//    }

}
