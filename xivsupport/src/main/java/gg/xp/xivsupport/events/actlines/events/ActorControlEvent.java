package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

/**
 * Represents various types of actor control events, e.g. wipes, barrier up/down, etc
 */
public class ActorControlEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 4887874439217409590L;
	private final long instance;
	private final long command;
	private final long data0;
	private final long data1;
	private final long data2;
	private final long data3;
	private final long updateTypeRaw;
	private final long instanceContentTypeRaw;

	public ActorControlEvent(long instance, long command, long data0, long data1, long data2, long data3) {
		this.instance = instance;
		this.command = command;
		this.data0 = data0;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
		this.updateTypeRaw = instance >> 16;
		this.instanceContentTypeRaw = instance % 65536;
	}

	public long getInstance() {
		return instance;
	}

	public long getCommand() {
		return command;
	}

	public long getData0() {
		return data0;
	}

	public long getData1() {
		return data1;
	}

	public long getData2() {
		return data2;
	}

	public long getData3() {
		return data3;
	}

	public long getUpdateTypeRaw() {
		return updateTypeRaw;
	}

	public long getInstanceContentTypeRaw() {
		return instanceContentTypeRaw;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%X:%X %X:%X:%X:%X", instance, command, data0, data1, data2, data3);
	}
}
