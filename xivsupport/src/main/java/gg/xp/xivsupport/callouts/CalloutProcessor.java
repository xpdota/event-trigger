package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.conversions.GlobalCallReplacer;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CalloutProcessor {

	private static final Logger log = LoggerFactory.getLogger(CalloutProcessor.class);

	private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
	private final GroovyManager groovyMgr;
	private volatile GroovyShell interpreter;
	// TODO: this *shouldn't* need to be static, but something is up with it
	private static final Object interpLock = new Object();
	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");

	private final GlobalCallReplacer gcr;
	private final SingleValueReplacement svr;

	// TODO: make interface/autoscan for all the converters
	public CalloutProcessor(GroovyManager groovyMgr, GlobalCallReplacer gcr, SingleValueReplacement svr) {
		this.groovyMgr = groovyMgr;
		this.gcr = gcr;
		this.svr = svr;
	}

	@HandleEvents
	public <X> void handleRawModifiedCallout(EventContext ctx, RawModifiedCallout<X> raw) {
		CalloutEvent realCall = processCallout(raw);
		ctx.accept(realCall);
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

	@HandleEvents(order = Integer.MAX_VALUE)
	public void initEvent(EventContext ctx, InitEvent init) {
		// TODO: this is bad, but works
		// It will probably not matter anyway once the other groovy stuff is done
		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(10_000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			compile("\"dummy script to force init\"");
		}, "CalloutProcessorSetup");
		thread.setDaemon(true);
		thread.start();
	}

	public <X> CalloutEvent processCallout(RawModifiedCallout<X> raw) {
		String rawTts = raw.getTts();
		String rawText = raw.getText();
		String tts;
		Supplier<String> text;
		X event = raw.getEvent();
		boolean fastPath = (rawTts == null || !rawTts.contains("{")) && (rawText == null || !rawText.contains("{"));
		if (fastPath) {
			// Fast path for when there are no {}
			tts = rawTts == null ? null : gcr.doReplacements(rawTts, true);
			String finalText = rawText == null ? null : gcr.doReplacements(rawText, false);
			text = () -> finalText;
		}
		else {
			Map<String, Object> arguments = new HashMap<>(raw.getArguments());
			if (event != null) {
				arguments.put("event", event);
			}
			Binding binding = groovyMgr.makeBinding();
			arguments.forEach(binding::setVariable);
//		Binding binding = new Binding(arguments);

			tts = applyReplacements(raw, rawTts, binding, true);
			text = () -> applyReplacements(raw, rawText, binding, false);
		}

		ProcessedCalloutEvent out = new ProcessedCalloutEvent(
				raw.trackingKey(),
				tts,
				text,
				() -> raw.getExpiry().getAsBoolean(),
				() -> raw.getGuiProvider().apply(event),
				raw.getColorOverride(),
				raw.getSound());
		out.setReplaces(raw.getReplaces());
		log.info("Callout: TTS='{}' from '{}' caused by '{}'", tts, raw.getDescription(), raw.getParent());
		return out;
	}


	private Script compile(String input) {
		setupShell();
		return interpreter.parse(input);
	}

	@Contract("_, null, _, _ -> null")
	public @Nullable String applyReplacements(RawModifiedCallout<?> raw, @Nullable String input, Binding binding, boolean isTts) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}
		String resolved;
		synchronized (interpLock) {
			resolved = replacer.matcher(input).replaceAll(m -> {
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
					return svr.singleReplacement(rawEval);
				}
				catch (Throwable e) {
					if (raw.shouldLogError()) {
						log.error("Eval error for input '{}'", input, e);
					}
					return "Error";
				}
			});
		}
		return gcr.doReplacements(resolved, isTts);
	}

}
