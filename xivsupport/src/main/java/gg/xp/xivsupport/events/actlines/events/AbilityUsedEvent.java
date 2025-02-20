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
public class AbilityUsedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects, HasTargetIndex, HasCastPrecursor {

	@Serial
	private static final long serialVersionUID = -4539070760062288496L;
	private final XivAbility ability;
	private final XivCombatant caster;
	private final XivCombatant target;
	private final List<AbilityEffect> effects;
	private final long sequenceId;
	private final long targetIndex;
	private final long numberOfTargets;
	private final @Nullable Duration animationLock;
	private @Nullable DescribesCastLocation<AbilityUsedEvent> locationInfo;
	private @Nullable AbilityCastStart precursor;

	public AbilityUsedEvent(XivAbility ability, XivCombatant caster, XivCombatant target, List<AbilityEffect> effects, long sequenceId, long targetIndex, long numberOfTargets) {
		this(ability, caster, target, effects, sequenceId, targetIndex, numberOfTargets, null);
	}

	public AbilityUsedEvent(XivAbility ability, XivCombatant caster, XivCombatant target, List<AbilityEffect> effects, long sequenceId, long targetIndex, long numberOfTargets, @Nullable Duration animationLock) {
		this.ability = ability;
		this.caster = caster;
		this.target = target;
		this.effects = effects;
		this.sequenceId = sequenceId;
		this.targetIndex = targetIndex;
		this.numberOfTargets = numberOfTargets;
		this.animationLock = animationLock;
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

	@Override
	public @Nullable AbilityCastStart getPrecursor() {
		return precursor;
	}

	@Override
	public void setPrecursor(@NotNull AbilityCastStart precursor) {
		this.precursor = precursor;
	}

	/**
	 * Get the animation lock.
	 *
	 * @return The animation lock, if one has been set. Null if using an old ACT version that does not support this.
	 */
	public @Nullable Duration getAnimationLock() {
		return animationLock;
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
