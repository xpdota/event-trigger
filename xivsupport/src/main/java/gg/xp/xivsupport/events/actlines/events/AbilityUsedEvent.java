package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class AbilityUsedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects {

	@Serial
	private static final long serialVersionUID = -4539070760062288496L;
	private final XivAbility ability;
	private final XivCombatant caster;
	private final XivCombatant target;
	private final List<AbilityEffect> effects;
	private final long sequenceId;
	private final long targetIndex;
	private final long numberOfTargets;

	public AbilityUsedEvent(XivAbility ability, XivCombatant caster, XivCombatant target, List<AbilityEffect> effects, long sequenceId, long targetIndex, long numberOfTargets) {
		this.ability = ability;
		this.caster = caster;
		this.target = target;
		this.effects = effects;
		this.sequenceId = sequenceId;
		this.targetIndex = targetIndex;
		this.numberOfTargets = numberOfTargets;
	}

	@Override
	public XivAbility getAbility() {
		return ability;
	}

	@Override
	public XivCombatant getSource() {
		return caster;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public List<AbilityEffect> getEffects() {
		return Collections.unmodifiableList(effects);
	}

	public long getSequenceId() {
		return sequenceId;
	}

	public long getTargetIndex() {
		return targetIndex;
	}

	public long getNumberOfTargets() {
		return numberOfTargets;
	}

	public boolean isLastTarget() {
		return targetIndex >= numberOfTargets - 1;
	}
}
