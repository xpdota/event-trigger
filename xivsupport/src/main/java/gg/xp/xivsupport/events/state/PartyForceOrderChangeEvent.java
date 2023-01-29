package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartyForceOrderChangeEvent extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -4569965378704611459L;
	private final @Nullable List<Long> members;

	public PartyForceOrderChangeEvent(@Nullable List<Long> members) {
		this.members = members == null ? null : new ArrayList<>(members);
	}

	public @Nullable List<Long> getMembers() {
		return members == null ? null : Collections.unmodifiableList(members);
	}
}
