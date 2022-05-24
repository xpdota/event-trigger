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

	public HeadMarkerEvent(XivCombatant target, long markerId) {
		this.target = target;
		this.markerId = markerId;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public long getMarkerId() {
		return markerId;
	}

	@Override
	public String toString() {
		return String.format("HeadMarkerEvent(%s on %s)", markerId, target);
	}


	@Override
	public String getPrimaryValue() {
		return String.format("HM %d", markerId);
	}
}
