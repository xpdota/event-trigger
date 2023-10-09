package gg.xp.xivsupport.speech;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
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


	@Override
	public String getOriginDescription() {
		if (calloutDesc == null && calloutField == null) {
			return "Unknown";
		}
		else {
			StringBuilder out = new StringBuilder();
			if (calloutField != null) {
				out.append(calloutField.getDeclaringClass().getSimpleName()).append('.').append(calloutField.getName()).append('\n');
			}
			if (calloutDesc != null) {
				out.append(calloutDesc).append('\n');
			}
			return out.toString();
		}
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
}
