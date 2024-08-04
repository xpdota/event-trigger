package gg.xp.xivsupport.speech;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifiableCalloutTraceInfo implements CalloutTraceInfo {

	private final @Nullable Field calloutField;
	private final @Nullable String calloutDesc;
	private final String rawTts;
	private final String rawText;
	private final Map<String, Object> args;

	public ModifiableCalloutTraceInfo(RawModifiedCallout<?> raw) {
		ModifiedCalloutHandle handle = raw.getHandle();
		if (handle == null) {
			calloutField = null;
			calloutDesc = null;
		}
		else {
			calloutDesc = handle.getDescription();
			calloutField = handle.getField();
		}
		this.rawTts = raw.getTts();
		this.rawText = raw.getText();
		this.args = Collections.unmodifiableMap(new HashMap<>(raw.getArguments()));
	}

	private String getOriginDescriptionInt(boolean multiline) {
		if (calloutDesc == null && calloutField == null) {
			return "Unknown";
		}
		else {
			List<String> items = new ArrayList<>();
			if (calloutField != null) {
				items.add("%s.%s".formatted(calloutField.getDeclaringClass().getSimpleName(), calloutField.getName()));
			}
			if (calloutDesc != null) {
				items.add(calloutDesc);
			}
			return String.join(multiline ? "\n" : ": ", items);
		}

	}

	@Override
	public String getOriginDescription() {
		return getOriginDescriptionInt(true);
	}

	@Override
	public String getRawTts() {
		return rawTts;
	}

	@Override
	public String getRawText() {
		return rawText;
	}

	@Override
	public Map<String, Object> getArgs() {
		return args;
	}

	public Field getCalloutField() {
		return calloutField;
	}

	@Override
	public String toString() {
		return "ModifiableCalloutTraceInfo(%s)".formatted(this.getOriginDescriptionInt(false));
	}
}
