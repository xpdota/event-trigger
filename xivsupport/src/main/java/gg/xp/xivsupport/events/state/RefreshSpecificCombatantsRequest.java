package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

@SystemEvent
public class RefreshSpecificCombatantsRequest extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -8788456299294198856L;

	private final Collection<Long> combatants;

	public RefreshSpecificCombatantsRequest(Collection<Long> combatants) {
		this.combatants = combatants;
	}

	public Collection<Long> getCombatants() {
		return Collections.unmodifiableCollection(combatants);
	}

	@Override
	public String getPrimaryValue() {
		return "Refresh Combatants " + combatants;
	}
}
