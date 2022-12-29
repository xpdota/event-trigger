package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public record ActionDescription<X extends Action<Y>, Y>(
		Class<X> clazz,
		Class<Y> appliesTo,
		String description,
		Supplier<X> instanceCreator,
		BiFunction<X, HasMutableActions<?>, Component> guiprovider
) {
	public boolean appliesTo(Class<?> eventType) {
		return appliesTo.isAssignableFrom(eventType);
	}

	public X newInst() {
		return instanceCreator.get();
	}
}
