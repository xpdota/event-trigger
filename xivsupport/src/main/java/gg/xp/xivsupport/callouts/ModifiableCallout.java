package gg.xp.xivsupport.callouts;

import bsh.EvalError;
import bsh.Interpreter;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.time.TimeUtils;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.DynamicCalloutEvent;
import gg.xp.xivsupport.speech.ParentedCalloutEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ModifiableCallout<X> {

	private static final Logger log = LoggerFactory.getLogger(ModifiableCallout.class);

	private final String description;
	private final String defaultTtsText;
	private final String defaultVisualText;
	private final Predicate<X> expiry;
	private final List<CalloutCondition> conditions;
	private final long defaultVisualHangTime;
	private int errorCount;
	private static final int maxErrors = 10;

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

	public ModifiableCallout(String description, String text, List<CalloutCondition> conditions) {
		this.description = description;
		defaultTtsText = text;
		defaultVisualText = text;
		this.conditions = new ArrayList<>(conditions);
		defaultVisualHangTime = 5000L;
		Instant defaultExpiryAt = TimeUtils.now().plus(defaultHangDuration);
		this.expiry = eventItem -> {
			if (eventItem instanceof BaseEvent be) {
				return be.getEffectiveTimeSince().compareTo(defaultHangDuration) > 0;
			}
			else {
				return defaultExpiryAt.isBefore(Instant.now());
			}
		};
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
		if (Objects.equals(modifiedVisualText, visualText) && this.expiry == null) {
			return new BasicCalloutEvent(
					modifiedCallText,
					modifiedVisualText);
		}
		else {
			return new ParentedCalloutEvent<>(
					event,
					modifiedCallText,
					() -> applyReplacements(visualText, arguments),
					expiry
			);
		}
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
		if (Objects.equals(modifiedVisualText, visualText)) {
			return new BasicCalloutEvent(
					modifiedCallText,
					modifiedVisualText);
		}
		else {
			return new DynamicCalloutEvent(
					modifiedCallText,
					() -> applyReplacements(visualText, arguments),
					defaultVisualHangTime
			);
		}
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

	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");
	private static final ThreadLocal<Interpreter> interpreterTl = ThreadLocal.withInitial(Interpreter::new);

	@Contract("null, _ -> null")
	public @Nullable String applyReplacements(@Nullable String input, Map<String, Object> replacements) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}

		/*
		TODO: this doesn't present a performance challenge *yet*, but may if I decide to expand this to more areas
		At first, this was using JShell which was horribly slow and overly complicated.
		Beanshell seems to be a lot faster than JShell, but still isn't all that fast.
		Options:
		1. Look into Groovy. It can compile cached scripts and is more advanced in general. Shouldn't miss out on as
		   many newer Java features.
		2. Rather than evaluating every time, just have Beanshell crank out a lambda one time, then call that lambda.
		   This should theoretically improve performance significantly, but that should be tested.

		Performance here isn't an automatic dealbreaker, since text overlay updates happen on their own thread, but
		may still be worth looking into at some point.
		 */
		Interpreter interpreter = interpreterTl.get();
		try {
			replacements.forEach((k, v) -> {
				try {
					interpreter.set(k, v);
				}
				catch (EvalError e) {
					errorCount++;
					if (shouldLogError()) {
						log.error("Error setting variable in bsh", e);
					}
				}
			});
			return replacer.matcher(input).replaceAll(m -> {
				try {
					Object rawEval = interpreter.eval(getClass().getCanonicalName() + ".singleReplacement(" + m.group(1) + ")");
					if (rawEval == null) {
						return m.group(0);
					}
					return rawEval.toString();
				}
				catch (EvalError e) {
					if (shouldLogError()) {
						log.error("Eval error", e);
					}
					return "Error";
				}
			});
		}
		finally {
			replacements.forEach((k, v) -> {
				try {
					interpreter.unset(k);
				}
				catch (EvalError e) {
					if (shouldLogError()) {
						log.error("Error unsetting variable in bsh", e);
					}
				}
			});
		}
	}

	// Default conversions
	@SuppressWarnings("unused")
	public static String singleReplacement(Object rawValue) {
		String value;
		if (rawValue instanceof String) {
			value = (String) rawValue;
		}
		else if (rawValue instanceof XivCombatant cbt) {
			if (cbt.isThePlayer()) {
				value = "YOU";
			}
			else {
				value = cbt.getName();
			}
		}
		else if (rawValue instanceof Duration dur) {
			if (dur.isZero()) {
				return "NOW";
			}
			return String.format("%.01f", dur.toMillis() / 1000.0);
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
	public static <Y extends Event & HasDuration> ModifiableCallout<Y> durationBasedCall(String desc, String text) {
		return new ModifiableCallout<>(desc, text, text + " ({event.getEstimatedRemainingDuration()})", hd -> hd.getEstimatedTimeSinceExpiry().compareTo(defaultLingerTime) > 0);
	}

	private static final Duration defaultLingerTime = Duration.of(3, ChronoUnit.SECONDS);

}
