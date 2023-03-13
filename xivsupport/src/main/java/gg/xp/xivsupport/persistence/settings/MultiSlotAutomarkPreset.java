package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MultiSlotAutomarkPreset<X extends Enum<X>> {

	private final String name;
	private final Map<X, MarkerSign> presetData;

	public MultiSlotAutomarkPreset(String name, Map<X, MarkerSign> presetData) {
		this.name = name;
		this.presetData = new EnumMap<>(presetData);
	}

	public String getName() {
		return name;
	}

	public Map<X, MarkerSign> getPresetData() {
		return Collections.unmodifiableMap(presetData);
	}
}
