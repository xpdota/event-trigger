package gg.xp.xivsupport.groovy;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.SingleValueReplacement;
import gg.xp.xivsupport.callouts.conversions.GlobalCallReplacer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@ScanMe
public class GroovyScriptProcessor {

	private static final Logger log = LoggerFactory.getLogger(GroovyScriptProcessor.class);
	// TODO: this *shouldn't* need to be static, but something is up with it
	private static final Object interpLock = new Object();
	// TODO: this should have {{ }} support like in CalloutProcessor
	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");
	private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
	private final GroovyManager groovyMgr;
	private final GlobalCallReplacer gcr;
	private final SingleValueReplacement svr;

	private volatile GroovyShell interpreter;

	public GroovyScriptProcessor(GroovyManager groovyMgr,
	                             GlobalCallReplacer gcr,
	                             SingleValueReplacement svr) {
		this.groovyMgr = groovyMgr;
		this.gcr = gcr;
		this.svr = svr;
	}

	private void setupShell() {
		if (interpreter == null) {
			synchronized (interpLock) {
				if (interpreter == null) {
					interpreter = groovyMgr.makeShell();
				}
			}
		}
	}

	public @Nullable String replaceInString(@Nullable String input, Binding binding, boolean individualReplacements, boolean globalReplacements) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}
		synchronized (interpLock) {
			String result = replacer.matcher(input).replaceAll(m -> {
				try {
					Object rawEval;
					try (SandboxScope ignored = groovyMgr.getSandbox().enter()) {
						Script script = scriptCache.computeIfAbsent(m.group(1), this::compile);
						script.setBinding(binding);
						rawEval = script.run();
					}
					if (rawEval == null) {
						return "null";
//						return m.group(0);
					}
					else {
						if (individualReplacements) {
							return svr.singleReplacement(rawEval);
						}
						else {
							return rawEval.toString();
						}
					}
				}
				catch (Throwable e) {
					log.error("Eval error for input '{}'", input, e);
					return "Error";
				}
			});
			if (globalReplacements) {
				return gcr.doReplacements(result, false);
			}
			return result;
		}
	}

	public @Nullable <X> X runScript(@Nullable String input, Binding binding, Class<X> expectedType) {
		if (input == null) {
			return null;
		}
		synchronized (interpLock) {
			try (SandboxScope ignored = groovyMgr.getSandbox().enter()) {
				Script script = scriptCache.computeIfAbsent(input, this::compile);
				script.setBinding(binding);
				Object result = script.run();
				return expectedType.cast(result);
			}
		}
	}

	private Script compile(String input) {
		setupShell();
		return interpreter.parse(input);
	}

}
