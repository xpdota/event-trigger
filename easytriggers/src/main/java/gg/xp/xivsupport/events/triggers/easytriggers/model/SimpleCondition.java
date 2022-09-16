package gg.xp.xivsupport.events.triggers.easytriggers.model;

public interface SimpleCondition<X> extends Condition<X> {
	@Override
	default boolean test(EasyTriggerContext context, X event) {
		return test(event);
	}

	boolean test(X event);
}
