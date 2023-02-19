package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class MechAssignmentEvent<K> extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -5975570546647941261L;
	private final Map<K, XivPlayerCharacter> assignments;

	protected MechAssignmentEvent(Map<K, XivPlayerCharacter> assignments) {
		this.assignments = assignments;
	}

	public @Nullable K forPlayer(XivPlayerCharacter xpc) {
		return assignments.entrySet().stream()
				.filter(e -> e.getValue().equals(xpc))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public @Nullable K localPlayerAssignment() {
		return assignments.entrySet().stream()
				.filter(e -> e.getValue().isThePlayer())
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public Map<K, XivPlayerCharacter> getAssignments() {
		return Collections.unmodifiableMap(assignments);
	}

	public @Nullable XivPlayerCharacter getPlayerForAssignment(K assignment) {
		return assignments.get(assignment);
	}

	@Override
	public String getPrimaryValue() {
		return assignments.entrySet().stream()
				.map(e -> String.format("%s: %s", e.getKey(), e.getValue().getName()))
				.collect(Collectors.joining("; "));
	}
}
