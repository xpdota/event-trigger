package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class SubMapChangeEvent extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -3754616328174384853L;
	private final @Nullable XivMap map;

	public SubMapChangeEvent(@Nullable XivMap map) {
		this.map = map;
	}

	public @Nullable XivMap getMap() {
		return map;
	}

	@Override
	public String getPrimaryValue() {
		return map == null ? "(reset)" : map.toString();
	}
}
