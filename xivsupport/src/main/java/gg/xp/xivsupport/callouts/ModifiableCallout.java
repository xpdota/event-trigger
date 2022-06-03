package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.time.TimeUtils;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.BaseCalloutEvent;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.DynamicCalloutEvent;
import gg.xp.xivsupport.speech.ParentedCalloutEvent;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// TODO: refactor all of this into a builder pattern
public class ModifiableCallout<X> {

	private static final Logger log = LoggerFactory.getLogger(ModifiableCallout.class);

	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");
	private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
	// TODO: should this use GroovyManager instead?
	private final GroovyShell interpreter = new GroovyShell();

	private final String description;
	private final String defaultTtsText;
	private final String defaultVisualText;
	private final Predicate<X> expiry;
	private final List<CalloutCondition> conditions;
	private final long defaultVisualHangTime;
	private final Object interpLock = new Object();
	private int errorCount;
	private static final int maxErrors = 10;
	private Function<? super X, ? extends @Nullable Component> guiProvider = e -> null;

	private static final Duration defaultHangDuration = Duration.of(5, ChronoUnit.SECONDS);

	private volatile ModifiedCalloutHandle handle;

	public ModifiableCallout(String description, String text) {
		this(description, text, Collections.emptyList());
	}

	public ModifiableCallout(String description, String tts, String text, Predicate<X> expiry) {
		this.description = description;
		this.defaultTtsText = tts;
		this.defaultVisualText = text;
		this.expiry = expiry;
		this.defaultVisualHangTime = 5000L;
		conditions = Collections.emptyList();
	}

	public ModifiableCallout(String description, String ttsAndText, int msExpiry) {
		this.description = description;
		this.defaultTtsText = ttsAndText;
		this.defaultVisualText = ttsAndText;
		this.expiry = expiresIn(Duration.ofMillis(msExpiry));
		this.defaultVisualHangTime = msExpiry;
		conditions = Collections.emptyList();
	}

	public ModifiableCallout(String description, String text, List<CalloutCondition> conditions) {
		this(description, text, text, conditions);
	}

	public ModifiableCallout(String description, String tts, String text, List<CalloutCondition> conditions) {
		this.description = description;
		defaultTtsText = tts;
		defaultVisualText = text;
		this.conditions = new ArrayList<>(conditions);
		defaultVisualHangTime = 5000L;
		this.expiry = expiresIn(defaultHangDuration);
	}

	public static <X> Predicate<X> expiresIn(Duration dur) {
		Instant defaultExpiryAt = TimeUtils.now().plus(dur);
		return eventItem -> {
			if (eventItem instanceof BaseEvent be) {
				return be.getEffectiveTimeSince().compareTo(dur) > 0;
			}
			else {
				return defaultExpiryAt.isBefore(Instant.now());
			}
		};
	}

	public ModifiableCallout<X> autoIcon() {
		this.guiProvider = e -> IconTextRenderer.getStretchyIcon(RenderUtils.guessIconFor(e));
		return this;
	}

	public ModifiableCallout<X> guiProvider(Function<? super X, ? extends Component> guiProvider) {
		this.guiProvider = guiProvider;
		return this;
	}

	public static <X> Predicate<X> expiresIn(int seconds) {
		return expiresIn(Duration.ofSeconds(seconds));
	}

	public void attachHandle(ModifiedCalloutHandle handle) {
		this.handle = handle;
	}

	public String getDescription() {
		return description;
	}

	public String getOriginalTts() {
		return defaultTtsText;
	}

	public String getOriginalVisualText() {
		return defaultVisualText;
	}

	public CalloutEvent getModified() {
		return getModified(Collections.emptyMap());
	}

	public CalloutEvent getModified(X event) {
		return getModified(event, Collections.emptyMap());
	}

	public CalloutEvent getModified(X event, Map<String, Object> rawArguments) {
		// TODO
		Map<String, Object> arguments = new HashMap<>(rawArguments);
		arguments.put("event", event);
		String callText;
		String visualText;
		if (handle == null) {
			// TODO: consider splitting out some logic here so that we can make these easily without worrying about handles
			log.trace("ModifiableCallout does not have handle yet ({})", description);
			callText = defaultTtsText;
			visualText = defaultVisualText;
		}
		else {
			callText = handle.getEffectiveTts();
			visualText = handle.getEffectiveText();
		}
		String modifiedCallText = applyReplacements(callText, arguments);
		String modifiedVisualText = applyReplacements(visualText, arguments);
		BaseCalloutEvent call;
		if (Objects.equals(modifiedVisualText, visualText) && this.expiry == null) {
			call = new BasicCalloutEvent(
					modifiedCallText,
					modifiedVisualText);
		}
		else {
			call = new ParentedCalloutEvent<>(
					event,
					modifiedCallText,
					() -> applyReplacements(visualText, arguments),
					expiry,
					guiProvider);
		}
		if (handle != null) {
			call.setColorOverride(handle.getTextColorOverride().get());
		}
		return call;
	}

	public CalloutEvent getModified(Map<String, Object> arguments) {
		String callText;
		String visualText;
		if (handle == null) {
			log.warn("ModifiableCallout does not have handle yet ({})", description);
			callText = defaultTtsText;
			visualText = defaultVisualText;
		}
		else {
			callText = handle.getEffectiveTts();
			visualText = handle.getEffectiveText();
		}
		String modifiedCallText = applyReplacements(callText, arguments);
		String modifiedVisualText = applyReplacements(visualText, arguments);
		BaseCalloutEvent call;
		if (Objects.equals(modifiedVisualText, visualText)) {
			call = new BasicCalloutEvent(
					modifiedCallText,
					modifiedVisualText);
		}
		else {
			call = new DynamicCalloutEvent(
					modifiedCallText,
					() -> applyReplacements(visualText, arguments),
					defaultVisualHangTime
			);
		}
		if (handle != null) {
			call.setColorOverride(handle.getTextColorOverride().get());
		}
		return call;
	}

	private boolean shouldLogError() {
		errorCount++;
		if (errorCount < maxErrors) {
			return true;
		}
		else if (errorCount == maxErrors) {
			log.error("Hit the maximum number of logged errors for ModifiableCallout '{}', silencing future errors", description);
			return true;
		}
		else {
			return false;
		}
	}

	private Script compile(String input) {
		return interpreter.parse(input);
	}

	// TODO: there is a concurrency issue here. The TTS thread and visual callout processing thread can call this
	// at the same time.
	@Contract("null, _ -> null")
	public @Nullable String applyReplacements(@Nullable String input, Map<String, Object> replacements) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}

		synchronized (interpLock) {
			try {
				replacements.forEach((k, v) -> {
					try {
						interpreter.setVariable(k, v);
					}
					catch (Throwable e) {
						errorCount++;
						if (shouldLogError()) {
							log.error("Error setting variable in bsh", e);
						}
					}
				});
				return replacer.matcher(input).replaceAll(m -> {
					try {
						// TODO: scripts have a setBinding method. That might be easier.
						Script script = scriptCache.computeIfAbsent(m.group(1), this::compile);
						Object rawEval = script.run();
						if (rawEval == null) {
							return "null";
//						return m.group(0);
						}
						return singleReplacement(rawEval);
					}
					catch (Throwable e) {
						if (shouldLogError()) {
							log.error("Eval error for input '{}'", input, e);
						}
						return "Error";
					}
				});
			}
			finally {
				replacements.forEach((k, v) -> {
					try {
						interpreter.removeVariable(k);
					}
					catch (Throwable e) {
						if (shouldLogError()) {
							log.error("Error unsetting variable in bsh", e);
						}
					}
				});
			}
		}
	}

	// Default conversions
	@SuppressWarnings("unused")
	public static String singleReplacement(Object rawValue) {
		String value;
		if (rawValue instanceof String strVal) {
			value = strVal;
		}
		else if (rawValue instanceof XivCombatant cbt) {
			if (cbt.isThePlayer()) {
				value = "YOU";
			}
			else {
				value = cbt.getName();
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
		else {
			value = rawValue.toString();
		}
		return value;
	}


	/**
	 * Used for things like water stack in TEA or P2S where the callout is based on a buff time or castbar.
	 * <p>
	 * Just because something *can* be used with this method doesn't mean it should - many buff/castbar mechanics
	 * do not warrant this. e.g. if the initial cast merely tells you what you need to do, or if it is expected
	 * that the buff will.
	 *
	 * @param desc The description.
	 * @param text The base text. For the visual text, the duration will be appended in parenthesis.
	 *             e.g. "Water on You" will become "Water on You" (123.4) will be appended, and the timer will count
	 *             down.
	 * @return the ModifiableCallout
	 */
	public static <Y extends HasDuration> ModifiableCallout<Y> durationBasedCall(String desc, String text) {
		return new ModifiableCallout<>(desc, text, text + " ({event.getEstimatedRemainingDuration()})", hd -> hd.getEstimatedTimeSinceExpiry().compareTo(defaultLingerTime) > 0);
	}

	public static <Y extends HasDuration> ModifiableCallout<Y> durationBasedCallWithoutDurationText(String desc, String text) {
		return new ModifiableCallout<>(desc, text, text, hd -> hd.getEstimatedTimeSinceExpiry().compareTo(defaultLingerTime) > 0);
	}

	private static final Duration defaultLingerTime = Duration.of(3, ChronoUnit.SECONDS);

}
