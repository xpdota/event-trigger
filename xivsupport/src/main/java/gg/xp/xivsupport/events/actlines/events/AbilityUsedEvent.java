package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Represents an ability snapshotting
 */
public class AbilityUsedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects, HasTargetIndex {

	@Serial
	private static final long serialVersionUID = -4539070760062288496L;
	private final XivAbility ability;
	private final XivCombatant caster;
	private final XivCombatant target;
	private final List<AbilityEffect> effects;
	private final long sequenceId;
	private final long targetIndex;
	private final long numberOfTargets;
	private @Nullable Duration animationLock;
	private @Nullable DescribesCastLocation<AbilityUsedEvent> locationInfo;

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

	@Override
	public long getTargetIndex() {
		return targetIndex;
	}

	@Override
	public long getNumberOfTargets() {
		return numberOfTargets;
	}

	@Override
	public boolean isFirstTarget() {
		return targetIndex == 0;
	}

	@Override
	public boolean isLastTarget() {
		return targetIndex >= numberOfTargets - 1;
	}

	public @Nullable DescribesCastLocation<AbilityUsedEvent> getLocationInfo() {
		return locationInfo;
	}

	public void setLocationInfo(@NotNull DescribesCastLocation<AbilityUsedEvent> locationInfo) {
		this.locationInfo = locationInfo;
	}

	/**
	 * Get the animation lock.
	 * <p>
	 * Note that this is expected to be populated *after* this event is emitted, as this data
	 * is not known at the time of the original event. In addition, it may never be present,
	 * such as if you are importing a non-OP log, or an fflogs import.
	 *
	 * @see AnimationLockEvent
	 * @return The animation lock, if one has been set.
	 */
	public @Nullable Duration getAnimationLock() {
		return animationLock;
	}

	public void setAnimationLock(@Nullable Duration animationLock) {
		this.animationLock = animationLock;
	}

	@Override
	public String toString() {
		return "AbilityUsedEvent{" +
				"ability=" + ability +
				", caster=" + caster +
				", target=" + target +
				", effects=" + effects +
				", sequenceId=" + sequenceId +
				", targetIndex=" + targetIndex +
				", numberOfTargets=" + numberOfTargets +
				'}';
	}
}
