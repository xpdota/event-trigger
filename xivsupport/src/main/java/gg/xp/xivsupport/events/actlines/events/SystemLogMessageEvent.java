package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.io.Serial;

@SystemEvent
public class SystemLogMessageEvent extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -8601881229999998758L;
	private final long unknown;
	private final long id;
	private final long param0;
	private final long param1;
	private final long param2;

	public SystemLogMessageEvent(long unknown, long id, long param0, long param1, long param2) {
		this.unknown = unknown;
		this.id = id;
		this.param0 = param0;
		this.param1 = param1;
		this.param2 = param2;
	}

	public long getUnknown() {
		return unknown;
	}

	public long getId() {
		return id;
	}

	public long getParam0() {
		return param0;
	}

	public long getParam1() {
		return param1;
	}

	public long getParam2() {
		return param2;
	}

	@Override
	public String toString() {
		return "SystemLogMessage{" +
				"unknown=" + unknown +
				", id=" + id +
				", param0=" + param0 +
				", param1=" + param1 +
				", param2=" + param2 +
				'}';
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%X:%X:%X:%X:%X", unknown, id, param0, param1, param2);
	}
}
