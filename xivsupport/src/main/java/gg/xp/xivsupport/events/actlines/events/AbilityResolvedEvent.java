package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Instant;
import java.util.List;

/**
 * Represents an ability actual taking effect (as opposed to snapshotting)
 */
public class AbilityResolvedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects, HasTargetIndex, HasOptionalDelay {
	@Serial
	private static final long serialVersionUID = 4043588325843768440L;
	private final AbilityUsedEvent originalEvent;
	private final long sequenceId;
	private final XivCombatant source;
	private final XivCombatant target;

	public AbilityResolvedEvent(AbilityUsedEvent originalEvent) {
		this.originalEvent = originalEvent;
		this.sequenceId = originalEvent.getSequenceId();
		this.source = originalEvent.getSource();
		this.target = originalEvent.getTarget();
	}
	public AbilityResolvedEvent(AbilityUsedEvent originalEvent, XivCombatant source, XivCombatant target) {
		this.originalEvent = originalEvent;
		this.sequenceId = originalEvent.getSequenceId();
		this.source = source;
		this.target = target;
	}


	@Override
	public XivAbility getAbility() {
		return originalEvent.getAbility();
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
	public List<AbilityEffect> getEffects() {
		return originalEvent.getEffects();
	}

	public long getSequenceId() {
		return sequenceId;
	}

	@Override
	public long getTargetIndex() {
		return originalEvent.getTargetIndex();
	}

	@Override
	public long getNumberOfTargets() {
		return originalEvent.getNumberOfTargets();
	}

	@Override
	public @Nullable Instant getPrecursorHappenedAt() {
		return originalEvent.getEffectiveHappenedAt();
	}
}
