package gg.xp.events.jails;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinalTitanJailsSolvedEvent extends BaseEvent {
	private final List<XivEntity> jailedPlayers;

	public FinalTitanJailsSolvedEvent(List<XivEntity> jailedPlayers) {
		this.jailedPlayers = new ArrayList<>(jailedPlayers);
	}

	public List<XivEntity> getJailedPlayers() {
		return Collections.unmodifiableList(jailedPlayers);
	}
}
