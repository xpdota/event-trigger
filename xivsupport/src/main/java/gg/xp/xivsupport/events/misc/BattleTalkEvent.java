package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class BattleTalkEvent extends BaseEvent implements HasSourceEntity {

	@Serial
	private static final long serialVersionUID = -7341616369784484447L;
	private final XivCombatant source;
	private final long instanceContentTextId;

	public BattleTalkEvent(XivCombatant source, long instanceContentTextId) {
		this.source = source;
		this.instanceContentTextId = instanceContentTextId;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	public long getInstanceContentTextId() {
		return instanceContentTextId;
	}
}
