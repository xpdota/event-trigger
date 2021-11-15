package gg.xp.events.jails;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivPlayerCharacter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinalTitanJailsSolvedEvent extends BaseEvent {
	private static final long serialVersionUID = 3966119066157989985L;
	private final List<XivPlayerCharacter> jailedPlayers;

	public FinalTitanJailsSolvedEvent(List<XivPlayerCharacter> jailedPlayers) {
		this.jailedPlayers = new ArrayList<>(jailedPlayers);
	}

	public List<XivPlayerCharacter> getJailedPlayers() {
		return Collections.unmodifiableList(jailedPlayers);
	}
}
