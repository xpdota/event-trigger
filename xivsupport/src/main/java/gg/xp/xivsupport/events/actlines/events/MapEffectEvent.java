package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class MapEffectEvent extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -7715636755055037213L;
	private final long instanceContentId;
	private final long flags;
	private final long index;
	private final long unknown1;
	private final long unknown2;

	public MapEffectEvent(long instanceContentId, long flags, long index, long unknown1, long unknown2) {
		this.instanceContentId = instanceContentId;
		this.flags = flags;
		this.index = index;
		this.unknown1 = unknown1;
		this.unknown2 = unknown2;
	}


	public long getInstanceContentId() {
		return instanceContentId;
	}

	public long getFlags() {
		return flags;
	}

	public long getIndex() {
		return index;
	}

	public long getUnknown1() {
		return unknown1;
	}

	public long getUnknown2() {
		return unknown2;
	}

	@Override
	public String toString() {
		return "MapEffect{" +
				"instanceContentId=" + instanceContentId +
				", flags=" + flags +
				", index=" + index +
				", unknown1=" + unknown1 +
				", unknown2=" + unknown2 +
				'}';
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%X:%X:%X:%X:%X", index, flags, index, unknown1, unknown2);
	}
}
