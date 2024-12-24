package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@SystemEvent
public class SnapshotLocationDataEvent extends BaseEvent implements DescribesCastLocation<AbilityUsedEvent>, HasSourceEntity, HasAbility {

	@Serial
	private static final long serialVersionUID = -2892534662649165007L;
	private final AbilityUsedEvent event;
	private final Position pos;
	private final Double heading;
	private final @Nullable XivCombatant animationTarget;

	public SnapshotLocationDataEvent(AbilityUsedEvent event, @Nullable XivCombatant animationTarget, DescribesCastLocation<?> other) {
		this.event = event;
		this.pos = other.getPos();
		this.heading = other.getHeadingOnly();
		this.animationTarget = animationTarget;
	}

	public SnapshotLocationDataEvent(AbilityUsedEvent event, @Nullable XivCombatant animationTarget, Position pos) {
		this.event = event;
		this.pos = pos;
		this.heading = null;
		this.animationTarget = animationTarget;
	}

	public SnapshotLocationDataEvent(AbilityUsedEvent event, @Nullable XivCombatant animationTarget, double heading) {
		this.event = event;
		this.pos = null;
		this.heading = heading;
		this.animationTarget = animationTarget;
	}

	@Deprecated // Use constructors with animationTarget
	public SnapshotLocationDataEvent(AbilityUsedEvent event, DescribesCastLocation<?> other) {
		this.event = event;
		this.pos = other.getPos();
		this.heading = other.getHeadingOnly();
		this.animationTarget = null;
	}

	@Deprecated // Use constructors with animationTarget
	public SnapshotLocationDataEvent(AbilityUsedEvent event, Position pos) {
		this.event = event;
		this.pos = pos;
		this.heading = null;
		this.animationTarget = null;
	}

	@Deprecated // Use constructors with animationTarget
	public SnapshotLocationDataEvent(AbilityUsedEvent event, double heading) {
		this.event = event;
		this.pos = null;
		this.heading = heading;
		this.animationTarget = null;
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

	@Override
	public XivAbility getAbility() {
		return event.getAbility();
	}

	@Override
	public XivCombatant getSource() {
		return event.getSource();
	}

	@Override
	public @Nullable XivCombatant getAnimationTarget() {
		return animationTarget;
	}

	@Override
	public String toString() {
		return "SnapshotLocationDataEvent{" +
		       "id=" + event.getAbility().getId() +
		       ", pos=" + pos +
		       ", heading=" + heading +
		       ", animTgt=" + animationTarget +
		       '}';
	}
}
