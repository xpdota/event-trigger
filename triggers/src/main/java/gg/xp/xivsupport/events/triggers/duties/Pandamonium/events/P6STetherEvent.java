package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.models.XivCombatant;

public class P6STetherEvent extends BaseEvent implements HasPrimaryValue {
	private final XivCombatant source;
	private final XivCombatant target;

	public P6STetherEvent(XivCombatant source, XivCombatant target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("Tether %s, %s", source, target);
	}
}
