package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a headmarker. Note that this does not have any correction applied, i.e. for offset markers
 */
public class HeadMarkerEvent extends BaseEvent implements HasTargetEntity, HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -413687601479469145L;
	private final XivCombatant target;
	private final long markerId;
	private final @Nullable XivCombatant secondaryTarget;
	private int pullOffset;

	public HeadMarkerEvent(XivCombatant target, long markerId) {
		this(target, markerId, null);
	}

	public HeadMarkerEvent(XivCombatant target, long markerId, @Nullable XivCombatant secondaryTarget) {
		this.target = target;
		this.markerId = markerId;
		this.secondaryTarget = secondaryTarget;
	}

	/**
	 * @return The target of this headmarker
	 */
	@Override
	public XivCombatant getTarget() {
		return target;
	}

	/**
	 * @return The "secondary" target. It is theorized that this is used by line stack and similar markers to make
	 * the marker face the correct angle.
	 */
	public @Nullable XivCombatant getSecondaryTarget() {
		return secondaryTarget;
	}
	// TODO: expose this on UI

	public boolean eitherTargetMatches(XivCombatant cbt) {
		return Objects.equals(cbt, target) || Objects.equals(cbt, secondaryTarget);
	}

	public List<XivCombatant> getTargets() {
		if (secondaryTarget == null) {
			return List.of(target);
		}
		return List.of(target, secondaryTarget);
	}

	public @Nullable XivCombatant getTargetMatching(Predicate<XivCombatant> targetCondition) {
		if (targetCondition.test(target)) {
			return target;
		}
		if (secondaryTarget != null && targetCondition.test(secondaryTarget)) {
			return secondaryTarget;
		}
		return null;
	}

	public boolean eitherTargetMatches(Predicate<XivCombatant> targetCondition) {
		return targetCondition.test(target) || (secondaryTarget != null && targetCondition.test(secondaryTarget));
	}


	/**
	 * @return The ID of this headmarker
	 */
	public long getMarkerId() {
		return markerId;
	}

	@Override
	public String toString() {
		return String.format("HeadMarkerEvent(%s on %s, offset %s)", markerId, target, pullOffset);
	}

	/**
	 * @return The ID of this headmarker relative to the beginning of the pull
	 */
	public int getMarkerOffset() {
		return pullOffset;
	}

	public void setPullOffset(int pullOffset) {
		this.pullOffset = pullOffset;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("HM %d (0x%X), offset %s%s", markerId, markerId, pullOffset >= 0 ? "+" : "-", Math.abs(pullOffset));
	}

	public boolean markerIdMatches(long... expected) {
		long id = markerId;
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
	}
}
