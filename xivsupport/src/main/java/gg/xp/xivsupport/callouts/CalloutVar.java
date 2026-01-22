package gg.xp.xivsupport.callouts;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CalloutVar {
	private volatile CalloutVarHandle handle;

	private final String name;
	private final String defaultValue;
	private @Nullable String extendedDescription;

	public CalloutVar(String name, String defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public CalloutVar extendedDescription(String extendedDescription) {
		this.extendedDescription = extendedDescription;
		return this;
	}

	public void attachHandle(CalloutVarHandle handle) {
		this.handle = handle;
	}

	Object getValue() {
		CalloutVarHandle handle = this.handle;
		if (handle == null) {
			return defaultValue;
		}
		else {
			return handle.getValue();
		}
	}

	public String getName() {
		return name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
}
