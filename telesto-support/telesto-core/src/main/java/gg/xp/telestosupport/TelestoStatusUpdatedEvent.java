package gg.xp.telestosupport;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class TelestoStatusUpdatedEvent extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = 673924008017979056L;
	private final TelestoStatus oldStatus;
	private final TelestoStatus newStatus;

	public TelestoStatusUpdatedEvent(TelestoStatus oldStatus, TelestoStatus newStatus) {
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public TelestoStatus getOldStatus() {
		return oldStatus;
	}

	public TelestoStatus getNewStatus() {
		return newStatus;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s -> %s", oldStatus, newStatus);
	}
}
