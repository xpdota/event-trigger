package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class SpecificAutoMarkSlotRequest extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 792969414398476188L;
	private final int slotToMark;
	private final MarkerSign marker;

	public SpecificAutoMarkSlotRequest(int slotToMark, MarkerSign marker) {
		this.marker = marker;
		if (slotToMark >= 1 && slotToMark <= 8) {
			this.slotToMark = slotToMark;
		}
		else {
			throw new IllegalArgumentException("Party slot must be between 1 and 8, but got " + slotToMark);
		}
	}

	public int getSlotToMark() {
		return slotToMark;
	}

	public MarkerSign getMarker() {
		return marker;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("'%s' on <%s>", marker.getCommand(), slotToMark);
	}
}
