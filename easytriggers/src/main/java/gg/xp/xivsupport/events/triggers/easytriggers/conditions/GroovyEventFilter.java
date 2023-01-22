package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.SubBinding;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class GroovyEventFilter implements Condition<Event> {
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private static final Logger log = LoggerFactory.getLogger(GroovyEventFilter.class);

	private final GroovyManager mgr;
	private @Nullable GroovyShell shell;


	public Class<? extends Event> eventType = Event.class;
	private String groovyScript = "event != null";
	private boolean strict = true;
	@JsonIgnore
	private Predicate<? extends Event> groovyCompiledScript;
	@JsonIgnore
	private volatile Throwable lastError;
	private volatile EasyTriggerContext currentContext;

	public GroovyEventFilter(@JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr) {
		this.mgr = mgr;
	}

	@JsonCreator
	public GroovyEventFilter(@JsonProperty("groovyScript") String groovyScript,
	                         @JsonProperty("strict") boolean strict,
	                         @JsonProperty("eventType") Class<? extends Event> eventType,
	                         @JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr
	) {
		this(mgr);
		this.strict = strict;
		this.eventType = eventType;
		this.groovyScript = groovyScript;
		exs.submit(() -> groovyCompiledScript = compile(groovyScript));
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
		if (shell == null) {
			shell = mgr.makeShell();
		}
		String longClassName = eventType.getCanonicalName();
		String shortClassName = eventType.getSimpleName();
		String varName = "event";
		String checkType = strict ? "@CompileStatic" : "";
//		String inJavaForm = """
//				import %s;
//				new Predicate<%s>() {
//					%s
//					@Override
//					public boolean test(%s %s) {
//						%s
//					}
//				};
//				""".formatted(longClassName, shortClassName, checkType, shortClassName, varName, script);
		String inJavaForm = """
				%s
				public boolean test(%s %s) {
					%s
				}
				Predicate<%s> myPredicate = this::test;
				return myPredicate;
				""".formatted(checkType, longClassName, varName, script, longClassName);
		try (SandboxScope ignored = mgr.getSandbox().enter()) {
			Script parsedScript = shell.parse(inJavaForm);
			Binding originalBinding = parsedScript.getBinding();
			// TODO: does getVariables() also need to be overridden?
			SubBinding mergedBinding = new SubBinding(originalBinding) {
				@Override
				public Object getVariable(String name) {
					Object extra = currentContext.getExtraVariables().get(name);
					if (extra != null) {
						return extra;
					}
					return super.getVariable(name);
				}

				@Override
				public boolean hasVariable(String name) {
					return currentContext.getExtraVariables().containsKey(name) || super.hasVariable(name);
				}

				@Override
				public void setVariable(String name, Object value) {
					currentContext.addVariable(name, value);
				}
			};
			parsedScript.setBinding(mergedBinding);
			return (Predicate<? extends Event>) parsedScript.run();
		}

	}


	@Override
	public String fixedLabel() {
		return "Groovy Filter";
	}

	@Override
	public String dynamicLabel() {
		return "(Groovy Expression)";
	}

	public Throwable getLastError() {
		return lastError;
	}

	@Override
	public boolean test(EasyTriggerContext context, Event event) {
		if (groovyCompiledScript == null) {
			try {
				groovyCompiledScript = compile(groovyScript);
			}
			catch (Throwable t) {
				log.error("Error compiling script, disabling", t);
				groovyCompiledScript = (e) -> false;
			}
		}
		if (eventType.isInstance(event)) {
			currentContext = context;
			try (SandboxScope ignored = mgr.getSandbox().enter()) {
				return ((Predicate<Event>) groovyCompiledScript).test(event);
			}
			catch (Throwable t) {
				log.error("Easy trigger Groovy script encountered an error (returning false)", t);
				lastError = t;
				return false;
			}
			finally {
				currentContext = null;
			}
		}
		else {
			return false;
		}
	}

}
