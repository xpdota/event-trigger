package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

import java.util.function.Function;

public interface GroovySandbox {
	SandboxScope enter();

	default <X, Y> Function<X, Y> wrapFunc(Function<X, Y> func) {
		return x -> {
			try (var ignored = enter()) {
				return func.apply(x);
			}
		};
	}

	default Runnable wrapRunnable(Runnable run) {
		return () -> {
			try (var ignored = enter()) {
				run.run();
			}
		};
	}
}
