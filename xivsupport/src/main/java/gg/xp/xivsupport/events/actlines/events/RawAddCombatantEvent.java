package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Equivalent to an ACT 03 line. This event is intentionally left pretty bare, because
 * you should get all of the info from XivState.
 */
@SystemEvent
public class RawAddCombatantEvent extends BaseEvent {
	@Serial
	private static final long serialVersionUID = 3615674340829313314L;
	private final XivCombatant entity;
	private final @Nullable RawXivCombatantInfo fullInfo;

	public RawAddCombatantEvent(XivCombatant entity) {
		this(entity, null);
	}
	public RawAddCombatantEvent(XivCombatant entity, @Nullable RawXivCombatantInfo raw) {
		this.entity = entity;
		this.fullInfo = raw;
	}

	public XivCombatant getEntity() {
		return entity;
	}

	public @Nullable RawXivCombatantInfo getFullInfo() {
		return fullInfo;
	}
}
