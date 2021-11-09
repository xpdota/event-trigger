package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

public class EntityKilledEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {
	private static final long serialVersionUID = 2379855663603121468L;
	private final XivEntity source;
	private final XivEntity target;

	public EntityKilledEvent(XivEntity source, XivEntity target) {
		this.source = source;
		this.target = target;
	}

	public XivEntity getSource() {
		return source;
	}

	public XivEntity getTarget() {
		return target;
	}
}
