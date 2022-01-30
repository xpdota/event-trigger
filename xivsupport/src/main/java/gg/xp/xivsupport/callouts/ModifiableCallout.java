package gg.xp.xivsupport.callouts;

import bsh.EvalError;
import bsh.Interpreter;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.DynamicCalloutEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class ModifiableCallout {

	private static final Logger log = LoggerFactory.getLogger(ModifiableCallout.class);

	private final String description;
	private final String defaultTtsText;
	private final String defaultVisualText;
	private final List<CalloutCondition> conditions;
	private final long defaultVisualHangTime;

	private volatile ModifiedCalloutHandle handle;

	public ModifiableCallout(String description, String text) {
		this(description, text, Collections.emptyList());
	}

	public ModifiableCallout(String description, String text, List<CalloutCondition> conditions) {
		this.description = description;
		defaultTtsText = text;
		defaultVisualText = text;
		this.conditions = new ArrayList<>(conditions);
		defaultVisualHangTime = 5000L;
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

	public CalloutEvent getModified(Event event) {
		return getModified(event, Collections.emptyMap());
	}

	public CalloutEvent getModified(Event event, Map<String, Object> arguments) {
		// TODO
		Map<String, Object> map = new HashMap<>(arguments);
		map.put("event", event);
		return getModified(map);
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
					5000L
			);
		}
	}

	private static final Pattern replacer = Pattern.compile("\\{(.+?)}");

	@Contract("null, _ -> null")
	public @Nullable String applyReplacements(@Nullable String input, Map<String, Object> replacements) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}
		Interpreter interpreter = new Interpreter();
		replacements.forEach((k, v) -> {
			try {
				interpreter.set(k, v);
			}
			catch (EvalError e) {
				log.error("Error setting variable in bsh", e);
			}
		});
		return replacer.matcher(input).replaceAll(m -> {
			try {
				return interpreter.eval(m.group(1)).toString();
			}
			catch (EvalError e) {
				log.error("Eval error", e);
				return "Error";
			}
		});
	}

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
		else {
			value = rawValue.toString();
		}
		return value;
	}
}
