package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.util.Collections;
import java.util.Map;

public abstract class MechAssignmentEvent<K> extends BaseEvent {
	private final Map<K, XivPlayerCharacter> assignments;

	protected MechAssignmentEvent(Map<K, XivPlayerCharacter> assignments) {
		this.assignments = assignments;
	}

	public K forPlayer(XivPlayerCharacter xpc) {
		return assignments.entrySet().stream()
				.filter(e -> e.getValue().equals(xpc))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public Map<K, XivPlayerCharacter> getAssignments() {
		return Collections.unmodifiableMap(assignments);
	}

}
