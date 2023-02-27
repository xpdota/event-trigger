package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OmegaFirstSetAssignments extends BaseEvent {


	@Serial
	private static final long serialVersionUID = -9048799703682663091L;
	private final XivPlayerCharacter near;
	private final XivPlayerCharacter dist;
	private final List<XivPlayerCharacter> playersToMark;
	private final List<XivPlayerCharacter> leftovers;

	public OmegaFirstSetAssignments(XivPlayerCharacter near, XivPlayerCharacter dist, List<XivPlayerCharacter> playersToMark, List<XivPlayerCharacter> leftovers) {
		super();
		this.near = near;
		this.dist = dist;
		this.playersToMark = new ArrayList<>(playersToMark);
		this.leftovers = new ArrayList<>(leftovers);
	}

	public XivPlayerCharacter getNear() {
		return near;
	}

	public XivPlayerCharacter getDist() {
		return dist;
	}

	public List<XivPlayerCharacter> getPlayersToMark() {
		return Collections.unmodifiableList(playersToMark);
	}

	public List<XivPlayerCharacter> getLeftovers() {
		return Collections.unmodifiableList(leftovers);
	}
}
