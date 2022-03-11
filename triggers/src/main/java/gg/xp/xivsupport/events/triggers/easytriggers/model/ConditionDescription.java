package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.awt.*;
import java.util.function.Function;

public record ConditionDescription<X extends Condition<?>>(
		Class<X> clazz,
		Class<?> appliesTo,
		String description,
		Function<X, Component> guiprovider
) {
}
