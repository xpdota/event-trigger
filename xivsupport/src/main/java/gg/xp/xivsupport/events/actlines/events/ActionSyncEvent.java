package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivCombatant;


@SystemEvent
public class ActionSyncEvent extends BaseEvent implements HasTargetEntity {

	private static final long serialVersionUID = -8477450928742265164L;
	private final XivCombatant entity;
	private final long sequenceId;

	public ActionSyncEvent(XivCombatant entity, long sequenceId) {
		this.entity = entity;
		this.sequenceId = sequenceId;
	}

	@Override
	public XivCombatant getTarget() {
		return entity;
	}

	public long getSequenceId() {
		return sequenceId;
	}


}
