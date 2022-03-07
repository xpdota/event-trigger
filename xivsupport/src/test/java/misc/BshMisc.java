package misc;

import bsh.EvalError;
import bsh.Interpreter;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.function.Function;

@Ignore
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

//	public static void main(String[] args) {
//		new BshMisc().testGroovyConsole();
//	}
//
//	@Test
//	void testGroovyConsole() {
//		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
//		ImportCustomizer importCustomizer = new ImportCustomizer();
//		importCustomizer.addImports(
//				Predicate.class.getCanonicalName(),
//				CompileStatic.class.getCanonicalName(),
//				TypeChecked.class.getCanonicalName());
//		importCustomizer.addStarImports("gg.xp.xivsupport.events.actlines.events");
//		Reflections reflections = new Reflections(
//				new ConfigurationBuilder()
//						.setUrls(ClasspathHelper.forJavaClassPath())
//						.setScanners(Scanners.SubTypes));
//		reflections.get(SubTypes.of(Event.class).asClass())
//				.stream()
//				.map(Class::getCanonicalName)
//				.filter(Objects::nonNull)
//				.forEach(importCustomizer::addImports);
//
//		compilerConfiguration.addCompilationCustomizers(importCustomizer);
//		Binding binding = new Binding();
//		binding.setVariable("foo", 123);
//		Console console = new Console(BshMisc.class.getClassLoader(), binding, compilerConfiguration);
//		console.run();
//
//	}
}
