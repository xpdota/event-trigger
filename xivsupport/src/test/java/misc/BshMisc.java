package misc;

import bsh.EvalError;
import bsh.Interpreter;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.logging.Log;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.function.Function;

public class BshMisc {
	private static final Logger log = LoggerFactory.getLogger(BshMisc.class);
	@Test
	void bshLambda() throws EvalError {
		Interpreter interpreter = new Interpreter();
		Object ref = (Function<String, String>) s -> s.toUpperCase();
		Object ref2 = new Function<String, String>() {
			@Override
			public String apply(String s) {
				return s.toUpperCase();
			}
		};
		interpreter.eval(" Object ref2 = new Function<String, String>() { public String apply(String s) { return s.toUpperCase(); } }; ");
	}

	@Test
	void groovyLambda() {
		GroovyShell interpreter = new GroovyShell();
		Function<String, String> myFunc = (Function<String, String>) interpreter.evaluate("(Function<String, String>) s -> s.toUpperCase()");
	}

	@Test
	void groovyJunk() {
		GroovyShell interpreter = new GroovyShell();
		Script script = interpreter.parse("Foo Bar Baz 123");
		Object result = script.run();
		log.info("Result: {}", result);


	}
}
