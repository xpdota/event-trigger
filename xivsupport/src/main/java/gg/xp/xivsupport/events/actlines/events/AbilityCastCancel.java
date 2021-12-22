package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

public class AbilityCastCancel extends BaseEvent implements HasSourceEntity, HasAbility {

	@Serial
	private static final long serialVersionUID = -5704173639583049362L;
	private final XivCombatant source;
	private final XivAbility ability;
	private final String reason;

	public AbilityCastCancel(XivCombatant source, XivAbility ability, String reason) {
		this.source = source;
		this.ability = ability;
		this.reason = reason;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivAbility getAbility() {
		return ability;
	}

	public String getReason() {
		return reason;
	}
}
