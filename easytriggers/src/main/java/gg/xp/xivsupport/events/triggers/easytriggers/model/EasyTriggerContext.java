package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EasyTriggerContext {

	private Map<String, Object> extraVariables;

	public void addVariable(String key, Object value) {
		if (extraVariables == null) {
			extraVariables = new HashMap<>();
		}
		extraVariables.put(key, value);
	}

	public Map<String, Object> getExtraVariables() {
		if (extraVariables == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(extraVariables);
	}

}
