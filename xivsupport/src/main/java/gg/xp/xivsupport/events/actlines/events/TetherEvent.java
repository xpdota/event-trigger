package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class TetherEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {

	@Serial
	private static final long serialVersionUID = 7043671273943254143L;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long id;

	public TetherEvent(XivCombatant source, XivCombatant target, long id) {
		this.source = source;
		this.target = target;
		this.id = id;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public long getId() {
		return id;
	}
}
