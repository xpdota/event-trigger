package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.List;

public class AbilityResolvedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects {
	private static final long serialVersionUID = 4043588325843768440L;
	private final AbilityUsedEvent originalEvent;
	private final long sequenceId;

	public AbilityResolvedEvent(AbilityUsedEvent originalEvent) {
		this.originalEvent = originalEvent;
		this.sequenceId = originalEvent.getSequenceId();
	}

	@Override
	public XivAbility getAbility() {
		return originalEvent.getAbility();
	}

	@Override
	public XivCombatant getSource() {
		return originalEvent.getSource();
	}

	@Override
	public XivCombatant getTarget() {
		return originalEvent.getTarget();
	}

	@Override
	public List<AbilityEffect> getEffects() {
		return originalEvent.getEffects();
	}

	public long getSequenceId() {
		return sequenceId;
	}
}
