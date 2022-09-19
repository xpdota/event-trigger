package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

@FunctionalInterface
public interface SandboxScope extends AutoCloseable {

	@Override
	void close();

}
