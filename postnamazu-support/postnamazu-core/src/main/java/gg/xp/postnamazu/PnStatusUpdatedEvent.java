package gg.xp.postnamazu;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class PnStatusUpdatedEvent extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = 673924008017979056L;
	private final PnStatus oldStatus;
	private final PnStatus newStatus;

	public PnStatusUpdatedEvent(PnStatus oldStatus, PnStatus newStatus) {
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public PnStatus getOldStatus() {
		return oldStatus;
	}

	public PnStatus getNewStatus() {
		return newStatus;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s -> %s", oldStatus, newStatus);
	}

	@Override
	public boolean shouldSave() {
		return true;
	}
}
