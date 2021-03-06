package gg.xp.xivsupport.events.triggers.jails;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnsortedTitanJailsSolvedEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -358330710284359399L;
	private final List<XivPlayerCharacter> jailedPlayers;

	public UnsortedTitanJailsSolvedEvent(List<XivPlayerCharacter> jailedPlayers) {
		this.jailedPlayers = new ArrayList<>(jailedPlayers);
	}

	public List<XivPlayerCharacter> getJailedPlayers() {
		return Collections.unmodifiableList(jailedPlayers);
	}
}
