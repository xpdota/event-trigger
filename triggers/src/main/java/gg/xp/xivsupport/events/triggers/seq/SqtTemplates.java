package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class SqtTemplates {
	private static final Logger log = LoggerFactory.getLogger(SqtTemplates.class);

	private SqtTemplates() {
	}

	public static <X extends HasDuration> SequentialTrigger<BaseEvent> callWhenDurationIs(
			Class<X> eventType,
			Predicate<X> eventFilter,
			ModifiableCallout<? super X> callout,
			Duration targetDuration
	) {
		return sq(
				120_000,
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

	public static <X> SequentialTrigger<BaseEvent> sq(
			int timeoutMs,
			Class<X> startType,
			Predicate<X> startCondition,
			BiConsumer<X, SequentialTriggerController<BaseEvent>> trigger
	) {
		return new AutoWipeSequentialTrigger<>(timeoutMs, startType, startCondition, trigger);
	}



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
				log.warn("Too many invocations of this trigger!");
				return;
			}
			BiConsumer<X, SequentialTriggerController<BaseEvent>> current = triggers[index];
			current.accept(e1, s);
		};
		return new AutoWipeSequentialTrigger<>(timeoutMs, startType, startCondition, combined) {
			@Override
			void onWipe() {
				super.onWipe();
				mint.setValue(0);
			}
		};
	};

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

	private static class AutoWipeSequentialTrigger<X> extends SequentialTrigger<BaseEvent> {
		public AutoWipeSequentialTrigger(int timeoutMs, Class<X> startType, Predicate<X> startCondition, BiConsumer<X, SequentialTriggerController<BaseEvent>> trigger) {
			super(timeoutMs, BaseEvent.class, e1 -> startType.isInstance(e1) && startCondition.test((X) e1), (e1, s) -> {
				trigger.accept((X) e1, s);
			});
		}

		@Override
		public void feed(EventContext ctx, BaseEvent event) {
			if (event instanceof WipeEvent) {
				onWipe();
			}
			super.feed(ctx, event);
		}

		void onWipe() {
			forceExpire();
		};

	}
}
