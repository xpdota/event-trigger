package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivsupport.events.actlines.parsers.Line20Parser;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.time.Duration;

/**
 * Represents an ability cast beginning i.e. castbar has appeared
 */
public class AbilityCastStart extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasDuration {
	@Serial
	private static final long serialVersionUID = -8156458501097189982L;
	private final XivAbility ability;
	private final XivCombatant source;
	private final XivCombatant target;
	private final Duration duration;
	private final Duration serverCastDuration;

	public AbilityCastStart(XivAbility ability, XivCombatant source, XivCombatant target, double serverCastDuration) {
		this.ability = ability;
		this.source = source;
		this.target = target;
		this.serverCastDuration = Duration.ofMillis((long) (serverCastDuration * 1000.0));
		ActionInfo ai = ActionLibrary.forId(ability.getId());
		if (ai == null || ai.isPlayerAbility()) {
			duration = this.serverCastDuration;
		}
		else {
			duration = Duration.ofMillis(ai.castTimeRaw() * 100);
		}
	}

	@Override
	public XivAbility getAbility() {
		return ability;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public Duration getInitialDuration() {
		return duration;
	}

	public Duration getServerCastDuration() {
		return serverCastDuration;
	}
}
