package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivEntity;

public class AbilityCastCancel extends BaseEvent implements HasSourceEntity {

	private final XivEntity source;
	private final XivAbility ability;
	private final String reason;

	public AbilityCastCancel(XivEntity source, XivAbility ability, String reason) {
		this.source = source;
		this.ability = ability;
		this.reason = reason;
	}

	@Override
	public XivEntity getSource() {
		return source;
	}

	public XivAbility getAbility() {
		return ability;
	}

	public String getReason() {
		return reason;
	}
}
