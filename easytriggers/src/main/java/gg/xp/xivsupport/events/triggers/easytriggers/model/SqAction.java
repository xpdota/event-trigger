package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface SqAction<X extends BaseEvent> extends Action<X> {
	void accept(SequentialTriggerController<X> stc, EasyTriggerContext context, X event);
}
