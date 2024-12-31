package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * "Sequential Triggers" allow for a sequence of events to be collected interactively within a block of code.
 * This massively simplifies the code for complex mechanic triggers, as the code for the entire sequence of events
 * can live in one sequential trigger, rather than needing multiple "collector" triggers and having the logic spread
 * out.
 * <p>
 * It also simplifies cleanup, since all of your state can be kept as local variables rather than class fields.
 *
 * @param <X> The event type. Should usually just be 'BaseEvent'.
 */
public class SequentialTrigger<X extends BaseEvent> implements TypedEventHandler<X> {

	private @Nullable SequentialTriggerController<X> instance;
	private final List<SequentialTriggerController<X>> instances = new ArrayList<>();
	private final int timeoutMs;
	private Class<X> type;
	private final Predicate<X> startOn;
	private final BiConsumer<X, SequentialTriggerController<X>> trigger;
	private SequentialTriggerConcurrencyMode concurrency = SequentialTriggerConcurrencyMode.BLOCK_NEW;

	public SequentialTrigger(int timeoutMs, Class<X> type, Predicate<X> startOn, BiConsumer<X, SequentialTriggerController<X>> trigger) {
		this.timeoutMs = timeoutMs;
		this.type = type;
		this.startOn = startOn;
		this.trigger = trigger;
	}

	/**
	 * Feed an event into the sequential trigger
	 *
	 * @param ctx   The usual event context
	 * @param event The usual event
	 */
	public void feed(EventContext ctx, X event) {
		if (!type.isInstance(event)) {
			return;
		}
		switch (concurrency) {
			case BLOCK_NEW -> {
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
			case REPLACE_OLD -> {
				if (startOn.test(event)) {
					if (instance != null) {
						instance.stopSilently();
					}
					instance = new SequentialTriggerController<>(ctx, event, trigger, timeoutMs);
				}
				if (instance != null) {
					instance.provideEvent(ctx, event);
					if (instance.isDone()) {
						instance = null;
					}
				}
			}
			case CONCURRENT -> {
				var iter = instances.iterator();
				while (iter.hasNext()) {
					SequentialTriggerController<X> next = iter.next();
					next.provideEvent(ctx, event);
					if (next.isDone()) {
						iter.remove();
					}
				}
				if (startOn.test(event)) {
					instances.add(new SequentialTriggerController<>(ctx, event, trigger, timeoutMs));
				}
			}
		}
	}

	public void forceExpire() {
		SequentialTriggerController<X> inst = instance;
		if (inst != null) {
			inst.forceExpire();
			instance = null;
		}
		instances.forEach(SequentialTriggerController::stopSilently);
		instances.clear();
	}

	public void stopSilently() {
		SequentialTriggerController<X> inst = instance;
		if (inst != null) {
			inst.stopSilently();
			instance = null;
		}
		instances.forEach(SequentialTriggerController::stopSilently);
		instances.clear();
	}


	@Override
	public void handle(EventContext context, X event) {
		feed(context, event);
	}

	@Override
	public Class<X> getType() {
		return type;
	}

	public boolean isActive() {
		return instance != null || !instances.isEmpty();
	}

	/**
	 * Sets the concurrency policy. See {@link SequentialTriggerConcurrencyMode}. Should be set prior to any actual
	 * use of the trigger.
	 *
	 * @see SequentialTriggerConcurrencyMode
	 * @param concurrency The new concurrency policy.
	 * @return This (builder pattern)
	 */
	public SequentialTrigger<X> setConcurrency(SequentialTriggerConcurrencyMode concurrency) {
		this.concurrency = concurrency;
		return this;
	}
}
