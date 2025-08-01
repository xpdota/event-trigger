package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
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
import java.util.function.Supplier;

/**
 * A version of GroovyEventFilter that doesn't require an event parameter.
 * This is suitable for use with TriggerFolder which doesn't restrict to any specific event type.
 * Instead of a {@code Predicate<? extends Event>}, it uses a {@code Supplier<Boolean>}.
 */
public class GroovyFolderFilter implements Condition<Object> {
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private static final Logger log = LoggerFactory.getLogger(GroovyFolderFilter.class);

	private final GroovyManager mgr;
	private @Nullable GroovyShell shell;

	private String groovyScript = "true";
	private boolean strict = true;
	@JsonIgnore
	private Supplier<Boolean> groovyCompiledScript;
	@JsonIgnore
	private volatile Throwable lastError;
	private volatile EasyTriggerContext currentContext;

	public GroovyFolderFilter(@JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr) {
		this.mgr = mgr;
	}

	@JsonCreator
	public GroovyFolderFilter(@JsonProperty("groovyScript") String groovyScript,
	                          @JsonProperty("strict") boolean strict,
	                          @JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr
	) {
		this(mgr);
		this.strict = strict;
		this.groovyScript = groovyScript;
		exs.submit(() -> groovyCompiledScript = compile(groovyScript));
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
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
				groovyCompiledScript = () -> false;
				log.error("Error compiling groovy script", t);
			}
			else {
				throw new IllegalArgumentException(t);
			}
		}
		this.groovyScript = groovyScript;
	}

	private Supplier<Boolean> compile(String script) {
		if (shell == null) {
			shell = mgr.makeShell();
		}
		String checkType = strict ? "@CompileStatic" : "";
		String inJavaForm = """
				%s
				public boolean get() {
					%s
				}
				Supplier<Boolean> mySupplier = this::get;
				return mySupplier;
				""".formatted(checkType, script);
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
			return (Supplier<Boolean>) parsedScript.run();
		}
	}

	@Override
	public String fixedLabel() {
		return "Groovy Supplier Filter";
	}

	@Override
	public String dynamicLabel() {
		return "(Groovy Expression)";
	}

	@Override
	public Class<Object> getEventType() {
		return Object.class;
	}

	// TODO: expose this on UI?
	public Throwable getLastError() {
		return lastError;
	}

	@Override
	public boolean test(EasyTriggerContext context, Object event) {
		if (groovyCompiledScript == null) {
			try {
				groovyCompiledScript = compile(groovyScript);
			}
			catch (Throwable t) {
				log.error("Error compiling script, disabling", t);
				groovyCompiledScript = () -> false;
			}
		}
		
		currentContext = context;
		try (SandboxScope ignored = mgr.getSandbox().enter()) {
			return groovyCompiledScript.get();
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
}