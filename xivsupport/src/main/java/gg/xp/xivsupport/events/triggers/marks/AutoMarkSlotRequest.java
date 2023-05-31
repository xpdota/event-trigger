package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.services.Handleable;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class AutoMarkSlotRequest extends BaseEvent implements HasPrimaryValue, Handleable {

	@Serial
	private static final long serialVersionUID = 792969414398476188L;
	private final int slotToMark;
	private transient boolean handled;

	public AutoMarkSlotRequest(int slotToMark) {
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

	@Override
	public String getPrimaryValue() {
		return String.format("<%s>", slotToMark);
	}

	@Override
	public String toString() {
		return "AutoMarkSlotRequest{" +
		       "slotToMark=" + slotToMark +
		       '}';
	}

	@Override
	public boolean isHandled() {
		return handled;
	}

	@Override
	public void setHandled() {
		handled = true;
	}
}
