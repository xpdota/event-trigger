package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.conversions.PlayerNameConversion;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.gui.groovy.GroovyManager;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CalloutProcessor {

	private static final Logger log = LoggerFactory.getLogger(CalloutProcessor.class);

	private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
	private final GroovyManager groovyMgr;
	private volatile GroovyShell interpreter;
	// TODO: this *shouldn't* need to be static, but something is up with it
	private static final Object interpLock = new Object();
	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");

	private final BooleanSetting replaceYou;
	private final EnumSetting<PlayerNameConversion> pcNameStyle;

	public CalloutProcessor(GroovyManager groovyMgr, PersistenceProvider pers) {
		this.groovyMgr = groovyMgr;
		this.replaceYou = new BooleanSetting(pers, "callout-processor.replace-you", true);
		this.pcNameStyle = new EnumSetting<>(pers, "callout-processor.pc-style", PlayerNameConversion.class, PlayerNameConversion.FULL_NAME);
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
		Map<String, Object> arguments = new HashMap<>(raw.getArguments());
		X event = raw.getEvent();
		if (event != null) {
			arguments.put("event", event);
		}
		Binding binding = groovyMgr.makeBinding();
		arguments.forEach(binding::setVariable);
//		Binding binding = new Binding(arguments);

		// TODO: fast path for when there are no {}
		String tts = applyReplacements(raw, raw.getTts(), binding);
		Supplier<String> text = () -> applyReplacements(raw, raw.getText(), binding);

		ProcessedCalloutEvent out = new ProcessedCalloutEvent(
				raw.trackingKey(),
				tts,
				text,
				() -> raw.getExpiry().getAsBoolean(),
				() -> raw.getGuiProvider().apply(event),
				raw.getColorOverride(),
				raw.getSound());
		out.setReplaces(raw.getReplaces());
		return out;
	}


	private Script compile(String input) {
		setupShell();
		return interpreter.parse(input);
	}

	@Contract("_, null, _ -> null")
	public @Nullable String applyReplacements(RawModifiedCallout<?> raw, @Nullable String input, Binding binding) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}
		synchronized (interpLock) {
			return replacer.matcher(input).replaceAll(m -> {
				try {
					Script script = scriptCache.computeIfAbsent(m.group(1), this::compile);
					script.setBinding(binding);
					Object rawEval = script.run();
					if (rawEval == null) {
						return "null";
//						return m.group(0);
					}
					return singleReplacement(rawEval);
				}
				catch (Throwable e) {
					if (raw.shouldLogError()) {
						log.error("Eval error for input '{}'", input, e);
					}
					return "Error";
				}
			});
		}
	}

	// Default conversions
	@SuppressWarnings("unused")
	public String singleReplacement(Object rawValue) {
		if (rawValue instanceof String strVal) {
			return strVal;
		}
		else if (rawValue instanceof XivCombatant cbt) {
			if (cbt.isThePlayer() && replaceYou.get()) {
				return "YOU";
			}
			else if (rawValue instanceof XivPlayerCharacter xpc) {
				return pcNameStyle.get().convert(xpc);
			}
			else {
				return cbt.getName();
			}
		}
		else if (rawValue instanceof NameIdPair pair) {
			return pair.getName();
		}
		else if (rawValue instanceof Duration dur) {
			if (dur.isZero()) {
				return "NOW";
			}
			return String.format("%.01f", dur.toMillis() / 1000.0);
		}
		else if (rawValue instanceof Supplier supp) {
			Object realValue = supp.get();
			// Prevent infinite loops if a supplier produces another supplier
			if (realValue instanceof Supplier) {
				return realValue.toString();
			}
			else {
				return singleReplacement(realValue);
			}
		}
		else if (rawValue instanceof HasFriendlyName hfn) {
			return hfn.getFriendlyName();
		}
		else if (rawValue instanceof Collection<?> coll) {
			return coll.stream()
					.map(this::singleReplacement)
					.collect(Collectors.joining(", "));
		}
		else {
			return rawValue.toString();
		}
	}

	public BooleanSetting getReplaceYou() {
		return replaceYou;
	}

	public EnumSetting<PlayerNameConversion> getPcNameStyle() {
		return pcNameStyle;
	}
}
