package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public record ConditionDescription<X extends Condition<Y>, Y>(
		Class<X> clazz,
		Class<Y> appliesTo,
		String description,
		Supplier<X> instanceCreator,
		BiFunction<X, EasyTrigger<? super Y>, Component> guiprovider
) {
	public boolean appliesTo(Class<?> eventType) {
		return appliesTo.isAssignableFrom(eventType);
	}

	public X newInst() {
		return instanceCreator.get();
	}
}
