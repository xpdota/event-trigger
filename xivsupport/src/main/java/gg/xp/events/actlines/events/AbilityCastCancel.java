package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivCombatant;

public class AbilityCastCancel extends BaseEvent implements HasSourceEntity {

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

	public XivAbility getAbility() {
		return ability;
	}

	public String getReason() {
		return reason;
	}
}
