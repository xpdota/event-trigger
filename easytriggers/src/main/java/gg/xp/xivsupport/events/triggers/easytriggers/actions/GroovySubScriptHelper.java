package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
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
import java.util.function.Function;

public class GroovySubScriptHelper implements HasMutableEventType {
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private static final Logger log = LoggerFactory.getLogger(GroovySubScriptHelper.class);

	private final GroovyManager mgr;
	private @Nullable GroovyShell shell;


	public Class<?> eventType = Event.class;
	private String groovyScript = "event";
	@JsonIgnore
	private Function<Event, Object> groovyCompiledScript;
	@JsonIgnore
	private volatile Throwable lastError;
	private volatile EasyTriggerContext currentContext;

	public GroovySubScriptHelper(@JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr) {
		this.mgr = mgr;
	}

	@JsonCreator
	public GroovySubScriptHelper(@JsonProperty("groovyScript") String groovyScript,
	                             @JsonProperty("eventType") Class<? extends Event> eventType,
	                             @JacksonInject(useInput = OptBoolean.FALSE) GroovyManager mgr
	) {
		this(mgr);
		this.eventType = eventType;
		this.groovyScript = groovyScript;
		exs.submit(() -> {
			this.groovyCompiledScript = compile(groovyScript);
		});
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
				groovyCompiledScript = (o) -> null;
				// TODO: expose pre-existing errors on the UI
				log.error("Error compiling groovy script", t);
			}
			else {
				throw new IllegalArgumentException(t);
			}
		}
		this.groovyScript = groovyScript;
	}

	private Function<Event, Object> compile(String script) {
		if (shell == null) {
			shell = mgr.makeShell();
		}
		try (SandboxScope ignored = mgr.getSandbox().enter()) {
			Script parsedScript = shell.parse(script);
			Binding originalBinding = parsedScript.getBinding();
			// TODO: does getVariables() also need to be overridden?
			// TODO: make this more official
			// TODO: verify that this reflects changes in the parent binding
			Binding mergedBinding = new Binding(originalBinding.getVariables()) {
				@Override
				public Object getVariable(String name) {
					if (name.equals("event")) {
						return currentEvent;
					}
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
			return event -> parsedScript.run();
		}

	}

	private volatile Event currentEvent;

	public Object run(EasyTriggerContext context, Event event) {
		if (groovyCompiledScript == null) {
			try {
				groovyCompiledScript = compile(groovyScript);
			}
			catch (Throwable t) {
				log.error("Error compiling script, disabling", t);
				groovyCompiledScript = (e) -> null;
			}
		}
		if (eventType.isInstance(event)) {
			currentContext = context;
			currentEvent = event;
			try (SandboxScope ignored = mgr.getSandbox().enter()) {
				return groovyCompiledScript.apply(event);
			}
			catch (Throwable t) {
				lastError = t;
				throw t;
			}
			finally {
				currentContext = null;
				currentEvent = null;
			}
		}
		return null;
	}

	//	@Override
//	public String fixedLabel() {
//		return "Groovy Action";
//	}
//
//	@Override
//	public String dynamicLabel() {
//		return "(Groovy Expression)";
//	}
//
	public Throwable getLastError() {
		return lastError;
	}


	@Override
	public Class<?> getEventType() {
		return eventType;
	}

	@Override
	public void setEventType(Class<?> eventType) {
		this.eventType = eventType;
	}
}
