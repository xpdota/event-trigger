package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class NpcYellEvent extends BaseEvent implements HasSourceEntity, HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = 765355305124585263L;
	private final XivCombatant source;
	private final NpcYellInfo yell;

	// TODO: integrate RSV here
	public NpcYellEvent(XivCombatant source, NpcYellInfo yell) {
		this.source = source;
		this.yell = yell;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	public NpcYellInfo getYell() {
		return yell;
	}

	@Override
	public String toString() {
		return "NpcYellEvent(%s)".formatted(getPrimaryValue());
	}

	@Override
	public String getPrimaryValue() {
		return "%s: %s".formatted(yell.id(), yell.text());
	}
}
