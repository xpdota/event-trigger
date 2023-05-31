package gg.xp.xivsupport.events.triggers.easytriggers.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@SystemEvent
public class EasyTriggersInitEvent extends BaseEvent {
	@Serial
	private static final long serialVersionUID = 585977695445950515L;

	private final @Nullable EasyTrigger<EasyTriggersInitEvent> triggerToInit;

	/**
	 * Constructor for initializing a specific trigger (i.e. importing a specific trigger)
	 *
	 * @param triggerToInit The trigger to initialize. Easy triggers will ignore this event if it is not "their" event.
	 */
	public EasyTriggersInitEvent(@Nullable EasyTrigger<EasyTriggersInitEvent> triggerToInit) {
		this.triggerToInit = triggerToInit;
	}

	/**
	 * Constructor for initializing all triggers (i.e. startup)
	 */
	public EasyTriggersInitEvent() {
		this.triggerToInit = null;
	}

	public @Nullable EasyTrigger<EasyTriggersInitEvent> getTriggerToInit() {
		return triggerToInit;
	}
}
