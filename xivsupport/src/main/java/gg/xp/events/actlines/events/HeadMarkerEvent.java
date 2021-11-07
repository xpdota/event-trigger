package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

public class HeadMarkerEvent extends BaseEvent implements HasTargetEntity {

	private final XivEntity target;
	private final long markerId;

	public HeadMarkerEvent(XivEntity target, long markerId) {
		this.target = target;
		this.markerId = markerId;
	}

	@Override
	public XivEntity getTarget() {
		return target;
	}

	public long getMarkerId() {
		return markerId;
	}
}
