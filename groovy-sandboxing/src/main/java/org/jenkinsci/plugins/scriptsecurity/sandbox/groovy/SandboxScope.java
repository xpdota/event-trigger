package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

/**
 * Handle for exiting the dynamic scope of the Groovy sandbox.
 *
 * @see #enter
 */
@FunctionalInterface
public interface SandboxScope extends AutoCloseable {

	@Override
	void close();

}
