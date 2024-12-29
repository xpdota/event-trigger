package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class SqtTemplates {
	private static final Logger log = LoggerFactory.getLogger(SqtTemplates.class);

	private SqtTemplates() {
	}

	/**
	 * Given an event with a duration, trigger the given callout when the duration dips
	 * below the given duration.
	 *
	 * @param eventType      Event type
	 * @param eventFilter    Event filter
	 * @param callout        The callout to trigger
	 * @param targetDuration The duration at which to call out
	 * @param <X>            Event type
	 * @return The constructed trigger
	 */
	public static <X extends HasDuration> SequentialTrigger<BaseEvent> callWhenDurationIs(
			Class<X> eventType,
			Predicate<X> eventFilter,
			ModifiableCallout<? super X> callout,
			Duration targetDuration
	) {
		return sq(
				180_000,
				eventType,
				eventFilter,
				(e1, s) -> {
					long msToWait = e1.getEstimatedRemainingDuration().minus(targetDuration).toMillis();
					if (msToWait > 0) {
						s.waitMs(msToWait);
					}
					s.updateCall(callout.getModified(e1));
				});
	}

	/**
	 * Convenience function for making a typical sequential controller.
	 * <p>
	 * Unlike using a raw {@link SequentialTrigger}, this provides an auto-reset-on-wipe, as well
	 * as handling type safety of the initial event since it is typically recommended to use a more
	 * broad type (e.g. BaseEvent) as the generic type, while the start event is more specific.
	 *
	 * @param timeoutMs      Timeout
	 * @param startType      Starting event type
	 * @param startCondition Starting event condition
	 * @param trigger        The trigger code
	 * @param <X>            The start event type
	 * @return The constructed Sequential Trigger
	 */
	public static <X> SequentialTrigger<BaseEvent> sq(
			int timeoutMs,
			Class<X> startType,
			Predicate<X> startCondition,
			BiConsumer<X, SequentialTriggerController<BaseEvent>> trigger
	) {
		return new AutoWipeSequentialTrigger<>(timeoutMs, startType, startCondition, trigger);
	}


	/**
	 * Trigger template for when the same event might indicate different things in a fight.
	 * <p>
	 * The first time this is called, it will execute the first instance in the 'triggers' array.
	 * The second time, it will execute the second, and so on. It will reset back to the first
	 * on a wipe/reset.
	 *
	 * @param timeoutMs      Timeout. This is the same for each individual trigger, so you should
	 *                       use the highest value that any individual invocation needs.
	 * @param startType      Start event type.
	 * @param startCondition Start event condition.
	 * @param triggers       Array of triggers to be fired in order when we see the start event.
	 *                       These will work exactly like normal sequential triggers, but will be
	 *                       used sequentially.
	 * @param <X>            Start event type.
	 * @return The constructed trigger.
	 */
	public static <X> SequentialTrigger<BaseEvent> multiInvocation(
			int timeoutMs,
			Class<X> startType,
			Predicate<X> startCondition,
			BiConsumer<X, SequentialTriggerController<BaseEvent>>... triggers
	) {
		MutableInt mint = new MutableInt();
		BiConsumer<X, SequentialTriggerController<BaseEvent>> combined = (e1, s) -> {
			int index = mint.getAndIncrement();
			if (index >= triggers.length) {
				log.info("Too many invocations of this trigger, ignoring.");
				return;
			}
			BiConsumer<X, SequentialTriggerController<BaseEvent>> current = triggers[index];
			current.accept(e1, s);
		};
		return new AutoWipeSequentialTrigger<>(timeoutMs, startType, startCondition, combined) {
			@Override
			void reset() {
				super.reset();
				mint.setValue(0);
			}
		};
	}

	public static <X> SequentialTrigger<BaseEvent> selfManagedMultiInvocation(
			int timeoutMs,
			Class<X> startType,
			Predicate<X> startCondition,
			TriConsumer<X, SequentialTriggerController<BaseEvent>, Integer> trigger) {
		MutableInt mint = new MutableInt();
		BiConsumer<X, SequentialTriggerController<BaseEvent>> combined = (e1, s) -> {
			int index = mint.getAndIncrement();
			trigger.accept(e1, s, index);
		};
		return new AutoWipeSequentialTrigger<>(timeoutMs, startType, startCondition, combined) {
			@Override
			void reset() {
				super.reset();
				mint.setValue(0);
			}
		};

	}

	/**
	 * Trigger template for when you want one call at the start of a cast bar, then
	 * another call at the end of the cast bar.
	 * <p>
	 * Note that this uses the cast duration ONLY - it does not check to see if the
	 * ability has actually gone off or not.
	 *
	 * @param castFilter  What cast to look for
	 * @param initialCall Initial call
	 * @param followup    Followup call
	 * @return The constructed trigger
	 */
	public static SequentialTrigger<BaseEvent> beginningAndEndingOfCast(
			Predicate<AbilityCastStart> castFilter,
			ModifiableCallout<? super AbilityCastStart> initialCall,
			ModifiableCallout<?> followup) {
		return sq(60_000, AbilityCastStart.class, castFilter,
				(e1, s) -> {
					s.updateCall(initialCall.getModified(e1));
					s.waitMs(e1.getEstimatedRemainingDuration().toMillis());
					s.updateCall(followup.getModified());
				});
	}

	/**
	 * @return A no-op trigger.
	 */
	public static SequentialTrigger<BaseEvent> nothing() {
		return sq(10_000, BaseEvent.class, be -> false, (e1, s) -> {
		});
	}

	private static class AutoWipeSequentialTrigger<X> extends SequentialTrigger<BaseEvent> {
		public AutoWipeSequentialTrigger(int timeoutMs, Class<X> startType, Predicate<X> startCondition, BiConsumer<X, SequentialTriggerController<BaseEvent>> trigger) {
			super(timeoutMs, BaseEvent.class, e1 -> startType.isInstance(e1) && startCondition.test((X) e1), (e1, s) -> {
				trigger.accept((X) e1, s);
			});
		}

		@Override
		public void feed(EventContext ctx, BaseEvent event) {
			if (event instanceof WipeEvent || event instanceof PullStartedEvent) {
				reset();
			}
			super.feed(ctx, event);
		}

		void reset() {
			forceExpire();
		}

	}
}
