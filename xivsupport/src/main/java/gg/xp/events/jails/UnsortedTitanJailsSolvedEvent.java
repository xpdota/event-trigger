package gg.xp.events.jails;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnsortedTitanJailsSolvedEvent extends BaseEvent {

	private static final long serialVersionUID = -358330710284359399L;
	private final List<XivPlayerCharacter> jailedPlayers;

	public UnsortedTitanJailsSolvedEvent(List<XivPlayerCharacter> jailedPlayers) {
		this.jailedPlayers = new ArrayList<>(jailedPlayers);
	}

	public List<XivPlayerCharacter> getJailedPlayers() {
		return Collections.unmodifiableList(jailedPlayers);
	}
}
