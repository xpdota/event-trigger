package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.Position;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class SnapshotLocationDataEvent extends BaseEvent implements DescribesCastLocation<AbilityUsedEvent> {

	@Serial
	private static final long serialVersionUID = -2892534662649165007L;
	private final AbilityUsedEvent event;
	private final Position pos;
	private final Double heading;

	public SnapshotLocationDataEvent(AbilityUsedEvent event, Position pos) {
		this.event = event;
		this.pos = pos;
		this.heading = null;
	}

	public SnapshotLocationDataEvent(AbilityUsedEvent event, double heading) {
		this.event = event;
		this.pos = null;
		this.heading = heading;
	}
	
	@Override
	public AbilityUsedEvent originalEvent() {
		return event;
	}

	@Nullable
	@Override
	public Position getPos() {
		return pos;
	}

	@Nullable
	@Override
	public Double getHeadingOnly() {
		return heading;
	}
}
