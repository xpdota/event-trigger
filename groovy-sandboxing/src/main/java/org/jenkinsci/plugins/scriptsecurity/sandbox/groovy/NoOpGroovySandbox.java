package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

public class NoOpGroovySandbox implements GroovySandbox {
	@Override
	public SandboxScope enter() {
		return () -> {
		};
	}
}
