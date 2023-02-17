package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.groovy.GroovyManager;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GroovyAction implements Action<Event> {
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private static final Logger log = LoggerFactory.getLogger(GroovyAction.class);

	private final GroovyManager mgr;
	private @Nullable GroovyShell shell;


	public Class<? extends Event> eventType = Event.class;
	private String groovyScript = "globals.groovyActionEvent = event";
	private boolean strict = true;
	@JsonIgnore
	private Consumer<? extends Event> groovyCompiledScript;
	@JsonIgnore
	private volatile Throwable lastError;
	private volatile EasyTriggerContext currentContext;

	public GroovyAction(@JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr) {
		this.mgr = mgr;
	}

	@JsonCreator
	public GroovyAction(@JsonProperty("groovyScript") String groovyScript,
	                    @JsonProperty("strict") boolean strict,
	                    @JsonProperty("eventType") Class<? extends Event> eventType,
	                    @JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr
	) {
		this(mgr);
		this.strict = strict;
		this.eventType = eventType;
		this.groovyScript = groovyScript;
		exs.submit(() -> {
			this.groovyCompiledScript = compile(groovyScript);
		});
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
				groovyCompiledScript = (o) -> {
				};
				// TODO: expose pre-existing errors on the UI
				log.error("Error compiling groovy script", t);
			}
			else {
				throw new IllegalArgumentException(t);
			}
		}
		this.groovyScript = groovyScript;
	}

	private Consumer<? extends Event> compile(String script) {
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
				public void accept(%s %s) {
					%s
				}
				Consumer<%s> myConsumer = this::accept;
				return myConsumer;
				""".formatted(checkType, longClassName, varName, script, longClassName);
		try (SandboxScope ignored = mgr.getSandbox().enter()) {
			Script parsedScript = shell.parse(inJavaForm);
			Binding originalBinding = parsedScript.getBinding();
			// TODO: does getVariables() also need to be overridden?
			// TODO: make this more official
			Binding mergedBinding = new Binding(originalBinding.getVariables()) {
				@Override
				public Object getVariable(String name) {
					Object extra = currentContext.getExtraVariables().get(name);
					if (extra != null) {
						return extra;
					}
					return super.getVariable(name);
				}

				@Override
				public void setVariable(String name, Object value) {
					currentContext.addVariable(name, value);
				}

				@Override
				public boolean hasVariable(String name) {
					return currentContext.getExtraVariables().containsKey(name) || super.hasVariable(name);
				}
			};
			parsedScript.setBinding(mergedBinding);
			return (Consumer<? extends Event>) parsedScript.run();
		}

	}


	@Override
	public void accept(EasyTriggerContext context, Event event) {
		// TODO: replace this with Future<X> and have scripts automatically build in the background
		// on startup.
		// Then, replace the editor with the nice multi-line editor.
		// This should alleviate the need to have separate trigger + script.
		if (groovyCompiledScript == null) {
			try {
				groovyCompiledScript = compile(groovyScript);
			}
			catch (Throwable t) {
				log.error("Error compiling script, disabling", t);
				groovyCompiledScript = (e) -> {
				};
			}
		}
		if (eventType.isInstance(event)) {
			currentContext = context;
			try (SandboxScope ignored = mgr.getSandbox().enter()) {
				((Consumer<Event>) groovyCompiledScript).accept(event);
			}
			catch (Throwable t) {
				log.error("Easy trigger Groovy script encountered an error", t);
				lastError = t;
			}
			finally {
				currentContext = null;
			}
		}
	}

	@Override
	public String fixedLabel() {
		return "Groovy Action";
	}

	@Override
	public String dynamicLabel() {
		return "(Groovy Expression)";
	}

	public Throwable getLastError() {
		return lastError;
	}


}
