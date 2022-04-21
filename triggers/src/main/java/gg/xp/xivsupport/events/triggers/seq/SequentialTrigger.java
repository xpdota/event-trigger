package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SequentialTrigger<X extends BaseEvent> {

	private @Nullable SequentialTriggerController<X> instance;
	private final int timeoutMs;
	private Class<X> type;
	private final Predicate<X> startOn;
	private final BiConsumer<X, SequentialTriggerController<X>> trigger;

	public SequentialTrigger(int timeoutMs, Class<X> type, Predicate<X> startOn, BiConsumer<X, SequentialTriggerController<X>> trigger) {
		this.timeoutMs = timeoutMs;
		this.type = type;
		this.startOn = startOn;
		this.trigger = trigger;
	}

	public void feed(EventContext ctx, X event) {
		if (!type.isInstance(event)) {
			return;
		}
		if (instance == null) {
			if (startOn.test(event)) {
				instance = new SequentialTriggerController<>(ctx, event, trigger, timeoutMs);
			}
		}
		else {
			instance.provideEvent(ctx, event);
			if (instance.isDone()) {
				instance = null;
			}
		}
	}


}
