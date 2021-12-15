package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

	public CalloutEvent getModified(Map<String, Object> arguments) {
		String callText;
		String visualText;
		if (handle == null) {
			log.warn("ModifiableCallout does not have handle yet ({})", description);
			callText = defaultTtsText;
			visualText = defaultVisualText;
		}
		else if (handle.isEffectivelyEnabled()) {
			callText = handle.getEnableTts().get() ? handle.getTtsSetting().get() : null;
			visualText = handle.getEnableText().get() ? handle.getTextSetting().get() : null;
		}
		else {
			callText = null;
			visualText = null;
		}
		callText = applyReplacements(callText, arguments);
		visualText = applyReplacements(visualText, arguments);
		return new CalloutEvent(
				callText,
				visualText);
	}

	@Contract("null, _ -> null")
	public static @Nullable String applyReplacements(@Nullable String input, Map<String, Object> replacements) {
		if (input == null) {
			return null;
		}
		for (Map.Entry<String, Object> entry : replacements.entrySet()) {
			String key = entry.getKey();
			Object rawValue = entry.getValue();
			String value;
			if (rawValue instanceof String) {
				value = (String) rawValue;
			}
			else if (rawValue instanceof XivCombatant) {
				XivCombatant cbt = (XivCombatant) rawValue;
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
			String searchString = String.format("\\{%s\\}", key);
			input = input.replaceAll(searchString, value);
		}
		return input;
	}
}
