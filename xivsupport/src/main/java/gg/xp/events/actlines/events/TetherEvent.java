package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;

public class TetherEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {

	private static final long serialVersionUID = 7043671273943254143L;
	private final XivEntity source;
	private final XivEntity target;
	private final long id;

	public TetherEvent(XivEntity source, XivEntity target, long id) {
		this.source = source;
		this.target = target;
		this.id = id;
	}

	public XivEntity getSource() {
		return source;
	}

	public XivEntity getTarget() {
		return target;
	}

	public long getId() {
		return id;
	}
}
