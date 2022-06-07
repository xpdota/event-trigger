package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.models.XivPlayerCharacter;

public class SpecificAutoMarkRequest extends BaseEvent implements HasPrimaryValue {

	private final XivPlayerCharacter playerToMark;
	private final MarkerSign marker;

	public SpecificAutoMarkRequest(XivPlayerCharacter playerToMark, MarkerSign marker) {
		this.playerToMark = playerToMark;
		this.marker = marker;
	}

	public XivPlayerCharacter getPlayerToMark() {
		return playerToMark;
	}

	public MarkerSign getMarker() {
		return marker;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("'%s' on %s", marker.getCommand(), playerToMark.getName());
	}
}
