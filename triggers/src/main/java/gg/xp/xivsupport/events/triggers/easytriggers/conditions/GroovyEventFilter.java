package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.CompileStatic;
import groovy.transform.TypeChecked;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Predicate;

import static org.reflections.scanners.Scanners.SubTypes;

public class GroovyEventFilter implements Condition<Event> {
	private static final Logger log = LoggerFactory.getLogger(GroovyEventFilter.class);

	private static final GroovyShell shell;

	static {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports(Predicate.class.getCanonicalName(), CompileStatic.class.getCanonicalName(), TypeChecked.class.getCanonicalName());
		importCustomizer.addStarImports("gg.xp.xivsupport.events.actlines.events", "javax.swing", "gg.xp.xivsupport.gui", "gg.xp.xivsupport.gui.tables");
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath()).setParallel(true).setScanners(Scanners.SubTypes));
		reflections.get(SubTypes.of(Event.class).asClass()).stream().map(Class::getCanonicalName).filter(Objects::nonNull).forEach(importCustomizer::addImports);

		compilerConfiguration.addCompilationCustomizers(importCustomizer);
		Binding binding = new Binding();
		shell = new GroovyShell(binding, compilerConfiguration);
	}

	public Class<? extends Event> eventType = Event.class;
	private String groovyScript = "event != null";
	private boolean strict = true;
	@JsonIgnore
	private Predicate<? extends Event> groovyCompiledScript;

	public GroovyEventFilter() {
	}

	@JsonCreator
	public GroovyEventFilter(@JsonProperty("groovyScript") String groovyScript,
	                         @JsonProperty("strict") boolean strict,
	                         @JsonProperty("eventType") Class<? extends Event> eventType) {
		this.strict = strict;
		this.eventType = eventType;
		setGroovyScript(groovyScript);
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
		// TODO: does this need to recompile?
	}

	public String getGroovyScript() {
		return groovyScript;
	}

	public void setGroovyScript(String groovyScript) {
		try {
			this.groovyCompiledScript = compile(groovyScript);
		}
		catch (Throwable t) {
			// Special handling for deserialization
			if (groovyCompiledScript == null) {
				groovyCompiledScript = (o) -> false;
				// TODO: expose pre-existing errors on the UI
				log.error("Error compiling groovy script", t);
			}
			else {
				throw new IllegalArgumentException(t);
			}
		}
		this.groovyScript = groovyScript;
	}

	private Predicate<? extends Event> compile(String script) {
		String longClassName = eventType.getCanonicalName();
		String shortClassName = eventType.getSimpleName();
		String varName = "event";
		String checkType = strict ? "@CompileStatic" : "";
		String inJavaForm = """
				import %s;
				new Predicate<%s>() {
					%s
					@Override
					public boolean test(%s %s) {
						%s
					}
				};
				""".formatted(longClassName, shortClassName, checkType, shortClassName, varName, script);
		return (Predicate<? extends Event>) shell.evaluate(inJavaForm);

	}

	@Override
	public String fixedLabel() {
		return "Groovy Filter";
	}

	@Override
	public String dynamicLabel() {
		return "(Groovy Expression)";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean test(Event event) {
		if (groovyCompiledScript == null) {
			groovyCompiledScript = compile(groovyScript);
		}
		if (eventType.isInstance(event)) {
			return ((Predicate<Event>) groovyCompiledScript).test(event);
		}
		else {
			return false;
		}
	}

}
