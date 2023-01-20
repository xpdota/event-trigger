package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

/**
 * Represents a headmarker. Note that this does not have any correction applied, i.e. for offset markers
 */
public class HeadMarkerEvent extends BaseEvent implements HasTargetEntity, HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -413687601479469145L;
	private final XivCombatant target;
	private final long markerId;
	private int pullOffset;

	public HeadMarkerEvent(XivCombatant target, long markerId) {
		this.target = target;
		this.markerId = markerId;
	}

	/**
	 * @return The target of this headmarker
	 */
	@Override
	public XivCombatant getTarget() {
		return target;
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
}
