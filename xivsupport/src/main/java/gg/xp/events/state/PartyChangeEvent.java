package gg.xp.events.state;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivPlayerCharacter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartyChangeEvent extends BaseEvent {
	private final List<XivPlayerCharacter> members;

	public PartyChangeEvent(List<XivPlayerCharacter> members) {
		this.members = new ArrayList<>(members);
	}

	public List<XivPlayerCharacter> getMembers() {
		return Collections.unmodifiableList(members);
	}
}
