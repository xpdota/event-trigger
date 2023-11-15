package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@SystemEvent
public class CastLocationDataEvent extends BaseEvent implements DescribesCastLocation<AbilityCastStart>, HasSourceEntity, HasAbility {

	@Serial
	private static final long serialVersionUID = -1353534122836715832L;
	private final AbilityCastStart event;
	private final Position pos;
	private final Double heading;

	public CastLocationDataEvent(AbilityCastStart event, Position pos) {
		this.event = event;
		this.pos = pos;
		this.heading = null;
	}

	public CastLocationDataEvent(AbilityCastStart event, double heading) {
		this.event = event;
		this.pos = null;
		this.heading = heading;
	}

	@Override
	public AbilityCastStart originalEvent() {
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
}
