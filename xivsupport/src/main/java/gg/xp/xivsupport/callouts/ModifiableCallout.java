package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.DynamicCalloutEvent;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;
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

	// Not stupid if it works, probably
	// This is the easiest (only?) way to pass variables into a jshell instance.
	private static final JShell shell = JShell.builder().executionEngine(new LocalExecutionControlProvider(), Collections.emptyMap()).build();
	private static final Object lock = new Object();
	public static final Map<String, Object> context = new HashMap<>();

	Pattern replacer = Pattern.compile("\\{(.+?)}");

	@Contract("null, _ -> null")
	public @Nullable String applyReplacements(@Nullable String input, Map<String, Object> replacements) {
		if (input == null) {
			return null;
		}
		if (!input.contains("{")) {
			return input;
		}
		synchronized (lock) {
			try {
				context.clear();
				String thisClassName = getClass().getCanonicalName();
				shell.eval(String.format("Map<String, Object> context = %s.context ;", thisClassName));
				replacements.forEach((k, v) -> {
					context.put(k, v);
					String canonicalName = v.getClass().getCanonicalName();
					if (canonicalName == null) {
						// Fallback
						canonicalName = "Object";
					}
					shell.eval(String.format("%s %s = (%s) %s.context.get(\"%s\") ;", canonicalName, k, canonicalName, thisClassName, k));
				});
				return replacer.matcher(input).replaceAll(m -> {
					List<SnippetEvent> snippets = shell.eval(String.format("%s.singleReplacement(%s)", thisClassName, m.group(1)));
					// TODO: improve logging
					String value = snippets.get(snippets.size() - 1).value();
					// If null, leave it untouched. e.g. '{target}' would still be literally '{target}'
					value = value == null ? m.group(0) : value;
					if (value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length() - 1);
					}
					return value;
				});
			}
			finally {
				shell.snippets().forEach(shell::drop);
			}
		}
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
