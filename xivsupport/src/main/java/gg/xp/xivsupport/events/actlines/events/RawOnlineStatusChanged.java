package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.io.Serial;

@SystemEvent
public class RawOnlineStatusChanged extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -5620693730040682765L;
	private final long targetId;
	private final int rawStatusId;
	private final String statusName;

	public RawOnlineStatusChanged(long targetId, int rawStatusId, String statusName) {
		this.targetId = targetId;
		this.rawStatusId = rawStatusId;
		this.statusName = statusName;
	}

	public long getTargetId() {
		return targetId;
	}

	public int getRawStatusId() {
		return rawStatusId;
	}

	public String getStatusName() {
		return statusName;
	}
}
