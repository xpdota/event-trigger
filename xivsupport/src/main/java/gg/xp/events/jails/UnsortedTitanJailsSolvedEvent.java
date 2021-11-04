package gg.xp.events.jails;

import gg.xp.events.Event;
import gg.xp.events.XivEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnsortedTitanJailsSolvedEvent implements Event {

	private final List<XivEntity> jailedPlayers;

	public UnsortedTitanJailsSolvedEvent(List<XivEntity> jailedPlayers) {
		this.jailedPlayers = new ArrayList<>(jailedPlayers);
	}

	public List<XivEntity> getJailedPlayers() {
		return Collections.unmodifiableList(jailedPlayers);
	}
}
