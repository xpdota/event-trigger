package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SqtTemplates {

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
		return new SequentialTrigger<>(
				timeoutMs,
				BaseEvent.class,
				e1 -> startType.isInstance(e1) && startCondition.test((X) e1),
				(e1, s) -> {
					trigger.accept((X) e1, s);
				}) {
			@Override
			public void feed(EventContext ctx, BaseEvent event) {
				if (event instanceof WipeEvent wipe) {
					forceExpire();
				}
				super.feed(ctx, event);
			}
		};
	}

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

}
